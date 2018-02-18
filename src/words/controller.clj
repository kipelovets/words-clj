(ns words.controller
  (:require [words.users :as users]
            [clojure.string :as str]
            [words.dic.glosbe :as dic]
            [words.tr :refer :all]
            [words.storage :as storage]
            [words.common :as c]))

(def ^:private btn-en "🇺🇸 EN")
(def ^:private btn-de "🇩🇪 DE")
(def ^:private btn-ru "🇷🇺 RU")
(def ^:private btn-pl "🇵🇱 PL")
(def ^:private btn-show-words "📖 Show my words")
(def ^:private btn-help "❓ Help")
(def ^:private btn-exercise "📝 Start exercise")
(def ^:private btn-remove "🙅‍♂️ Remove word")
(def ^:private btn-cancel "🤚 Cancel")
(def ^:private btn-stop "🤚 Stop exercise")
(def ^:private btn-lang "⚙️ Change languages")

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
        default-buttons [btn-show-words btn-help]
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

(defmulti handle-message (fn [state _ _ _] (or state :none)))

(defmethod handle-message :none [_ user-id message reply]
  (reply "Oops, you're in unknown state"))

(defmethod handle-message "word" [_ user-id message reply]
  (let [word (str/lower-case message)]
    (if (storage/get-word user-id word)
      (reply (str "Word '" word "' already exists, choose another"))
      (let [user (users/start-adding-word user-id word)
            translations (if-let [t (dic/translation word (:lang-to user) (:lang-from user))] t ["Nothing found"])]
        (reply (prompt user translations)
               (buttons {:id (:id user) :state "translation" :translations translations}))))))

(defmethod handle-message "translation" [_ user-id message reply]
  (let [word (str/lower-case message)
        user (users/finish-adding-word user-id word)]
    (reply (str "Translation saved " (:word user) " -> " word))
    (reply (prompt user) (buttons {:state "translation-added" :id (:id user)}))))

(defmethod handle-message "exercise" [_ user-id message reply]
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
    (cond
      (= message "/start") (let [user (users/add-user user-id)]
                             (reply (:welcome tr) (buttons user))
                             (reply-prompt user))

      (= message btn-help) (do (reply (users/desc-state user-id) (buttons {:id (:id user) :state "help"})))

      (= message btn-show-words) (do (reply (if-let [words-list (not-empty (users/desc-words user-id))]
                                              words-list
                                              "You have no words yet"))
                         (reply-prompt (assoc user :state "desc-words")))

      (= message btn-cancel) (case (:state user)
                   "translation" (let [user (users/reset user-id)]
                                   (reply-prompt user))
                   "word" (let [user (users/start-adding-word user-id (:word user))]
                            (reply-prompt user))
                   "remove-word" (reply-prompt (users/reset user-id))
                   (reply "Nothing to cancel"))

      (= message btn-remove) (let [user (users/start-removing-word user-id)]
                   (reply (prompt user) (buttons user)))

      (= message btn-stop) (let [user (users/stop-exercise user-id)]
                 (reply "Exercise cancelled")
                 (reply (prompt user) (buttons user)))

      (= message btn-exercise) (let [exercise (users/start-exercise user-id)
                         first-word (first exercise)]
                     (if (< 0 (count exercise))
                       (reply
                         (str "Exercise started, words to go: " (count exercise) ". Your first word: " first-word)
                         (buttons user))
                       (reply "Too few words to start exercise, add more")))

      (= message btn-lang) (reply-prompt (users/reset-langs user-id))

      (some #(= message %) btns-langs) (let [lang (get btn-to-lang message)
                                             user (case (:state user)
                                                    "lang-from" (users/set-lang-from user-id lang)
                                                    "lang-to" (users/set-lang-to user-id lang)
                                                    (do (c/log "UNKNOWN STATE" (:state user))
                                                        {:state :none}))]
                                         (reply-prompt user))

      true (handle-message (:state user) user-id message reply))))

