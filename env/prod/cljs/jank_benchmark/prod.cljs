(ns jank-benchmark.prod
  (:require [jank-benchmark.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
