(ns words.lesson-test
  (:require [clojure.test :refer :all]
            [words.state :as state]
            [words.users :as users]
            [words.lessons :as lessons]
            [words.test-common :refer :all]
            ))

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

(deftest lesson-test
  (testing "lessons"
    (send state/btn-lessons
          '["Available lessons:\nCelownik"]
          ["Celownik"]
          users/state-select-lesson)

    (send "WRONG LESSON"
          '["Please select a lesson"]
          ["Celownik"]
          users/state-select-lesson)

    (send "Celownik"
          '["Welcome to celownik" "Celownik is a Polish version of Dative case"]
          ["Continue"]
          users/state-lesson)

    (let [steps (:steps (lessons/get-lesson "pl" "Celownik"))]
      ;(dbg-println "Going through lesson: " (clojure.data.json/write-str steps))
      (doseq [[prompt buttons expected] steps]
        (if (= "" expected) nil (send expected))))

    (expect-state users/state-word)

    ))
