(ns jank-benchmark.handler
  (:require [jank-benchmark
             [middleware :refer [wrap-middleware]]
             [css :as css]]
            [ring.util.response :refer [response]]
            [compojure
             [core :refer [GET POST defroutes]]
             [route :refer [not-found]]]
            [hiccup.page :refer [include-js include-css html5]]
            [clojure.data.json :as json]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:style (css/main)]])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     [:div#app
      [:h3 "Loading..."]]
     (include-js "/js/app.js")]))

(defroutes app-routes
  (GET "/" [] (loading-page))
  (GET "/api/stats" [] (response @run/current-data))
  (POST "/api/run" {body :body} (response
                                  (run/run! (json/read-str (slurp body)
                                                           :key-fn keyword))))
  (not-found "Not Found"))

(def app (wrap-middleware #'app-routes))
