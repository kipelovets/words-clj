(ns words.storage
  (:require [words.storage.redis :as redis]))

; User {:id int, :state State, :exercise [string], :word string}
; States: :word, :translation, :exercise
; Word {:translation string, :strength int, :updated date}

; Internal

(defn- keyword-keys [u]
  (zipmap (map keyword (keys u)) (vals u)))

; User
(defn get-user [id]
  (keyword-keys (redis/fetch :user id)))

(defn set-user [id, user]
  (redis/save :user id user))

; Word

(defn get-word [id, word]
  (redis/fetch-rel :user id :word word))

(defn update-word [id, word, params]
  (redis/save-rel :user id :word word params))

(defn get-words [id]
  (let [words (redis/list-rel :user id :word)]
    (zipmap words (map (fn [word] (redis/fetch-rel :user id :word word)) words))))

(defn remove-word [id word]
  (redis/remove-rel :user id :word word))
