(ns words.yandex
  (:require [clojure.data.json :as json]
            [clj-http.client :as client])
  (:use [environ.core]
        [slingshot.slingshot :only [try+ throw+]]))

(def ^:private base-url "https://dictionary.yandex.net/api/v1/dicservice.json/")
(def ^:private api-key (env :yandex-key))

(defn- parse [response]
  (apply concat (map (fn [def] (map #(get % "text") (get def "tr"))) (get response "def"))))

(defn translation [word lang]
  (let [resp (client/request
                      {:method :get
                       :url (str base-url "lookup")
                       :query-params {"key" api-key
                                      "text"    word
                                      "lang" lang}})
        body (json/read-str (:body resp))]
     (parse body)))

