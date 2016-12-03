(ns jank-benchmark.poll
  (:require [jank-benchmark.util :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def data (reagent/atom {}))
(def rate-ms 1000)

(defn get-data! []
  (go
    (let [reply-js (<! (http/get "/api/stats"))
          reply (-> (js/JSON.parse (:body reply-js))
                    js->clj
                    util/keywordify)]
      (when (not= reply @data)
        (reset! data reply)))))

(defn init! []
  (js/setInterval get-data! rate-ms))
