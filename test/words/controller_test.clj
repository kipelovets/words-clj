(ns words.controller-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]
            [words.storage.redis :as redis]
            [words.controller :as controller]
            [words.state :as state]
            [clojure.string :as str]
            [words.users :as users]))

(redis/select-db 15)
(redis/clear-all)

(defn expect-in [needle haystack]
  (is (some #{needle} haystack)
      (str "Expecting '" needle "' in '" (str/join haystack) "'")))

(defn expect-intersect [expected actual]
  (if (empty? expected)
    true
    (is
      (< 0 (count (clojure.set/intersection (set expected) (set actual))))
      (str "Intersect '" (str/join "," expected) "' and '" (str/join "," actual) "'")
      )))

(defn expect-replies
  ([expected-replies]
   (fn
     ([prompt _]
      (expect-in prompt expected-replies))
     ([prompt]
      (expect-in prompt expected-replies))))
  ([expected-replies expected-buttons]
   (fn
     ([prompt buttons]
      (println "Received reply: " prompt)
      (expect-in prompt expected-replies)
      (expect-intersect expected-buttons buttons))
     ([prompt]
      (println "Received reply: " prompt)
      (expect-in prompt expected-replies))))
  )

(defn expect-state [state]
  (println "Expecting state " state)
  (is (= state (:state (storage/get-user 1))))
  )

(defn any-replies
  ([prompt] (doall (println "REPLY " prompt)))
  ([prompt buttons] (doall (println "REPLY " prompt " BUTTONS " buttons))))

(defn send
  ([message]
   (println "Sending message: " message)
    (controller/handle 1 message println))
  ([message expected-replies expected-buttons expected-state]
   (println "Sending message (expecting replies): " message)
   (controller/handle 1 message
                      (expect-replies expected-replies expected-buttons))
   (expect-state expected-state)))

(deftest test-controller
  (testing "default state"
    (println (add-user 1))
    (words.lessons/prepare-debug-lessons)

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

    (send "pies"
          '["Possible translations: dog, hound, pig, Canis familiaris, bull\nNow send your selected translation"]
          []
          "translation")

    (send "dog"
          '["Translation saved: pies -> dog" "Send a new word to train"]
          [state/btn-lessons]
          "word")

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

    (let [steps (:steps (words.lessons/get-lesson "pl" "Celownik"))]
      ;(println "Going through lesson: " (clojure.data.json/write-str steps))
      (doseq [[prompt reply expected] steps]
        (do

          (if (not (= :finish reply))
            (do
              (send (if (= :expect reply) expected reply))
              (println "Sending lesson answer " prompt reply expected))
            (println "Lesson finished"))

          )
        )
      )

    (expect-state users/state-word)

    ))