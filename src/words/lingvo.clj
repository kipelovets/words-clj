(ns words.lingvo
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-http.client :as client])
  (:use [environ.core]
        [slingshot.slingshot :only [try+ throw+]]))

(def base-url "https://developers.lingvolive.com")
(def api-key (env :lingvo-key))
(def languages {
                "pl" 1045
                "ru" 1049
                })

(def token nil)

(defn- auth []
  (try
    (let [uri (str base-url "/api/v1.1/authenticate")
          response (client/post uri {:headers {:authorization (str "Basic " api-key)} :content-length 0})]
      (:body response))
    (catch clojure.lang.ExceptionInfo e
      (prn "AUTH ERR" e)
      (throw e))))

(defn- request [method uri params]
  (defn req []
    (client/request (assoc params :method method :url (str base-url uri) :headers {:authorization (str "Bearer " token)})))
  (def token (if token token (auth)))
  (try+ (req)
        (catch [:status 401] {}
          (prn "RETRY")
          (def token (auth))
          (req))
        (catch Object e
          (prn "ERR" e)
          (throw+))))

(defn translation [word from to]
  (let [resp (request :get "/api/v1/Translation"
                      {:query-params {"text" word "srcLang" (get languages from) "dstLang" (get languages to)}})
        body (json/read-str (:body resp))
        trs (get (first (get (first body) "Body")) "Items")]
    (str/join "; " (map #(get (first (get (first (get % "Markup")) "Markup")) "Text") trs))))
