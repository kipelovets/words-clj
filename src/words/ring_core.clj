(ns words.ring-core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [words.fb.api :as api]
            [words.fb.bot :as bot]
            [fb-messenger.send :as send]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.data.json :as json]
            [words.lessons :as lessons]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn- lesson-to-api [lesson]
  (defn list-text [arr] (map-indexed (fn [ind text] {:key ind :text text}) arr))
  (assoc lesson :steps (map-indexed (fn [index [prompt buttons answer]]
                              {:key index
                               :prompt (list-text prompt)
                               :answer answer
                               :buttons (list-text buttons)})
                            (:steps lesson))
                :id (:name lesson)))

(defn- keyword-keys [u]
  (zipmap (map keyword (keys u)) (vals u)))

(defn- api-to-lesson [data]
  (defn text-list [arr] (map (fn [row]
                               (let [text (get row "text")] text))
                             arr))
  (assoc data :steps (map (fn [step]
                            (let [stepKw (keyword-keys step)
                                  {prompt :prompt buttons :buttons answer :answer} stepKw]
                              [(text-list prompt)
                               (text-list buttons)
                               answer]))
                          (get data "steps"))
              :id (get data "name")
              :name (get data "name")))

(defroutes app-routes
           ;(GET "/" [] "Hello World")

           ; Facebook
           (POST "/webhook" request
             (api/handle-message request bot/on-message bot/on-postback bot/on-payload bot/on-referral bot/on-quick-reply bot/on-attachments)
             {:status 200})
           (GET "/webhook" request (api/bot-authenticate request))

           ; API
           (GET "/lessons" []
             (let [lessons (map lesson-to-api (lessons/get-lessons "pl"))]
               {:headers {"Content-Range"                 (str (count lessons))
                          "Access-Control-Expose-Headers" "Content-Range"}
                :body    (json/write-str lessons)}))

           (GET "/lessons/:id" [id] (json/write-str (lesson-to-api (lessons/get-lesson "pl" id))))

           (PUT "/lessons/:id" [id :as request]
             (let [req (:json-params request)
                   params (api-to-lesson req)]
               (json/write-str
                 (lesson-to-api
                   (lessons/save-lesson "pl" id (api-to-lesson params))))))

           (POST "/lessons" request
             (let [req (:json-params request)
                   params (api-to-lesson req)]
               (json/write-str
                 (lesson-to-api
                   (lessons/save-lesson "pl" (get params "name") params)))))

           (DELETE "/lessons/:id" [id :as request]
             (do
               (lessons/remove-lesson "pl" id)
               "OK"))

           ;(OPTIONS "/lessons" [] "OK")

           (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes api-defaults)

      (wrap-file "/app/public")
      (wrap-content-type)
      (wrap-not-modified)

      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete]
                 ;:access-control-allow-headers #{:content-range :accept}
                 )
      (wrap-keyword-params)
      (wrap-json-params)
      (wrap-json-response)
      (wrap-reload #'app)
      ))

(def page-access-token (env :facebook-page-key))

(defn init
  []
  (words.storage.redis/select-db 0)
  (send/set-messenger-profile {
                               :greeting    [{:locale "default" :text "Hello"}]
                               :get_started {:payload "/start"}} page-access-token))