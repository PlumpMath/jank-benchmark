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
              [cljs.reader :as reader]
              [clojure.pprint :refer [pprint write]]))

;; -------------------------
;; Views

(def views (reagent/atom [[:tests]
                          [:empty-compile :empty-run]
                          [:fib-compile :fib-run-40]]))

(def grid-cols 9)
(def row-height 150)

(def cell-width 3)
(def cell-height 3)
(def cell-margin [0 0])
(def cell-cols (/ grid-cols cell-width))

(defn generate-layout
  [vs]
  (map-indexed (fn [i v]
                 {:i (str i)
                  :x (* cell-width (mod i cell-cols))
                  :y (* cell-height (int (/ i cell-cols)))
                  :w cell-width :h cell-height
                  :minW cell-width :minH cell-height
                  :maxW cell-width :maxH cell-height})
               vs))

(def layout (reagent/atom (generate-layout @views)))

(defn parse-input [evt]
  (let [str-value (-> evt .-target .-value)]
    (try
      (let [value (reader/read-string str-value)]
        (when (not= views value)
          (reset! views value)
          (reset! layout (generate-layout @views)))
        true)
      (catch js/Object e
        ; Don't change anything; the input isn't valid
        true))))

(def formatter (time-format/formatter "MMM d"))
(defn format-timestamp [stamp]
  (time-format/unparse formatter (time-coerce/from-long stamp)))

; TODO: Show queue
(defn home-page []
  (let [results (map #(-> %
                          :results
                          (assoc :commit-timestamp
                                 (format-timestamp (:commit-timestamp %))))
                     @poll/data)]
    (if (empty? results)
      [:div "no results"]
      [:div
       [:div {:class "input"}
        [:textarea {:style {:width "100%"
                            :height "20%"
                            :font-size 20}
                    :default-value (write @views :stream nil)
                    :on-input parse-input}]]

       [:div {:class "grid"
              :style {:margin 10
                      :padding 10}}
        [:> (js/ReactGridLayout.WidthProvider js/ReactGridLayout)
         {:className "layout"
          :layout @layout
          :margin cell-margin
          :cols grid-cols
          :rowHeight row-height}
         (map-indexed
           (fn [i v]
             [:div {:key (str i)}
              (let [all-points (map #(util/extract % (conj v :commit-timestamp))
                                    results)
                    ; Some test may be added in later commits, so some points
                    ; may have nil results. Filter out points in a view where
                    ; all tests are nil; allow the case where some are nil
                    ; and just show the value as 0 for that.
                    valid-points (filter #(every? some? ((apply juxt v) %))
                                         all-points)]
                [:> js/Recharts.ResponsiveContainer
                 [:> js/Recharts.LineChart {:data valid-points}
                  [:> js/Recharts.XAxis {:dataKey "commit-timestamp"}]
                  [:> js/Recharts.YAxis]
                  [:> js/Recharts.CartesianGrid {:strokeDasharray "3 3"}]
                  [:> js.Recharts.Tooltip {:isAnimationActive false}]
                  [:> js/Recharts.Legend]
                  (for [k v]
                    [:> js/Recharts.Line {:type "monotone"
                                          :key (str i "-" k)
                                          :dataKey k
                                          :isAnimationActive false}])]])])
           @views)]]])))

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
