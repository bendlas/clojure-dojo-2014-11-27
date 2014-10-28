(ns dojo.middleware
  (:require
   [net.cgrand.moustache :refer [path-info]]
   [ring.util.codec :refer [url-decode]]
   [ring.util.response :refer [resource-response]]))

(defn wrap-resource
  "See ring.middleware.response
  This version resolves the path-info instead of full uri, thus can be used on a sublevel-handler."
  [handler root-path]
  (fn [request]
    (if-not (= :get (:request-method request))
      (handler request)
      (let [path (.substring (url-decode (path-info request)) 1)]
        (or (resource-response path {:root root-path})
            (handler request))))))

