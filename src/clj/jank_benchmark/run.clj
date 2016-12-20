(ns jank-benchmark.run
  (:refer-clojure :exclude [run!])
  (:require [me.raynes.fs :as fs]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]))

(def queue (atom []))
(def runner-sleep-ms 1000)
(def data-file "stored-data")

(defn read-data []
  (let [data (if (fs/exists? data-file)
               (read-string (slurp data-file))
               [])]
    data))

(def current-data (atom (read-data)))

(defn write-data [data]
  (spit data-file (pr-str data)))

(defn sh! [& args]
  (let [result (apply clojure.java.shell/sh args)
        exit (:exit result)]
    (assert (zero? exit) (str "error " exit ": " args " -- " (:err result)))
    result))

(defn checkout! [commit]
  ; TODO: spec/conform commit
  (assert (re-matches #"^[a-zA-Z0-9]{7,40}$" commit) "invalid commit format")
  (let [temp-dir (fs/temp-dir "jank-benchmark")
        jank-dir (str temp-dir "/jank")]
    (println (str commit " - Cloning jank into " jank-dir))
    (sh! "git" "clone" "-q" "--single-branch"
         "https://github.com/jeaye/jank.git"
         :dir temp-dir)
    (sh! "git" "checkout" commit :dir jank-dir)
    (sh! "git" "submodule" "update" "--recursive" "--init" :dir jank-dir)
    jank-dir))

(defn run! [request]
  ; TODO: spec/conform request; have spec check for master branch
  (let [commit (:after request)]
    (if (some (comp #{commit} :commit) @current-data)
      (println (str commit " - Already ran this benchmark"))
      (let [jank-dir (checkout! commit)
            _ (println (str commit " - Running benchmark"))
            ; Get deps first so dep output isn't in benchmark output
            deps (sh! "lein" "with-profile" "benchmark" "deps" :dir jank-dir)
            sh-result (sh! "lein" "with-profile" "benchmark" "run" :dir jank-dir)
            _ (println (str commit " - Storing results"))
            _ (println (str commit " - " (:out sh-result)))
            data (read-string (:out sh-result))]
        (swap! current-data #(->> (assoc data :commit commit)
                                  (conj %)
                                  (sort-by :commit-timestamp)))
        (write-data @current-data)))))

(defn enqueue! [request]
  (swap! queue
         #(let [commit (:after request)
                new-request (assoc request :running? false)]
            (if (or (some (comp #{commit} :commit) @current-data)
                    (some (comp #{commit} :after) %))
              ; Already queued
              %
              (conj % new-request)))))

(defn run-queue! []
  (when (not-empty @queue)
    (swap! queue assoc-in [0 :running?] true)
    (try
      (run! (first @queue))
      (catch Exception e
        (println (str "Exception: " e)))
      (finally
        ; Pop from the queue
        (swap! queue subvec 1))))
  (Thread/sleep runner-sleep-ms)
  (recur))
