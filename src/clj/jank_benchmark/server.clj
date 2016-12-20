(ns jank-benchmark.server
  (:gen-class)
  (:require [jank-benchmark.handler :refer [app]]
            [jank-benchmark.run :as run]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3001"))]
    (future (run/run-queue!))
    (run-jetty app {:port port :join? false})))
