(ns words.fb.bot
  (:gen-class)
  (:require [clojure.string :as s]
            [environ.core :refer [env]]
            [words.fb.messages :as msg]
            [words.controller :as controller]))

(defn- handle
  [sender-id message-text]
  (let [reply (fn
                ([reply-text]
                 (println "REPLY " reply-text)
                 (msg/send-message sender-id reply-text))
                ([reply-text buttons]
                 (println "REPLY BUTTONS " reply-text buttons)
                 (if (seq buttons)
                   (msg/send-quick-reply sender-id reply-text buttons)
                   (msg/send-message sender-id reply-text))))]
    (controller/handle sender-id message-text reply)))

(defn on-message [payload]
  (println "on-message payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        message-text (get-in payload [:message :text])]

    (handle sender-id message-text)
  ))

(defn on-payload [payload]
  (println "on-payload payload:")
  (println payload)
  (let [action (get-in payload [:payload])
        sender-id (get-in payload [:psid])]
    (msg/referral-fallback sender-id)))

(defn on-postback [payload]
  (println "on-postback payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        postback (get-in payload [:postback :payload])
        referral (get-in payload [:postback :referral :ref])]
    (handle sender-id postback)
    ))

(defn on-referral [payload]
  (println "on-referral payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        referral (get-in payload [:referral :ref])]
    (msg/referral-fallback sender-id)))

(defn on-quick-reply [payload]
  (println "on-quick-reply payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        quick-reply (get-in payload [:message :quick_reply :payload])
        text (get-in payload [:message :text])]
    (println quick-reply)
    (println text)
    (handle sender-id text)
    ))

(defn on-attachments [payload]
  (println "on-attachment payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        attachments (get-in payload [:message :attachments])]
    (msg/attachment-fallback sender-id)))
