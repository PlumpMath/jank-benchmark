(ns jank-benchmark.handler
  (:require [jank-benchmark
             [middleware :refer [wrap-middleware]]
             [css :as css]]
            [ring.util.response :refer [response]]
            [compojure
             [core :refer [GET POST defroutes]]
             [route :refer [not-found resources]]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]
            [me.raynes.fs :as fs]
            [clojure.data.json :as json]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:style (css/main)]])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(def data-file "stored-data")

(defn read-data []
  (let [data (if (fs/exists? data-file)
               (read-string (slurp data-file))
               [])]
    data))

(def current-data (atom (read-data)))

(defn write-data [data]
  (spit data-file (pr-str data)))

(def lib-dir "lib/")
(def jank-dir (str lib-dir "jank/"))

(defn sh [& args]
  (let [result (apply clojure.java.shell/sh args)]
    (assert (zero? (:exit result))
            (str "error " (:exit result) ": " args))
    result))

(defn checkout [commit]
  ; TODO: spec/conform commit
  (assert (re-matches #"^[a-zA-Z0-9]{7,40}$" commit) "invalid commit format")
  (when (not (fs/exists? jank-dir))
    (fs/mkdir lib-dir)
    (println "Cloning jank...")
    (sh "git" "clone" "https://github.com/jeaye/jank.git"
                           :dir lib-dir))
  (sh "git" "fetch" "origin" :dir jank-dir)
  (sh "git" "checkout" commit :dir jank-dir))

(defn run [request]
  ; TODO: Only run if master branch updated
  ; TODO: Don't run multiple times for same commit
  (let [commit (:commit request)
        _ (checkout commit)
        sh-result (sh "lein" "with-profile" "benchmark" "trampoline" "run"
                      :dir jank-dir)
        data (read-string (:out sh-result))]
    (swap! current-data conj data)
    (swap! current-data (partial sort-by :commit-timestamp))
    (write-data @current-data)
    data))

(defroutes app-routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/api/stats" [] (response @current-data))
  (POST "/api/run" {body :body} (response (run (json/read-str (slurp body)
                                                              :key-fn keyword))))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'app-routes))
