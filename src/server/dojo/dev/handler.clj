(ns dojo.dev.handler
  (:require
   [dojo.string :refer [pprint-html html-chrome]]
   [ring.util.response :refer [content-type response]]
   [clojure.pprint :refer [pprint *print-right-margin*]]
   [prone.debug :refer [debug]]))

(defn echo-handler
  "A simple echo handler for ring requests. PPrints the request to HTML."
  [req]
  (pprint-html)
  (content-type
   (response
    (html-chrome "<p>Your request was:</p>" (pprint-html)))
   "text/html"))

(defn throw-handler [_] (/ 1 0))
(defn debug-handler [{{verb :request-method
                       :strs [host referer user-agent]} :headers}]
  (debug))
