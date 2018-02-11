(defproject words "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [morse "0.3.4"]
                 [com.taoensso/carmine "2.17.0"]
                 [environ "1.1.0"]]
  :main ^:skip-aot words.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
