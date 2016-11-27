(ns jank-benchmark.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljsjs.recharts]))

;; -------------------------
;; Views

(def data [{:name "Jan 1" :uv 4000 :pv 2400 :amt 2400}
           {:name "Feb 1" :uv 3000 :pv 1398 :amt 2210}
           {:name "Mar 1" :uv 2000 :pv 9800 :amt 2290}
           {:name "Apr 1" :uv 2780 :pv 3908 :amt 2000}
           {:name "May 1" :uv 1890 :pv 4800 :amt 2181}
           {:name "Jun 1" :uv 2390 :pv 3800 :amt 2500}
           {:name "Jul 1" :uv 3490 :pv 4300 :amt 2100}
           {:name "Aug 1" :uv 6490 :pv 5300 :amt 2100}])

(defn home-page []
  [:> js/Recharts.LineChart {:width 1000
                             :height 700
                             :margin {:top 5, :right 30, :left 20, :bottom 5}
                             :data data}
   [:> js/Recharts.XAxis {:dataKey "name"}]
   [:> js/Recharts.YAxis]
   [:> js/Recharts.CartesianGrid {:strokeDasharray "3 3"}]
   [:> js.Recharts.Tooltip]
   [:> js/Recharts.Legend]
   [:> js/Recharts.Line {:type "monotone" :dataKey "pv" :stroke "#8884d8" :activeDot {:r 8}}]
   [:> js/Recharts.Line {:type "monotone" :dataKey "uv" :stroke "#82ca9d"}]])

(defn about-page []
  [:div [:h2 "About jank-benchmark!"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
