(ns words.fb-api
  (:gen-class)
  (:require [fb-messenger.auth :as fb]
            [environ.core :refer [env]]))

(def verify-token (env :facebook-verify-token))

(defn bot-authenticate [{:keys [params]}]
  (fb/authenticate verify-token params))

(defn handle-message [request on-message on-postback on-payload on-referral on-quick-reply on-attachments]
  (println "Incoming Request:")
  (println request)
  (let [data (get-in request [:params])]
    (println "request params:")
    (println data)
    (= (:object data) "page")
    (doseq [page-entry (:entry data)]
      (doseq [messaging-event (:messaging page-entry)]
        ; Check for message (onMessage) or postback (onPostback) here
        (cond (contains? messaging-event :postback) (on-postback messaging-event)
              (contains? messaging-event :referral) (on-referral messaging-event)
              (contains? messaging-event :message) (cond (contains? (:message messaging-event) :attachments) (on-attachments messaging-event)
                                                         (contains? (:message messaging-event) :quick_reply) (on-quick-reply messaging-event)
                                                         :else (on-message messaging-event))
              :else (println (str "Webhook received unknown messaging-event: " messaging-event)))))))

