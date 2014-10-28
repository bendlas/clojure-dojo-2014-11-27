(ns dojo.dev
  (:require
   [weasel.repl :as ws-repl]
   dojo.app))

(ws-repl/connect "ws://localhost:9001" :verbose true)

(dojo.app/main)
