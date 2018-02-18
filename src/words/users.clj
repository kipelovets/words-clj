(ns words.users
  (:require [clojure.string :as str]
            [words.storage :as storage]
            [words.storage.redis :as redis]))

(def state-word "word")
(def state-translation "translation")
(def state-exercise "exercise")
(def state-remove-word "remove-word")
(def state-lang-from "lang-from")
(def state-lang-to "lang-to")

(def max-strength 5)
(def exercise-length 5)

(defn add-user [id]
  (let [user {:id id :state "lang-from" :language "pl-ru"}]
    (storage/set-user id user)))

(defn get-user [id]
  (storage/get-user id))

(defn start-adding-word [id, word]
  (let [user (assoc (get-user id) :state "translation" :word word)]
    (storage/set-user id user)))

(defn finish-adding-word [id, translation]
  (let [user (assoc (redis/fetch :user id) :state "word")
        word (:word user)]
    (redis/save :user id user)
    (redis/save-rel :user id :word word {:translation translation :strength 0})
    user))

(defn- get-weak-words [id]
  (let [words (storage/get-words id)]
    (filter (fn [key] (< (Integer. (:strength (get words key))) max-strength)) (keys words))))

(defn- generate-exercise [id]
  (let [weak-words (get-weak-words id)]
    (take exercise-length (shuffle weak-words))))

(defn start-exercise [id]
  (let [exercise-words (generate-exercise id)]
    (if (< 0 (count exercise-words))
      (storage/set-user id {:exercise exercise-words :state "exercise" :points 0}))
    exercise-words))

(defn exercise-answer [id, word]
  (let [user (storage/get-user id)
        exercise-words (:exercise user)
        exercise-word (first (:exercise user))
        expected (:translation (storage/get-word id exercise-word))
        exercise-rest (rest exercise-words)
        points (Integer. (:points user))]
    (storage/set-user id {:exercise exercise-rest})
    (if (empty? exercise-rest)
      (storage/set-user id {:state "word"}))
    (if (= expected word)
      (do
        (storage/set-user id {:points points})
        (storage/update-word id exercise-word {:strength (inc (Integer. (:strength (storage/get-word id exercise-word))))})
        [true nil (first exercise-rest) points])
      [false expected (first exercise-rest) nil])))

(defn count-weak-words [id]
  (count (get-weak-words id)))

(defn exercise-next-word [id]
  (let [user (storage/get-user id)]
    (first (:exercise user))))

(defn- desc-word [key, word]
  (str (name key) " (" (:translation word) ") " (:strength word) "/" max-strength))

(defn desc-words [id]
  (let [words (storage/get-words id)]
    (str/join ", " (map (fn [key] (desc-word key (get words key))) (keys words)))))

(defn desc-state [id]
  (let [user (storage/get-user id)
        state (:state user)]
    (condp = state
      state-word "Waiting for a new word"
      state-translation (str "Waiting for a translation for " (:word user))
      state-exercise (str "You are in the middle of an exercise, words left: "
                      (count (:exercise user))
                      ". Waiting for translation for: "
                      (first (:exercise user)))
      state-lang-from "You're selecting your native language"
      state-lang-to "You're selecting the language you're learning"
      state-remove-word "You're removing a word"
      (str "Unknown state: " state))))

(defn reset [id]
  (storage/set-user id {:state "word"}))

(defn stop-exercise [id]
  (reset id))

(defn start-removing-word [id]
  (storage/set-user id {:state "remove-word"}))

(defn finish-removing-word [id word]
  (let [word-params (storage/get-word id word)]
    (if word-params (do (storage/remove-word id word)
                        (storage/set-user id {:state "word"}))
                    nil)))

(defn set-lang-from [id lang]
  (storage/set-user id {:lang-from lang :state state-lang-to}))

(defn set-lang-to [id lang]
  (storage/set-user id {:lang-to lang :state state-word}))

(defn reset-langs [id]
  (storage/set-user id {:state state-lang-from}))
