(ns jank-benchmark.middleware
  (:require [ring.middleware
             [defaults :refer [secure-site-defaults wrap-defaults]]
             [gzip :as gzip]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults (-> secure-site-defaults
                         (assoc :cookies false)
                         (assoc :session {})
                         (assoc :proxy true)
                         (assoc-in [:static :resources] "/")
                         (assoc-in [:security :hsts] false)
                         (assoc-in [:security :ssl-redirect] false)))
      gzip/wrap-gzip))
