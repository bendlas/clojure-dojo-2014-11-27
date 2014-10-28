(ns dojo.dev
  (:require
   [dojo.app :as app]
   [dojo.dev.handler :refer [echo-handler throw-handler debug-handler]]
   [dojo.middleware :refer [wrap-resource]]

   [clojure.tools.logging :as log]
   [net.cgrand.moustache :refer [app not-found]]
   [ring.util.response :refer [response content-type resource-response]]
   [ring.middleware.file :refer [wrap-file]]
   [prone.middleware :refer [wrap-exceptions]]

   [clojure.tools.nrepl.server :as nrepl]
   [cider.nrepl :refer [cider-middleware]]
   [cemerick.piggieback :refer [cljs-repl wrap-cljs-repl]]
   [weasel.repl.websocket :refer [repl-env]]
   [lighttable.nrepl.handler :refer [lighttable-ops]]

   [ring.adapter.jetty :refer [run-jetty]]
   (ring.middleware [params :refer [wrap-params]]
                    [file :refer [wrap-file]])))

;; # Development environment for Clojurescript

;; ## Dev infrastructure

;; The webapp in development uses :optimizations :none and uses
;; closure's provide/require mechanism to load closure compiled files
;;
;; Additional libraries are loaded from webjars included with leiningen

;; ### Dev Routes

;; The dev handler adds/overlays a couple of routes to the application
;; - `/`            .. Java resource file dev.html
;; - `/<path>`      .. Java resource file target/dev/<path> (cljs output)
;;
;; - `/test/error   .. Test page for server errors
;; - `/test/break   .. Test page for prone debug points
;; - `/test/echo    .. Test page pretty printing the request

(def handler
  (app
   wrap-params
   wrap-exceptions
                                        ; `/<path>`
   (wrap-file "target/dev")
   (wrap-file "target/prod-debug")
                                        ; `/`
   [] (constantly (-> (resource-response "html/dev.html")
                      (content-type "text/html;charset=utf-8")))
                                        ; test handlers for error and echo
   ["test" &] [["error"] throw-handler
               ["break"] debug-handler
                                        ; echo handler for debugging
               ["echo"]  echo-handler]
                                        ; regular app routes
   ["production" &] app/handler
   [&] app/handler))

;; ### Server runner
;; -main runs jetty and nrepl servers and waits for an exit code to orderly shut down

(declare jetty nrepl)
(defonce exit-code (promise))

(defn -main []
  (def nrepl (nrepl/start-server :port 4040
                                        ; with this custom handler, the nrepl connection can support
                                        ; an emacs cider session, a lighttable session, as well as
                                        ; transition to a weasel browser repl
                                 :handler (apply nrepl/default-handler
                                                 #'lighttable-ops
                                                 #'wrap-cljs-repl
                                                 (map resolve cider-middleware))))
  (def jetty (run-jetty #'handler {:port 8080 :join? false}))
  (log/info "Nrepl server set up at localhost:4040")

                                        ; Wait for exit
  (let [ec @exit-code]
    (log/info "Shutting down with exit code" ec)
    (.stop jetty)
    (nrepl/stop-server nrepl)
    (shutdown-agents)
    (System/exit ec)))

;; ### Cider cljs repl

(defn start-browser-repl
  "Start a weasel server from the current nrepl"
  []
  (cljs-repl :repl-env (repl-env :ip "0.0.0.0" :port 9001)))
