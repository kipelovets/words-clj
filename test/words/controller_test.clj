(ns words.controller-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]
            [words.storage.redis :as redis]
            [words.controller :as controller]
            [words.state :as state]
            [clojure.string :as str]
            [words.users :as users]))

(def ^:dynamic *verbose* false)

(defmacro dbg-println
  [& args]
  `(when *verbose*
     (println ~@args)))

(defmacro with-verbose
  [& body]
  `(binding [*verbose* true] ~@body))

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
      (dbg-println "Received reply: " prompt)
      (expect-in prompt expected-replies)
      (expect-intersect expected-buttons buttons))
     ([prompt]
      (dbg-println "Received reply: " prompt)
      (expect-in prompt expected-replies))))
  )

(defn expect-state [state]
  (dbg-println "Expecting state " state)
  (is (= state (:state (storage/get-user 1))))
  )

(defn any-replies
  ([prompt] (doall (dbg-println "REPLY " prompt)))
  ([prompt buttons] (doall (dbg-println "REPLY " prompt " BUTTONS " buttons))))

(defn send
  ([message]
   (dbg-println "Sending message: " message)
    (controller/handle 1 message (fn [& args] (dbg-println args))))
  ([message expected-replies expected-buttons expected-state]
   (dbg-println "Sending message (expecting replies): " message)
   (controller/handle 1 message
                      (expect-replies expected-replies expected-buttons))
   (expect-state expected-state)))

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

    (let [steps (:steps (words.lessons/get-lesson "pl" "Celownik"))]
      ;(dbg-println "Going through lesson: " (clojure.data.json/write-str steps))
      (doseq [[prompt buttons expected] steps]
        (if (= "" expected) nil (send expected))))

    (expect-state users/state-word)

    ))