(ns dojo.app
  (:require
   [goog.dom :as gdom]
   [goog.string :as gstr]
   [om.core :as om :include-macros true]
   [om-tools.dom :as dom :include-macros true]
   [bootstrap-cljs :as bs :include-macros true]
   [webnf.util :refer [xhr log]]
   [cljs.core.async :as async :refer [<! >! chan close! map> map< pipe put! mult tap filter<]]
   [cljs.reader :refer [read-string]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]))


(defn success? [s]
  (< 199 s 300))

(def app-state (atom {:editing false
                      :responses []}))

(defn add-response!
  "Register an xhr response channel for addition to the app state, showing up as an alert at the top.
   This allocates a blob url for the response, which should be discarded (see render-response)."
  ([xhr-request] (add-response! xhr-request nil))
  ([xhr-request message]
     (go
       (swap! app-state update-in [:responses] conj
              (let [{:keys [headers body] :as r} (<! xhr-request)]
                (assoc r
                  ::message message
                  ::blob-url (.createObjectURL
                              js/URL (js/Blob. (array body)
                                               (js-obj "type" (get headers "Content-Type" "text/plain"))))))))))

(defn render-response
  "Renders the alert for a received response"
  [i {:keys [status body ::message ::blob-url]}]
  (bs/alert {:bs-style (if (success? status)
                         "success" "danger")
             :on-dismiss (fn []
                           (swap! app-state update-in [:responses]
                                  #(into (subvec % 0 i)
                                         (subvec % (inc i))))
                           (.revokeObjectURL js/URL blob-url)
                           false)
             :on-click #(.open js/window blob-url "_blank")}
            (str status " " (or message (if (success? status)
                                          "XHR Completed"
                                          "XHR Error")))))

;; # XHR methods

(defn put-greeting!
  "Set greeting on server"
  [old-greeting greeting]
  (log "Putting greeting" greeting)
  (let [resp (xhr "/data/greeting"
                  {:headers {"Content-Type" "application/edn"}
                   :params {"cas" (pr-str old-greeting)}
                   :method :put
                   :body (pr-str greeting)})]
    (go (let [{status :status :as r} (<! resp)]
          (add-response! resp (if (= 201 status)
                                (str "Set greeting '" greeting "'")
                                (str "ERROR when setting greeting '" greeting "'"))))
        (swap! app-state assoc
               :editing false
               :sending false
               :new-greeting nil
               :greeting greeting))))


;; The main application
(defn app [{:keys [editing greeting responses sending new-greeting] :as cursor}
           owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
       (map-indexed render-response responses)
       (if editing
         (dom/form {:on-submit (fn [e]
                                 (put-greeting! greeting new-greeting)
                                 (om/update! cursor :sending new-greeting)
                                 false)}
                   (bs/input {:type "text"
                              :default-value greeting
                              :help "Enter new greeting"
                              :bs-style (when sending "warning")
                              :on-change #(om/update! cursor :new-greeting (.. % -target -value))}))
         (dom/h1 {:on-click #(om/update! cursor :editing true)}
                 (or greeting "Hi to OM!") " " (dom/small "You can edit the greeting")))
       (bs/button {:on-click #(add-response! (xhr "/test/error"))}
                  "Click here to get an xhr error")
       (bs/button {:on-click #(add-response! (xhr "/test/break") "XHR Breakpoint")}
                  "Click here to get an xhr breakpoint")))))

(defn ^:export main []
  (go ; get current greeting from server
    (let [{:keys [status body]} (<! (xhr "/data/greeting"))]
        (when (= 200 status)
          (swap! app-state assoc :greeting
                 (read-string body)))))
  (def app-root
    (om/root app app-state {:target (gdom/getElement "app-wrapper")})))

