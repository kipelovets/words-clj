(ns words.controller-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]
            [words.storage.redis :as redis]
            [words.controller :as controller]
            [words.state :as state]
            [clojure.string :as str]
            [words.users :as users]
            [words.test-common :refer :all]))

(use-fixtures :each
              (fn [f]
                (redis/select-db 15)
                (redis/clear-all)
                (words.lessons/prepare-debug-lessons)
                (dbg-println "PREPARING")
                (f)
                ))

(deftest test-controller
  (testing "adding user"
    (send "/start"
          '["Welcome! Start building your dictionary by adding words with translations"
            "Select your native language"]
          [state/btn-en]
          "lang-from")

    (send "🇺🇸 EN"
          '["Select the language you're learning"]
          [state/btn-pl]
          "lang-to")

    (send "🇵🇱 PL"
          '["Send a new word to train"]
          [state/btn-lessons]
          "word")
    )

  (testing "adding words"
    (set-state 1 "word")
    (send "kaczka"
          '["Possible translations: duck, canard, urine bottle, ducks, quack-quack\nNow send your selected translation"]
          []
          "translation")

    (send "duck"
          '["Translation saved: kaczka -> duck" "Send a new word to train"]
          [state/btn-lessons]
          "word")
    (doall (map-indexed (fn [word trans] (send word) (send trans)) {
            "pies"   "dog"
            "kot"    "cat"
            "koń"    "horse"
            "krowa"  "cow"
            "świnia" "pig"
            "koza"   "goat"
            }))
    (expect-state "word")

    (is (= 7 (count (storage/get-words 1))))
    )

  (testing "cancel selecting lesson"
    (send state/btn-lessons
          '["Available lessons:\nCelownik"]
          ["Celownik"]
          users/state-select-lesson)
    (send state/btn-cancel
          '["Send a new word to train"]
          [state/btn-show-words]
          users/state-word)
    )
  )
