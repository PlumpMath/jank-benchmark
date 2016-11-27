(ns jank-benchmark.middleware
  (:require [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [json :refer [wrap-json-response]]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      wrap-json-response
      wrap-exceptions
      wrap-reload))
