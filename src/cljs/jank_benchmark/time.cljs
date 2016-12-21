(ns jank-benchmark.time
  (:require [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]))

(def formatter (time-format/formatter "MMM d"))
(defn format-timestamp [stamp]
  (time-format/unparse formatter (time-coerce/from-long stamp)))
