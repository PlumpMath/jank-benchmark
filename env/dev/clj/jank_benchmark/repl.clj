(ns jank-benchmark.repl
  (:require [jank-benchmark.run :as run])
  (:use jank-benchmark.handler
        figwheel-sidecar.repl-api
        ring.server.standalone
        [ring.middleware file-info file]))

(defonce server (atom nil))
(def runner (future (run/run-queue!)))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'app
      ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
      ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(defn start-server
  [& [port]]
  (let [port (if port (Integer/parseInt port) 3000)]
    (reset! server
            (serve (get-handler)
                   {:port port
                    :auto-reload? true
                    :join? false}))
    (println (str "You can view the site at http://localhost:" port))))

(defn stop-server []
  (.stop @server)
  (reset! server nil))

(defn restart-server []
  (use 'jank-benchmark.repl :reload)
  (when @server
    (stop-server))
  (start-server))
