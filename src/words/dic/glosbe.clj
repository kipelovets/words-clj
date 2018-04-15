(ns words.dic.glosbe
  (:require [clojure.data.json :as json]
            [clj-http.client :as client])
  (:use [environ.core]
        [slingshot.slingshot :only [try+ throw+]]))

(def ^:private base-url "https://glosbe.com/gapi/translate")

(defn- parse [response]
  (let [trs (map #(get (get % "phrase") "text") (get response "tuc"))]
    (take 5 (filter identity trs))))

(defn translation [word from to]
  (let [resp (client/request
               {:method       :get
                :url          base-url
                :query-params {"from"   from
                               "dest"   to
                               "phrase" word
                               "format" "json"}})
        body (json/read-str (:body resp))]
    ;(prn "GLOSBE" word from to body)
    (parse body)))


