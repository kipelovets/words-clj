(ns words.ring-core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [words.fb.api :as api]
            [words.fb.bot :as bot]
            [fb-messenger.send :as send]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]
            [clojure.data.json :as json]
            [words.lessons :as lessons]))

(defroutes app-routes
           ;(GET "/" [] "Hello World")

           ; Facebook
           (POST "/webhook" request
                 (api/handle-message request bot/on-message bot/on-postback bot/on-payload bot/on-referral bot/on-quick-reply bot/on-attachments)
                 {:status 200})
           (GET "/webhook" request (api/bot-authenticate request))

           ; API
           (GET "/lessons/pl" [] (json/write-str (lessons/get-lesson-names "pl")))
           (GET "/lessons/pl/:id" [id] (json/write-str (lessons/get-lesson "pl" id)))
           (PUT "/lessons/pl/:id" [id :as request] (json/write-str (lessons/save-lesson "pl" id (:json-params request))))

           (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-file "/app/public")
      (wrap-content-type)
      (wrap-not-modified)
      (wrap-keyword-params)
      (wrap-json-params)))

(def page-access-token (env :facebook-page-key))

(defn init
  []
  (send/set-messenger-profile {
                               :greeting [{:locale "default" :text "Hello"}]
                               :get_started {:payload "/start"}} page-access-token))