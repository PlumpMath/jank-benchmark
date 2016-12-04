(ns jank-benchmark.core
    (:require [jank-benchmark.poll :as poll]
              [jank-benchmark.util :as util]
              [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljsjs.recharts]
              [cljsjs.react-grid-layout]
              [clojure.pprint :refer [pprint]]))

;; -------------------------
;; Views

; TODO: Allow interactive tweaking of this (put it in a ratom)
(def views [[:tests] [:fib-compile :fib-run-40]])

(def layout (reagent/atom
              (map-indexed (fn [i v]
                             {:i (str i)
                              :x (* i 4) :y 0
                              :w 4 :h 8
                              :minW 4 :minH 8})
                           views)))

(defn home-page []
  (let [results (map :results @poll/data)
        _ (pprint layout)]
    [:> (js/ReactGridLayout.WidthProvider js/ReactGridLayout)
     {:className "layout"
      :layout @layout
      :onLayoutChange #(reset! layout %)
      :cols 12
      :rowHeight 30}
     (map-indexed
       (fn [i v]
         [:div {:key (str i)
                :style {:background-color "#657b83"}}
          (let [points (map #(util/extract % v) results)]
            [:> js/Recharts.LineChart {;:width 500 :height 300
                                       :margin {:top 5 :right 30
                                                :left 20 :bottom 5}
                                       :data points}
             ;[:> js/Recharts.XAxis {:dataKey "name"}]
             [:> js/Recharts.XAxis]
             [:> js/Recharts.YAxis]
             [:> js/Recharts.CartesianGrid {:strokeDasharray "3 3"}]
             [:> js.Recharts.Tooltip]
             [:> js/Recharts.Legend]
             (for [k v]
               [:> js/Recharts.Line {:type "monotone"
                                     :dataKey k
                                     :activeDot {:r 8}}])])])
       views)]))

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
  (poll/init!)
  (mount-root))
