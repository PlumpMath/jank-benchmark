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
(def views [[:tests] [:fib-compile :fib-run-40]
            [:tests] [:fib-compile :fib-run-40]
            ;[:tests] [:fib-compile :fib-run-40]
            ;[:tests] [:fib-compile :fib-run-40]
            ;[:tests] [:fib-compile :fib-run-40]
            ;[:tests] [:fib-compile :fib-run-40]
            ])

(def cell-width 3) ; TODO: Map for these
(def cell-height 3)
(def cols 12)
(def cell-margin [0 0])
(def row-height 150)

(def window-width 1900)
(def grid-width (/ window-width (/ cols cell-width)))
(def grid-height (* row-height cell-height))

(def layout (reagent/atom
              (map-indexed (fn [i v]
                             {:i (str i)
                              :x (* i cell-width) :y 0
                              :w cell-width :h cell-height
                              :minW cell-width :minH cell-height})
                           views)))

(defn home-page []
  (let [results (map :results @poll/data)]
    [:> (js/ReactGridLayout.WidthProvider js/ReactGridLayout)
     {:className "layout"
      :layout @layout
      :onLayoutChange #(reset! layout %)
      :margin cell-margin
      :cols cols
      :rowHeight row-height}
     (map-indexed
       (fn [i v]
         [:div {:key (str i)
                ;:style {:background-color "#657b83"}
                }
          (let [points (map #(util/extract % v) results)]
            [:> js/Recharts.ResponsiveContainer
             [:> js/Recharts.LineChart {:data points}
             ;[:> js/Recharts.XAxis {:dataKey "name"}]
             [:> js/Recharts.XAxis]
             [:> js/Recharts.YAxis]
             [:> js/Recharts.CartesianGrid {:strokeDasharray "3 3"}]
             ;[:> js.Recharts.Tooltip]
             [:> js/Recharts.Legend]
             (for [k v]
               [:> js/Recharts.Line {:type "monotone"
                                     :dataKey k
                                     :isAnimationActive false
                                     :activeDot {:r 8}}])]])])
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
