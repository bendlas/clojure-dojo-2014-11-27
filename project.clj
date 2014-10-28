(defproject clojure-dojo-2014-11-27 "0.1.0-SNAPSHOT"
  :description "Template for planned browser app"
  :url "http://github.com/bendlas/clojure-dojo-2014-11-27"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/server" "src/client" "src/client-dev"]
  :main ^:skip-aot dojo.dev
  :clean-targets ^{:protect false} [:target-path "resources/www/dojo.prod.js" ".repl"]
  
  :dependencies [[org.clojure/clojure "1.7.0-alpha3"]

                 [webnf.deps/dev "0.1.0-alpha2"]
                 [webnf.deps/web "0.1.0-alpha2"]
                 [webnf/base "0.1.0-alpha2"]
                 [ring/ring-jetty-adapter "1.3.1"]

                 ;; # CLJS specific
                 [org.clojure/clojurescript "0.0-2371"]
                 [webnf/cljs "0.1.0-alpha2"]
                 [om "0.8.0-alpha1"]
                 [bootstrap-cljs "0.0.2"]

                 ;; ## Templating
                 ; [kioo "0.4.0"]
                 ; [sablono "0.2.22"]
                 ;; ## webjar libraries
                 [org.webjars/bootstrap "3.2.0"]
                 [org.webjars/react "0.11.2"]
                 [org.webjars/react-bootstrap "0.12.0"]]
  
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild
  {:builds
   {:dev {:source-paths ["src/client" "src/client-dev"]
          :compiler {:output-to "target/dev/dojo.dev.js"
                     :output-dir "target/dev"
                     :optimizations :none
                     :pretty-print true
                     :source-map true}}
    :prod-debug {:source-paths ["src/client"]
                 :compiler {:output-to "target/prod-debug/dojo.prod-debug.js"
                            :output-dir "target/prod-debug"
                            :externs ["react/externs/react.js" "externs/react-bootstrap.js"]
                            :optimizations :advanced
                            :pretty-print true
                            :pseudo-names true
                            :source-map "target/prod-debug/dojo.prod-debug.js.map"}}
    :prod {:source-paths ["src/client"]
           :compiler {:output-to "resources/www/dojo.prod.js"
                      :output-dir "target/prod"
                      :externs ["externs/react.js" "externs/react-bootstrap.js"]
                      :optimizations :advanced
                      :pretty-print false}}}})
