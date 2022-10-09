(defproject depict-code "0.1.0-SNAPSHOT"
  :description "A syntax highlighter with support for more complex visualizations of code."
  :url "https://github.com/Ross-Esmond/depict-code"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse "1.4.12"]
                 [org.clojure/core.match "1.0.0"]]
  :main ^:skip-aot lang.core
  :target-path "target/%s"
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.25.0"]
                             [venantius/ultra "0.6.0"]]}
             :uberjar {:aot :all}})
