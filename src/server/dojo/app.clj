(ns dojo.app
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [dojo.middleware :refer [wrap-resource]]
            [dojo.string :refer [pprint-str pprint-html html-chrome]]
            (liberator [core :refer [defresource handle-exception-rethrow]]
                       [representation :refer [ring-response as-response]])
            [net.cgrand.moustache :refer [app not-found]]
            (ring.middleware [params :refer [wrap-params]]
                             [file :refer [wrap-file]])
            [ring.util.response :refer [resource-response content-type]])
  (:import java.io.PushbackReader))

;; # Simple in-memory database

;; This atom contains a mapping of strings to edn values
(defonce database (atom {}))

(defn set-val
  "Compare and set database key to a new value, possibly raising a :cas-mismatch error"
  [db key cas-val new-val]
  (let [cur-val (get db key)]
    (when-not (= cur-val cas-val)
      (throw (ex-info "Entry exists, CAS error"
                      {:error :cas-mismatch
                       :current-value cur-val
                       :match-value cas-val})))
    (assoc db key new-val)))

;; # Handler logic

(defmulti handle-exception
  "Exception handler for liberator resources.
   Dispatches on the :error key of the exception data."
  (comp :error ex-data))

(defn wrap-exception [h]
  (fn [req]
    (try (h req)
         (catch Exception e
           (handle-exception e)))))

(defmethod handle-exception :default [ctx]
  (handle-exception-rethrow ctx))

;; Treat the cas-mismatch error from database as a 409 Conlict response

(defmethod handle-exception :cas-mismatch [e]
  (let [{:keys [current-value match-value]} (ex-data e)]
    {:status 409
     :headers {"Content-Type" "text/plain;charset=utf-8"}
     :body (str ";; Conflict! \n(not=       ; Current value\n" (pprint-str current-value)
                "\n            ; does not match submitted cas-value\n" (pprint-str match-value)
                ")")}))

;; ## Base resource for edn endpoints

(def edn-resource
  {:available-media-types ["application/edn" "text/plain" "text/html"]
   :as-response (fn [val {{t :media-type} :representation :as ctx}]
                  (as-response (case t
                                 "application/edn" (pr-str val)
                                 "text/html" (html-chrome (pprint-html val))
                                 "text/plain" (pprint-str val))
                               ctx))
   :handle-ok ::value})

;; ## Handler resources for access to the database

(defresource db edn-resource
  :exists? (fn [_] {::db @database}))

(defresource db-value [key] edn-resource
  :allowed-methods [:get :put]
  :exists? (fn [_]
             (let [db @database]
               [(contains? db key) {::db db ::value (get db key)}]))
  :put! (fn [ctx]
          (let [new-val (edn/read (PushbackReader. (io/reader (get-in ctx [:request :body]))))
                db* (if-let [cas (get-in ctx [:request :query-params "cas"])]
                      (swap! database set-val key (edn/read-string cas) new-val)
                      (swap! database assoc key new-val))]
            {::db db* ::value (get db* key)})))

;; ### App Routes

;; The dev handler adds/overlays a couple of routes to the application
;; - `/`            .. Java resource file app.html
;; - `/<path>`      .. Java resource file www/<path> or target/prod/<path> (cljs output)
;; - `/lib/<path>`  .. Java resource META-INF/resources/webjars/<lib-version>/<path>
;;
;; - `/data`        .. Database listing as EDN
;; - `/data/<name>` .. Database item as EDN


(def handler
  "The main application handler"
  (app
   (wrap-resource "www")
   wrap-params
   wrap-exception
   [] (constantly (-> (resource-response "html/app.html")
                      (content-type "text/html;charset=utf-8")))
                                        ; `/lib/<path>
   ["lib" &] [(wrap-resource "META-INF/resources/webjars/react/0.11.2")
              (wrap-resource "META-INF/resources/webjars/react-bootstrap/0.12.0")
              (wrap-resource "META-INF/resources/webjars/bootstrap/3.2.0")
              [&] not-found]

   ;; Database routes
   ["data"] (db)
   ["data" key] (db-value key)))
