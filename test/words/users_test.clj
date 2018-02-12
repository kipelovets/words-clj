(ns words.users-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]))

(def words {
            "pies"   "dog"
            "kot"    "cat"
            "koń"    "horse"
            "krowa"  "cow"
            "świnia" "pig"
            "koza"   "goat"
            })

(storage/select-db 15)
(storage/clear-all)

(defn prepare-user []
  (add-user 1)
  (doall (map (fn [[word translation]]
                (do
                  (start-adding-word 1 word)
                  (finish-adding-word 1 translation)))
              words)))

(deftest test-add-word
  (testing "Words"

    (prepare-user)

    (is (= 6 (count (storage/get-words 1))))))

(deftest test-exercise
  (testing "Gen exercise"

    (prepare-user)
    (def ex (start-exercise 1))
    (is (= "exercise" (:state (storage/get-user 1))))

    (is (= 5 (count ex)))

    (loop [w ex]
      (is (exercise-answer 1 (get words (first w))))
      (if (< 1 (count w))
        (recur (rest w))))

    (is (= 0 (count (:exercise (storage/get-user 1)))))
    (is (= "word" (:state (storage/get-user 1)))))

  (doall (map (fn [w]
                (is (= 1 (:strength (get (storage/get-words 1) w))))) ex))
  )