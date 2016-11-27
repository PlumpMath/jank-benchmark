(ns jank-benchmark.middleware
  (:require [ring.middleware
             [defaults :refer [secure-site-defaults wrap-defaults]]
             [json :refer [wrap-json-response]]
             [gzip :as gzip]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults (-> secure-site-defaults
                         (assoc :cookies false)
                         (assoc :session {})
                         (assoc :proxy true)
                         (assoc-in [:static :resources] "/")
                         (assoc-in [:security :hsts] false)
                         (assoc-in [:security :anti-forgery] false)
                         (assoc-in [:security :ssl-redirect] false)))
      wrap-json-response
      gzip/wrap-gzip))
