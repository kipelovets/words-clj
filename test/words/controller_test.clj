(ns words.controller-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]
            [words.storage.redis :as redis]
            [words.controller :as controller]
            [words.state :as state]
            [clojure.string :as str]
            [words.users :as users]))

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
      (expect-in prompt expected-replies)
      (expect-intersect expected-buttons buttons))
     ([prompt]
      (expect-in prompt expected-replies))))
  )

(defn expect-state [state]
  (is (= state (:state (storage/get-user 1)))))

(defn send
  ([message]
    (controller/handle 1 message (constantly nil)))
  ([message expected-replies expected-buttons expected-state]
   (controller/handle 1 message
                      (expect-replies expected-replies expected-buttons))
   (expect-state expected-state)))

(deftest test-controller
  (testing "default state"
    (add-user 1)
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
(println "0")

    (send "dog"
          '["Translation saved: pies -> dog"]
          [state/btn-lessons]
          "word")
(println "1")

    (send state/btn-lessons
          '["Lessons"]
          ["Celownik"]
          users/state-select-lesson)
(println "2")

    (send "Celownik"
          '["Welcome to celownik" "Celownik is a Polish version of Dative case"]
          ["Continue"]
          users/state-lesson)
(println "3")

    (for [[_ repl expected] (words.lessons/get-lesson "pl" "Celownik")]
      (send (if (= :expect repl) expected repl)))
(println "4")

    (expect-state users/state-word)
(println "5")

    ))