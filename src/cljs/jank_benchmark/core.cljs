(ns jank-benchmark.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljsjs.recharts]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]]
              [clojure.pprint :refer [pprint]])
    (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Views

(def data (reagent/atom {}))
(def poll-rate 1000) ; Milliseconds

(defn extract [m ks]
  (loop [ret {}
         ks' ks]
    (let [k (keyword (first ks'))]
      (if (empty? ks')
        ret
        (recur (assoc ret k (get m k))
               (rest ks'))))))

(defn home-page []
  (let [results (map :results @data)
        views (map :views @data)]
    [:div
     (when (not-empty views)
       (for [v (first views)]
         (let [points (map #(extract % v) results)] ; TODO: pull views out of results
           (pprint points)
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
                                    :activeDot {:r 8}}])])))]))

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
