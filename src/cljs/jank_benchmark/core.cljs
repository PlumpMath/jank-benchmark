(ns jank-benchmark.core
    (:require [jank-benchmark.poll :as poll]
              [jank-benchmark.util :as util]
              [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [cljsjs.recharts]
              [cljsjs.react-grid-layout]
              [clojure.pprint :refer [pprint]]))

;; -------------------------
;; Views

; TODO: Allow interactive tweaking of this (put it in a ratom)
(def views (reagent/atom [[:tests]
                          [:empty-compile :empty-run]
                          [:fib-compile :fib-run-40]]))

(def grid-cols 12)
(def row-height 150)

(def cell-width 3)
(def cell-height 3)
(def cell-margin [0 0])
(def cell-cols (/ grid-cols cell-width))

(def layout (reagent/atom
              (map-indexed (fn [i v]
                             {:i (str i)
                              :x (* cell-width (mod i cell-cols))
                              :y (* cell-height (int (/ i cell-cols)))
                              :w cell-width :h cell-height
                              :minW cell-width :minH cell-height})
                           views)))

(def formatter (time-format/formatter "MMM d"))
(defn format-timestamp [stamp]
  (time-format/unparse formatter (time-coerce/from-long stamp)))

(defn home-page []
  (let [results (map #(-> %
                          :results
                          (assoc :commit-timestamp
                                 (format-timestamp (:commit-timestamp %))))
                     @poll/data)]
    (if (empty? results)
      [:div "no results"]
      [:> (js/ReactGridLayout.WidthProvider js/ReactGridLayout)
       {:className "layout"
        :layout @layout
        :onLayoutChange #(reset! layout %)
        :margin cell-margin
        :cols grid-cols
        :rowHeight row-height}
       (map-indexed
         (fn [i v]
           [:div {:key (str i)}
            (let [points (map #(util/extract % v) results)]
              [:> js/Recharts.ResponsiveContainer
               [:> js/Recharts.LineChart {:data points}
                [:> js/Recharts.XAxis]
                [:> js/Recharts.YAxis]
                [:> js/Recharts.CartesianGrid {:strokeDasharray "3 3"}]
                ;[:> js.Recharts.Tooltip]
                [:> js/Recharts.Legend]
                (for [k v]
                  [:> js/Recharts.Line {:type "monotone"
                                        :key (str i "-" k)
                                        :dataKey k
                                        :isAnimationActive false
                                        :activeDot {:r 8}}])]])])
         views)])))

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
