(ns words.users-test
  (:require [clojure.test :refer :all]
            [words.users :refer :all]
            [words.storage :as storage]
            [words.storage.redis :as redis]))

(def words {
            "pies"   "dog"
            "kot"    "cat"
            "koń"    "horse"
            "krowa"  "cow"
            "świnia" "pig"
            "koza"   "goat"
            })


(defn prepare-user []
  (add-user 1)
  (doall (map (fn [[word translation]]
                (do
                  (start-adding-word 1 word)
                  (finish-adding-word 1 translation)))
              words)))

(use-fixtures :each
              (fn [f]
                (redis/select-db 15)
                (redis/clear-all)
                (f)
                ))

(deftest test-users

  (testing "Words"
    (prepare-user)
    (is (= 6 (count (storage/get-words 1)))))

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
    (is (= "word" (:state (storage/get-user 1))))

    (doall (map (fn [w]
                  (is (= "1" (:strength (get (storage/get-words 1) w))))) ex))

    )
  )
