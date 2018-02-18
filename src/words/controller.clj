(ns words.controller
  (:require [words.users :as users]
            [clojure.string :as str]
            [words.yandex :as yandex]
            [words.tr :refer :all]
            [words.storage :as storage]
            [words.common :as c]))

(defn- prompt
  ([user] (prompt user nil))
  ([user params]
   (case (:state user)
     "word" "Send a new word to train"
     "desc-words" "🤔"
     "translation" (str "Possible translations: " (str/join ", " params) "\nNow send your selected translation")
     "exercise" (str "Exercise progress: " (str (- users/exercise-length (count (:exercise user))) " / " users/exercise-length))
     "remove-word" (str "Send a word to remove")
     (str "Oops, you're in unknown state: " (:state user)))))

(defn- buttons [user]
  (let [weak-words-count (users/count-weak-words (:id user))
        default-buttons ["Show my words" "Help"]
        buttons (if (< 0 weak-words-count) (conj default-buttons (str "Start exercise")) default-buttons)]
    (case (:state user)
      "word" buttons
      "desc-words" (conj buttons "Remove word")
      "translation" (conj (:translations user) "Cancel")
      "translation-added" (conj buttons "Cancel")
      "exercise" (conj default-buttons "Stop exercise")
      "remove-word" ["Cancel"]
      buttons)))

(defmulti handle-message (fn [state _ _ _] (or state :none)))

(defmethod handle-message :none [_ user-id message reply]
  (reply "Oops, you're in unknown state"))

(defmethod handle-message "word" [_ user-id message reply]
  (let [user (users/start-adding-word user-id message)
        translations (yandex/translation message (:language user))]
    (reply (prompt user translations)
           (buttons {:id (:id user) :state "translation" :translations translations}))))

(defmethod handle-message "translation" [_ user-id message reply]
  (let [user (users/finish-adding-word user-id message)]
    (reply (str "Translation saved " (:word user) " -> " message))
    (reply (prompt user) (buttons {:state "translation-added" :id (:id user)}))))

(defmethod handle-message "exercise" [_ user-id message reply]
  (c/log "EX" user-id message)
  (let [[ok expected next-word points] (users/exercise-answer user-id message)]
    (let [user (storage/get-user user-id)
          next-prompt (if next-word
                        (str "Next word: " next-word)
                        (str "Excercise finished. Points earned: " points))]
      (if ok
        (reply (str "Correct! " next-prompt) (buttons user))
        (reply (str "Incorrect! Correct answer was: " expected ". " next-prompt) (buttons user))))))

(defmethod handle-message "remove-word" [_ user-id message reply]
  (let [user (users/finish-removing-word user-id message)]
    (if user (reply (prompt user) (buttons user))
           (reply "Word not found, try again"))))

(defn handle [user-id message reply]
  (let [user (users/get-user user-id)
        reply-prompt (fn [u] (reply (prompt u) (buttons u)))]
    (case message
      "/start" (let [user (users/add-user user-id)]
                 (reply (:welcome tr) (buttons user))
                 (reply-prompt user))
      "Help" (do (reply (users/desc-state user-id))
                 (reply-prompt user))
      "Show my words" (do (reply (users/desc-words user-id))
                          (reply-prompt (assoc user :state "desc-words")))
      "Cancel" (case (:state user)
                 "translation" (let [user (users/reset user-id)]
                                 (reply-prompt user))
                 "word" (let [user (users/start-adding-word user-id (:word user))]
                          (reply-prompt user))
                 "remove-word" (reply-prompt (users/reset user-id))
                 (reply "Nothing to cancel"))
      "Remove word" (let [user (users/start-removing-word user-id)]
                      (reply (prompt user) (buttons user)))
      "Stop exercise" (let [user (users/stop-exercise user-id)]
                        (reply "Exercise cancelled")
                        (reply (prompt user) (buttons user)))
      "Start exercise" (let [exercise (users/start-exercise user-id)
                        first-word (first exercise)]
                    (if (< 0 (count exercise))
                      (reply
                        (str "Exercise started, words to go: " (count exercise) ". Your first word: " first-word)
                        (buttons user))
                      (reply "Too few words to start exercise, add more")))
      (handle-message (:state user) user-id message reply))))

