(ns jank-benchmark.poll
  (:require [jank-benchmark.util :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.core :as reagent :refer [atom]]
            [clojure.pprint :refer [pprint]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Data

(def data (reagent/atom {}))
(def queue (reagent/atom {}))
(def rate-ms 1000)

(defn- get-data! []
  (go
    (let [reply-js (<! (http/get "/api/stats"))
          reply (-> (js/JSON.parse (:body reply-js))
                    js->clj
                    util/keywordify)]
      ; TODO: helper for this
      (when (not= (:results reply) @data)
        (reset! data (:results reply)))
      (when (not= (:queue reply) @queue)
        (reset! queue (:queue reply))))))

(defn init! []
  (js/setInterval get-data! rate-ms)
  ; Rather than waiting for the first call, do it immediately
  (get-data!))

;; -------------------------
;; Views

(defn div []
  (when (not-empty @queue)
    [:div
     [:ul
      (for [task @queue]
        (let [hashes (map #(subs % 0 7) ((juxt :before :after) task))]
          [:li
           [:a {:href (:compare task)}
            (str (first hashes) " ... " (second hashes)
                 (when (:running? task)
                   " (running)"))]]))]]))
