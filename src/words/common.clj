(ns words.common)

(defn log [msg] (println (str (.getId (Thread/currentThread)) ": " msg)) true)
