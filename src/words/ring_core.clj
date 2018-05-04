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
  (assoc lesson :steps (map-indexed (fn [index [prompt expect answer]]
                              {:key index
                               :prompt (map-indexed (fn [ind text] {:key ind :text text}) prompt)
                               :answer (if (= :expect expect) answer expect)
                               :button (if (= :expect expect) nil expect)})
                            (:steps lesson))
                ))

(defn- api-to-lesson [data]
  (assoc data :steps (map (fn [{prompt :prompt expect :expect button :button}]
                            [(map (fn [{text :text}] text) prompt)
                             (if button button :expect)
                             expect])
                          (:steps data))))

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
             (json/write-str
               (lesson-to-api
                 (lessons/save-lesson "pl" id (api-to-lesson (:json-params request))))))

           (POST "/lessons" request
             (let [params (api-to-lesson (:json-params request))]
               (println request)
               (json/write-str
                 (lesson-to-api
                   (lessons/save-lesson "pl" (get params "id") params)))))

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