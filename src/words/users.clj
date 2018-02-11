(ns words.users
  (:require [words.common :as c]
            [clojure.string :as str]
            [words.storage :as storage]))

; User {:id int, :state State, :words [:word Word], :exercise [string], :word string}
; States: :word, :translation, :exercise
; Word {:translation string, :strength int, :updated date}

(def max-strength 5)
(def exercise-length 5)

(defn add-user [id]
  (storage/set-user id {:id id})
  (storage/set-user-param id :state :word))

(defn start-adding-word [id, word]
  (storage/set-user-param id :word word)
  (storage/set-user-param id :state :translation))

(defn finish-adding-word [id, translation]
  (storage/set-word id (:word (storage/get-user id)) {:translation translation :strength 0})
  (storage/set-user-param id :state :word))

(defn- generate-exercise [id]
  (let [words (storage/get-words id)
        weak-words (filter (fn [key] (< (:strength (get words key)) max-strength)) (keys words))]
    (take exercise-length (shuffle weak-words))))

(defn start-exercise [id]
  (let [exercise-words (generate-exercise id)]
    (storage/set-user-param id :exercise exercise-words)
    (storage/set-user-param id :state :exercise)
    exercise-words))

(defn exercise-answer [id, word]
  (let [user (storage/get-user id)
        exercise-words (:exercise user)
        exercise-word (first (:exercise user))
        expected (:translation (storage/get-word id exercise-word))]
    (if (= expected word)
      (do (storage/update-word id exercise-word (fn [w] (assoc w :strength (inc (:strength w)))))
          (let [exercise-rest (rest exercise-words)]
            (storage/set-user-param id :exercise exercise-rest)
            (if (empty? exercise-rest)
              (storage/set-user-param id :state :word))
            true))
      (do (prn "INCORRECT " expected " != " word " for " exercise-word)
          false))))

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
    (case state
      "word"        "Waiting for a new word"
      "translation" (str "Waiting for a translation for " (:word user))
      "exercise"    (str "You are in the middle of an exercise, words left: "
                         (count (:exercise user))
                         ". Waiting for translation for: "
                         (first (:exercise user)))
      (str "Unknown state" state))))


