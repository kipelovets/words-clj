(ns words.test-common
  (:require [clojure.test :refer :all]
            [words.controller :as controller]
            [clojure.string :as str]
            ))

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
