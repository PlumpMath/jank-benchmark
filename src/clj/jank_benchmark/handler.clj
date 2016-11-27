(ns jank-benchmark.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [jank-benchmark.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(def data [{:name "Jan 1" :uv 4000 :pv 2400 :amt 2400}
           {:name "Feb 1" :uv 3000 :pv 1398 :amt 2210}
           {:name "Mar 1" :uv 2000 :pv 9800 :amt 2290}
           {:name "Apr 1" :uv 2780 :pv 3908 :amt 2000}
           {:name "May 1" :uv 1890 :pv 4800 :amt 2181}
           {:name "Jun 1" :uv 2390 :pv 3800 :amt 2500}
           {:name "Jul 1" :uv 3490 :pv 4300 :amt 2100}
           {:name "Aug 1" :uv 6490 :pv 5300 :amt 2100}])

(defroutes app-routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/api/stats" [] data)
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'app-routes))
