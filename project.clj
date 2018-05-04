(defproject words "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [morse "0.3.4"]
                 [com.taoensso/carmine "2.17.0"]
                 [environ "1.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.7.0"]
                 [slingshot "0.12.2"]
                 [fb-messenger "0.4.0"]

                 [compojure "1.5.2"]
                 [http-kit "2.2.0"]
                 [ring/ring-json "0.4.0"]

                 [ring/ring-core "1.5.0"]
                 [ring/ring-defaults "0.2.3"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring-cors "0.1.12"]
                 [ring/ring-devel "1.6.3"]
                 ]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.1.0"]]

  :ring {:init words.ring-core/init
         :handler words.ring-core/app}

  :main ^:skip-aot words.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
