(ns words.storage
  (:require [taoensso.carmine :as car :refer (wcar)]))

; User {:id int, :state State, :exercise [string], :word string}
; States: :word, :translation, :exercise
; Word {:translation string, :strength int, :updated date}

(def server1-conn {:pool {} :spec {:host "redis" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

; Keys
(defn- user-key [id] (str "user:" id))
(defn- word-key [id] (str "user:" id ":words"))

; Global
(defn clear-all [] (wcar* (car/flushdb)))
(defn select-db [n] (wcar* (car/select n)))

; Internal

(defn- keyword-keys [u]
  (zipmap (map keyword (keys u)) (vals u)))

; User
(defn get-user [id]
  (keyword-keys (wcar* (car/hgetall* (user-key id)))))

(defn set-user [id, user]
  (wcar*
    (car/del (user-key id))
    (car/hmset* (user-key id) user)))

(defn update-user [id, fn]
  (set-user id (fn (get-user id))))

(defn set-user-param [id, param, value]
  (wcar* (car/hset (user-key id) param value)))

; Word

(defn get-word [id, word]
  (wcar* (car/hget (word-key id) word)))

(defn set-word [id, word, params]
  (wcar* (car/hset (word-key id) word params)))

(defn update-word [id, word, f]
  (set-word id word (f (get-word id word))))

(defn get-words [id]
  (wcar* (car/hgetall* (word-key id))))