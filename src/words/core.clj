(ns words.core
  (:require [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]
            [words.common :as c]
            [words.users :as u]
            [words.storage :as storage]
            [words.lingvo :as lingvo])
  (:use [environ.core]))

(def token (env :telegram-token))

(c/log (str "Telegram token: " token))

(defn welcome-next-exercise-word [id word]
  (t/send-text token id (str "Your next word for translation: *" word "*")))

(defn welcome-add-word [id]
  (t/send-text token id (str "Send a new word to train")))

(h/defhandler bot-api
              (h/command-fn "start" (fn [{{id :id :as chat} :chat}]
                                      (c/log (str "Bot joined new chat: " chat))
                                      (u/add-user id)
                                      (t/send-text token id "Welcome! Start building your dictionary by adding words with translations")
                                      (welcome-add-word id)
                                      ))

              (h/command "help" {{id :id :as chat} :chat}
                         (c/log (str "Help was requested in " chat))
                         (let [message (u/desc-state id)]
                           (t/send-text token id message)))

              (h/command "words" {{id :id :as chat} :chat}
                         (c/log (str "Dumping words for " chat))
                         (t/send-text token id (str "Your words: " (u/desc-words id))))

              (h/command "exercise" {{id :id :as chat} :chat}
                         (let [exercise (u/start-exercise id)
                               first-word (first exercise)]
                           (t/send-text token id (str "Exercise started, words to go: " (count exercise) ))
                           (welcome-next-exercise-word id first-word)))

              (h/message {{id :id :as chat} :chat text :text :as message}
                         (c/log (str "Intercepted message:" message))
                         (let [current-state (:state (storage/get-user id))
                               reply-text (case current-state
                                            "word" (do
                                                     (u/start-adding-word id text)
                                                     (str "Possible translations: " (lingvo/translation text "pl" "ru") "\nNow send your selected translation"))
                                            "translation" (do (u/finish-adding-word id text) "Translation saved. What's the next word?")
                                            "exercise" (if (u/exercise-answer id text)
                                                        (str "Correct! " (if-let [next-word (u/exercise-next-word id)]
                                                                          (str "Your next word: " next-word)
                                                                          (str "Exercise finished!")))
                                                        (str "Sorry, that's not right"))
                                            (str "Oops, you're in unknown state! " current-state))]
                           (t/send-text token id reply-text))))

(defn -main []
  (c/log "Hello")
  (def channel (p/start token bot-api {:timeout 65536}))

  ; TODO: make the Telegram thread non-background instead
  (doall (repeatedly 1000 (fn [] (c/log "Sleeping") (Thread/sleep 60000))))

  (c/log "Bye"))


