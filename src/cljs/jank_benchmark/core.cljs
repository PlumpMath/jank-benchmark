(ns jank-benchmark.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljsjs.recharts]
              [cljsjs.react-grid-layout]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]]
              [clojure.pprint :refer [pprint]])
    (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Views

(def data (reagent/atom {}))
(def poll-rate 1000) ; Milliseconds

; TODO: Allow interactive tweaking of this (put it in a ratom)
(def views [[:tests] [:fib-compile :fib-run-40]])

(defn extract
  "Extract from the given map each pair for the given keys.
   Example: (= (extract {:a 0 :b 1 :c 2 :d 3} [:a :d]) {:a 0 :d 3})"
  [m ks]
  (loop [ret {}
         ks' ks]
    (let [k (keyword (first ks'))]
      (if (empty? ks')
        ret
        (recur (assoc ret k (get m k))
               (rest ks'))))))

(defn home-page []
  (def layout [{:i "a" :x 0 :y 0 :w 1 :h 2 :static true},
               {:i "b" :x 1 :y 0 :w 3 :h 2 :minW 2, :maxW 4},
               {:i "c" :x 4 :y 0 :w 1 :h 2}])
  [:> js/ReactGridLayout {:className "layout"
                          :layout layout
                          :cols 12
                          :rowHeight 30
                          :width 1200}
   [:div {:key "a"} "a"]
   [:div {:key "b"} "b"]
   [:div {:key "c"} "c"]])
  (comment let [results (map :results @data)]
    [:div
     (for [v views]
       (let [points (map #(extract % v) results)]
         ; TODO: Provide CSS to fit these dynamically
         [:> js/Recharts.LineChart {:width 1000
                                    :height 700
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
                                  :activeDot {:r 8}}])]))])

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

(defn keywordify [m]
  (cond
    (map? m) (into {} (for [[k v] m] [(keyword k) (keywordify v)]))
    (coll? m) (vec (map keywordify m))
    :else m))

(defn get-data! []
  (go
    (let [reply-js (<! (http/get "/api/stats"))
          reply (-> (js/JSON.parse (:body reply-js))
                    js->clj
                    keywordify)]
      (when (not= reply @data)
        (reset! data reply)))))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (get-data!)
  (js/setInterval get-data! poll-rate)
  (mount-root))
