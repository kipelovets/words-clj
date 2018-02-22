(ns words.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [words.fb-api :as api]
            [words.fb-bot :as bot]
            [fb-messenger.send :as send]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

(defroutes app-routes
           (GET "/" [] "Hello World")
           (POST "/webhook" request
                 (api/handle-message request bot/on-message bot/on-postback bot/on-payload bot/on-referral bot/on-quick-reply bot/on-attachments)
                 {:status 200})
           (GET "/webhook" request (api/bot-authenticate request))
           (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-keyword-params)
      (wrap-json-params)))

(def page-access-token (env :facebook-page-key))

(defn init
  []
  (send/set-messenger-profile {
                               :greeting [{:locale "default" :text "Hello"}]
                               :get_started {:payload "/start"}} page-access-token))