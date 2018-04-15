(ns words.lessons
  (:require [words.users :as users]
            [words.state :as state]
            [clojure.string :as string]))

(def lessons
  {
   "pl"
   {"Celownik"
    [                                                       ;steps
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
     ]
    }
   })

(defn get-lesson-names [language]
  (keys (get lessons language)))

(defn- lessons-buttons [lessons] (conj lessons state/btn-cancel))

(defn get-lesson [language lesson]
  (get (get lessons language) lesson))

(defn lessons-select [reply user]
  (let [lessons (get-lesson-names (:lang-to user))]
    (users/lessons-select (:id user))
    (reply (str "Available lessons:\n" (string/join "\n" lessons)) (lessons-buttons lessons))))

(defn- send-lesson-step [reply step]
  (let [resp (get step 1)
        rpl (fn [text] (if (= :expect resp) (reply text) (reply text [resp])))]
    (doseq [text (get step 0)] (rpl text)))

(defn lesson-start [reply user lesson-name]
  (let [language (:lang-to user)
        lessons (get-lesson-names language)
        lesson (get-lesson language lesson-name)
        step (get lesson 0)]
    (if (some #{lesson-name} lessons)
      (do
        (send-lesson-step reply step)
        (users/lesson-start (:id user) lesson-name)))
      (reply "Please select a lesson" (lessons-buttons lessons)))))

(defn lesson-step [reply user message]
  (let [user-id (:id user)
        lesson (get-lesson (:lang-to user) (:lesson user))
        step (get lesson (:step user))
        resp (get step 1)
        expected (get step 2)
        ] ; check if step doesn't exist
        (if (= :expect resp)
          (if (= expected message)
            (let [next-step-id (users/lesson-step user-id)
                  next-step (get lesson next-step-id)]
              (if next-step
                (do (reply "Correct!")
                    (send-lesson-step reply next-step))
                (do (reply "Correct! Lesson finished")
                    (users/reset user-id))))))))
