(ns words.lessons
  (:require [words.users :as users]
            [words.state :as state]
            [clojure.string :as string]
            [words.storage.redis :as redis]))

(defn prepare-debug-lessons []
  (redis/save-rel :language "pl" :lessons "Celownik" {:steps [                                                       ;steps
     ; [prompts] [buttons] correct-answer
     [["Welcome to celownik" "Celownik is a Polish version of Dative case"] ["Continue"] "Continue"]
     [["Endings:" "Masculine: -owi, -u\nNeuter: -u\nFeminine: -(i)e/-y/-i\nPlural: -om"] ["Continue"] "Continue"]
     [["Now exercise"] ["Start"] "Start"]
     [["Convert to Celownik: lekarz"] [] "lekarzowi"]
     [["Convert to Celownik: brat"] [] "bratu"]
     [["Convert to Celownik: serce"] [] "sercu"]
     [["Convert to Celownik: koleżanka"] [] "koleżance"]
     [["Convert to Celownik: babcia"] [] "babciom"]
     [["Convert to Celownik: małe dziecko"] [] "małemu dziecku"]
     [["Convert to Celownik: starsza kobieta"] [] "starszej kobiecie"]
     [["Convert to Celownik: małe dzieci"] [] "małym dzieciom"]
     [["Good job!"] [] ""]
     ]}))

(defn get-lesson-names [language]
  (redis/list-rel :language language :lessons))

(defn get-lesson [language lesson]
  (assoc (redis/fetch-rel :language language :lessons lesson) :id lesson :name lesson))

(defn get-lessons [language]
  (let [lessons (get-lesson-names language)]
    (map (fn [name] (assoc (get-lesson language name) :name name :id name)) lessons)))

(defn remove-lesson [language id]
  (redis/remove-rel :language language :lessons id))

(defn- lessons-buttons [lessons] (conj lessons state/btn-cancel))

(defn save-lesson [language id data]
  (redis/save-rel :language language :lessons id data))

(defn lessons-select [reply user]
  (let [lessons (get-lesson-names (:lang-to user))]
    (if (> (count lessons) 0)
      (do
        (users/lessons-select (:id user))
        (reply (str "Available lessons:\n" (string/join "\n" lessons)) (lessons-buttons lessons)))
      (reply "No available lessons"))
    ))

(defn- send-lesson-step [reply step]
  (let [[prompt answers] step]
    (doseq [text prompt] (reply text answers))))

(defn lesson-start [reply user lesson-name]
  (let [language (:lang-to user)
        lessons (get-lesson-names language)
        lesson (get-lesson language lesson-name)
        step (get (:steps lesson) 0)]
    (if (some #{lesson-name} lessons)
      (do
        (println "Lesson selected " language lesson step)
        (send-lesson-step reply step)
        (users/lesson-start (:id user) lesson-name)
        )
      (do
        (reply "Please select a lesson" (lessons-buttons lessons))))))

(defn lesson-step [reply user message]
  (let [user-id (:id user)
        lesson-steps (:steps (get-lesson (:lang-to user) (:lesson user)))
        step (get lesson-steps (Integer. (:step user)))
        [prompt answers correct-answer] step
        ] ; check if step doesn't exist
        (if (= correct-answer message)
            (let [next-step-id (users/lesson-step user-id)
                  next-step (get lesson-steps next-step-id)
                  subsequent-step (get lesson-steps (inc next-step-id))]
              (if subsequent-step
                (do (reply "Correct!")
                    (send-lesson-step reply next-step)
                    (println "SUBS" subsequent-step))
                (do (reply "Correct! Lesson finished")
                    (users/reset user-id))))
            (reply "Incorrect, please try again"))))

