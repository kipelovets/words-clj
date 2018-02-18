(ns words.core
  (:require [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]
            [words.common :as c]
            [words.controller :as controller])
  (:use [environ.core]))

(def token (env :telegram-token))

(c/log (str "Telegram token: " token))

(h/defhandler bot-api
              (h/message {{id :id :as chat} :chat text :text :as message}
                         (let [reply (fn
                                       ([reply-text] (t/send-text token id reply-text))
                                       ([reply-text buttons]
                                        (let [btns (map #(repeat 1 %) buttons)]
                                          (t/send-text token id {:reply_markup {:keyboard btns}} reply-text))))]
                           (c/log (str "Intercepted message:" message))
                           (controller/handle id text reply))))

(defn -main []
  (c/log "Hello")
  (def channel (p/start token bot-api {:timeout 65536}))

  ; TODO: make the Telegram thread non-background instead
  (doall (repeatedly 10000000 (fn [] (c/log "Sleeping") (Thread/sleep 60000))))

  (c/log "Bye"))


