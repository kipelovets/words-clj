(ns words.storage.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as str]))

(def server1-conn {:pool {} :spec {:host "redis" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn- keyword-keys [u]
  (not-empty (zipmap (map keyword (keys u)) (vals u))))

(defn- format-key [& ids] (subs (str/join ":" ids) 1))

(defn clear-all [] (wcar* (car/flushdb)))
(defn select-db [n] (wcar* (car/select n)))

(defn fetch [type id]
  (keyword-keys (wcar* (car/hgetall* (format-key type id)))))

(defn save [type id record]
  (wcar* (car/hmset* (format-key type id) record))
  (fetch type id))

(defn fetch-rel [type type-id rel rel-id]
  (keyword-keys (wcar* (car/hgetall* (format-key type type-id rel rel-id)))))

(defn save-rel [type type-id rel rel-id record]
  (wcar*
    (car/sadd (format-key type type-id :rels rel) rel-id)
    (car/hmset* (format-key type type-id rel rel-id) record))
  (fetch-rel type type-id rel rel-id))

(defn remove-rel [type type-id rel rel-id]
  (wcar*
    (car/srem (format-key type type-id :rels rel) rel-id)
    (car/del (format-key type type-id rel rel-id))))

(defn list-rel [type type-id rel]
  (wcar*
    (car/smembers (format-key type type-id :rels rel))))