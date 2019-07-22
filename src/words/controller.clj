(ns words.controller
  (:require [words.users :as users]
            [clojure.string :as str]
            [words.dic.glosbe :as dic]
            [words.tr :refer :all]
            [words.storage :as storage]
            [words.common :as c]
            [words.lessons :as lessons]
            [words.state :refer :all]))

(def ^:private btns-langs [btn-en btn-de btn-ru btn-pl])
(def ^:private btn-to-lang {btn-en "en"
                            btn-de "de"
                            btn-ru "ru"
                            btn-pl "pl"})

(defn- prompt
  ([user] (prompt user nil))
  ([user params]
   (case (:state user)
     "word" "Send a new word to train"
     "desc-words" "🤔"
     "translation" (str "Possible translations: " (str/join ", " params) "\nNow send your selected translation")
     "exercise" (str "Exercise progress: " (str (- users/exercise-length (count (:exercise user))) " / " users/exercise-length))
     "remove-word" (str "Send a word to remove")
     "lang-from" "Select your native language"
     "lang-to" "Select the language you're learning"
     (str "Oops, you're in unknown state: " (:state user)))))

(defn- buttons [user]
  (let [weak-words-count (users/count-weak-words (:id user))
        default-buttons [btn-show-words btn-help btn-lessons]
        buttons (if (< 0 weak-words-count) (conj default-buttons btn-exercise) default-buttons)]
    (case (:state user)
      "word" buttons
      "help" (conj buttons btn-lang)
      "desc-words" (conj buttons btn-remove)
      "translation" (conj (:translations user) btn-cancel)
      "translation-added" (conj buttons btn-cancel)
      "exercise" (conj default-buttons btn-stop)
      "remove-word" [btn-cancel]
      "lang-from" btns-langs
      "lang-to" btns-langs
      buttons)))

(def reply-prompt (fn [reply u] (reply (prompt u) (buttons u))))

(defn- handle-create-user [reply user-id]
  (let [user (users/add-user user-id)]
    (reply (:welcome tr) (buttons user))
    (reply-prompt reply user)))

(defmulti handle-message
          (fn [state _ _ _]
            (if (some #{state} '("word" "translation" "exercise" "remove-word" "select-lesson" "lesson"))
              state
              :none)
            ))

(defmethod handle-message :none [_ user-id message reply]
  (handle-create-user reply user-id))

(defmethod handle-message users/state-word [_ user-id message reply]
  (let [word (str/lower-case message)]
    (if (storage/get-word user-id word)
      (reply (str "Word '" word "' already exists, choose another"))
      (let [user (users/start-adding-word user-id word)
            translations (if-let [t (dic/translation word (:lang-to user) (:lang-from user))] t ["Nothing found"])]
        (reply (prompt user translations)
               (buttons {:id (:id user) :state "translation" :translations translations}))))))

(defmethod handle-message users/state-translation [_ user-id message reply]
  (let [word (str/lower-case message)
        user (users/finish-adding-word user-id word)]
    (reply (str "Translation saved: " (:word user) " -> " word))
    (reply (prompt user) (buttons {:state "translation-added" :id (:id user)}))))

(defmethod handle-message users/state-exercise [_ user-id message reply]
  (let [[ok expected next-word points] (users/exercise-answer user-id message)]
    (let [user (storage/get-user user-id)
          next-prompt (if next-word
                        (str "Next word: " next-word)
                        (str "Excercise finished. Points earned: " points))]
      (if ok
        (reply (str "Correct! " next-prompt) (buttons user))
        (reply (str "Incorrect! Correct answer was: " expected ". " next-prompt) (buttons user))))))

(defmethod handle-message users/state-remove-word [_ user-id message reply]
  (let [user (users/finish-removing-word user-id message)]
    (if user (reply (prompt user) (buttons user))
             (reply "Word not found, try again"))))

(defmethod handle-message users/state-select-lesson [_ user-id message reply]
  (lessons/lesson-start reply (users/get-user user-id) message))

(defmethod handle-message users/state-lesson [_ user-id message reply]
  (lessons/lesson-step reply (users/get-user user-id) message))

(defn handle [user-id message reply]
  (let [user (users/get-user user-id)
        state (:state user)]
    (cond
      (= message "/start") (handle-create-user reply user-id)

      (some #(= message %) ["/help" btn-help]) (do (reply (users/desc-state user-id) (buttons {:id (:id user) :state "help"})))

      (= message btn-show-words) (do 
                                   (reply (if-let 
                                            [words-list (not-empty (users/desc-words user-id))]
                                            words-list
                                            "You have no words yet"))
                                   (reply-prompt reply (assoc user :state "desc-words")))

      (= message btn-cancel) (condp = state
                   users/state-translation (let [user (users/reset user-id)]
                                   (reply-prompt reply user))
                   users/state-word (let [user (users/start-adding-word user-id (:word user))]
                            (reply-prompt reply user))
                   users/state-remove-word (reply-prompt reply (users/reset user-id))
                   users/state-select-lesson (reply-prompt reply (users/reset user-id))
                   users/state-lesson (reply-prompt reply (users/reset user-id))
                   (reply (str "Nothing to cancel in state " state)))

      (= message btn-remove) (let [user (users/start-removing-word user-id)]
                   (reply-prompt reply user))

      (= message btn-stop) (let [user (users/stop-exercise user-id)]
                 (reply "Exercise cancelled")
                 (reply-prompt reply user))

      (= message btn-exercise) (let [exercise (users/start-exercise user-id)
                         first-word (first exercise)]
                     (if (< 0 (count exercise))
                       (reply
                         (str "Exercise started, words to go: " (count exercise) ". Your first word: " first-word)
                         (buttons user))
                       (reply "Too few words to start exercise, add more")))

      (= message btn-lessons) (words.lessons/lessons-select reply user)

      (= message btn-lang) (reply-prompt reply (users/reset-langs user-id))

      (some #(= message %) btns-langs) (let [lang (get btn-to-lang message)
                                             user (case (:state user)
                                                    "lang-from" (users/set-lang-from user-id lang)
                                                    "lang-to" (users/set-lang-to user-id lang)
                                                    (do (c/log "UNKNOWN STATE" (:state user))
                                                        {:state :none}))]
                                         (reply-prompt reply user))

      true (handle-message (:state user) user-id message reply))))

