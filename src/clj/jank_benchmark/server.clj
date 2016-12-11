(ns jank-benchmark.server
  (:gen-class)
  (:require [jank-benchmark.handler :refer [app]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3001"))]
    (run-jetty app {:port port :join? false})))
