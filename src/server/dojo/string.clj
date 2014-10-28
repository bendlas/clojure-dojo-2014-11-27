(ns dojo.string
  (:require [clojure.pprint :refer [pprint *print-right-margin*]]))

(defn pprint-str
  ([o] (with-out-str (pprint o)))
  ([o right-margin]
     (binding [*print-right-margin* right-margin]
       (pprint-str o))))

(defn escape-html [s]
  (.. s
      (replace "&" "&amp;")
      (replace "<" "&lt;")
      (replace ">" "&gt;")
      (replace "\"" "&quot;")))

(defn pprint-html
  ([o] (pprint-html o 120))
  ([o right-margin]
     (str
      "<pre>"
      (escape-html (pprint-str right-margin))
      "</pre>")))

(defn html-chrome [& body]
  (concat ["<!DOCTYPE html><html><body>"]
          body
          ["</body></html>"]))
