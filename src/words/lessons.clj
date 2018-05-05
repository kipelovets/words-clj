(ns words.lessons
  (:require [words.users :as users]
            [words.state :as state]
            [clojure.string :as string]
            [words.storage.redis :as redis]))

(defn prepare-debug-lessons []
  (redis/save-rel :language "pl" :lessons "Celownik" {:steps [                                                       ;steps
     [["Welcome to celownik" "Celownik is a Polish version of Dative case"] "Continue"]
     [["Endings:" "Masculine: -owi, -u\nNeuter: -u\nFeminine: -(i)e/-y/-i\nPlural: -om"] "Continue"]
     [["Now exercise"] "Start"]
     [["Convert to Celownik: lekarz"] :expect "lekarzowi"]
     [["Convert to Celownik: brat"] :expect "bratu"]
     [["Convert to Celownik: serce"] :expect "sercu"]
     [["Convert to Celownik: koleżanka"] :expect "koleżance"]
     [["Convert to Celownik: babcia"] :expect "babciom"]
     [["Convert to Celownik: małe dziecko"] :expect "małemu dziecku"]
     [["Convert to Celownik: starsza kobieta"] :expect "starszej kobiecie"]
     [["Convert to Celownik: małe dzieci"] :expect "małym dzieciom"]
     [["Good job!"] :finish]
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
  (let [resp (get step 1)
        rpl (fn [text]
              (if (= :expect resp) (reply text) (reply text [resp])))
        ]
    (doseq [text (get step 0)] (rpl text))))

(defn lesson-start [reply user lesson-name]
  (let [language (:lang-to user)
        lessons (get-lesson-names language)
        lesson (get-lesson language lesson-name)
        step (get lesson 0)]
    (if (some #{lesson-name} lessons)
      (do
        (send-lesson-step reply step)
        (users/lesson-start (:id user) lesson-name)
        )
      (do
        (reply "Please select a lesson" (lessons-buttons lessons))))))

(defn lesson-step [reply user message]
  (let [user-id (:id user)
        lesson (:steps (get-lesson (:lang-to user) (:lesson user)))
        step (get lesson (Integer. (:step user)))
        resp (get step 1)
        expected (get step 2)
        expected-message (if (= :expect resp) expected resp)
        ] ; check if step doesn't exist
        (if (= expected-message message)
            (let [next-step-id (users/lesson-step user-id)
                  next-step (get lesson next-step-id)
                  finish-step (= :finish (get next-step 1))]
              (if (not finish-step)
                (do (reply "Correct!")
                    (send-lesson-step reply next-step))
                (do (reply "Correct! Lesson finished")
                    (users/reset user-id))))
            (reply "Incorrect, please try again"))))

