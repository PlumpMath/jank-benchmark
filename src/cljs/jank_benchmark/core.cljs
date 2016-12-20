(ns jank-benchmark.core
  (:require [jank-benchmark.poll :as poll]
            [jank-benchmark.grid :as grid]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]))

;; -------------------------
;; Views

(defn home-page []
  ; TODO: Pull these parts out into separate functions
  [:div
   [:ul
    (for [task @poll/queue]
      (let [hashes (map #(subs % 0 7) ((juxt :before :after) task))]
        [:li [:a {:href (:compare task)}
              (str (first hashes) " ... " (second hashes))]]))]
   grid/div])

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
