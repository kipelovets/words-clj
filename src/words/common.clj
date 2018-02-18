(ns words.common
  (:require [clojure.string :as str]))

(defn log [& msgs] (println (str (.getId (Thread/currentThread)) ": " (str/join " " msgs))) true)
