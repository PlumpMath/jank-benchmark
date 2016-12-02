(ns jank-benchmark.handler
  (:require [me.raynes.fs :as fs]
            [config.core :refer [env]]))

(def data-file "stored-data")
(def lib-dir "lib/")
(def jank-dir (str lib-dir "jank/"))

(defn read-data []
  (let [data (if (fs/exists? data-file)
               (read-string (slurp data-file))
               [])]
    data))

(def current-data (atom (read-data)))

(defn write-data [data]
  (spit data-file (pr-str data)))

(defn sh [& args]
  (let [result (apply clojure.java.shell/sh args)]
    (assert (zero? (:exit result))
            (str "error " (:exit result) ": " args))
    result))

(defn checkout [commit]
  ; TODO: spec/conform commit
  (assert (re-matches #"^[a-zA-Z0-9]{7,40}$" commit) "invalid commit format")
  (when (not (fs/exists? jank-dir))
    (fs/mkdir lib-dir)
    (println "Cloning jank...")
    (sh "git" "clone" "https://github.com/jeaye/jank.git"
                           :dir lib-dir))
  (sh "git" "fetch" "origin" :dir jank-dir)
  (sh "git" "checkout" commit :dir jank-dir))

(defn run [request]
  ; TODO: Only run if master branch updated
  ; TODO: Don't run multiple times for same commit
  (let [commit (:commit request)
        _ (checkout commit)
        sh-result (sh "lein" "with-profile" "benchmark" "trampoline" "run"
                      :dir jank-dir)
        data (read-string (:out sh-result))]
    (swap! current-data conj data)
    (swap! current-data (partial sort-by :commit-timestamp))
    (write-data @current-data)
    data))
