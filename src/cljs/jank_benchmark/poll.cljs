(ns jank-benchmark.poll
    (:require [reagent.core :as reagent :refer [atom]]))

(def data (reagent/atom {}))
(def rate-ms 1000)

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
  (js/setInterval get-data! rate-ms))
