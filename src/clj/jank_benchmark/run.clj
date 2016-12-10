(ns jank-benchmark.run
  (:refer-clojure :exclude [run!])
  (:require [me.raynes.fs :as fs]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]))

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
    (assert (zero? exit) (str "error " exit ": " args))
    result))

(defn checkout! [commit]
  ; TODO: spec/conform commit
  (assert (re-matches #"^[a-zA-Z0-9]{7,40}$" commit) "invalid commit format")
  (let [temp-dir (fs/temp-dir "jank-benchmark")
        jank-dir (str temp-dir "/" jank-dir)]
    (println (str "Cloning jank into " jank-dir))
    (sh! "git" "clone" "-q" "--single-branch"
         "https://github.com/jeaye/jank.git"
         :dir temp-dir)
    (sh! "git" "checkout" commit :dir jank-dir)
    (sh! "git" "submodule" "update" "--recursive" "--init" :dir jank-dir)
    jank-dir))

(defn run! [request]
  ; TODO: Don't run multiple times for same commit
  ; TODO: spec/conform request; have spec check for master branch
  (let [commit (:after request)
        jank-dir (checkout! commit)
        sh-result (sh! "lein" "with-profile" "benchmark" "trampoline" "run"
                       :dir jank-dir)
        data (read-string (:out sh-result))]
    (swap! current-data #(->> (conj % data)
                              (sort-by :commit-timestamp)))
    (write-data @current-data)
    data))
