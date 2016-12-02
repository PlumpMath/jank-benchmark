(ns jank-benchmark.css
  (:require [garden.core :refer [css]]))

; Colors from http://ethanschoonover.com/solarized
(def base00    "#657b83") (def base0     "#839496")
(def base01    "#586e75") (def base1     "#93a1a1")
(def base02    "#073642") (def base2     "#eee8d5")
(def base03    "#002b36") (def base3     "#fdf6e3")
(def yellow    "#b58900") (def orange    "#cb4b16")
(def red       "#dc322f") (def magenta   "#d33682")
(def violet    "#6c71c4") (def blue      "#268bd2")
(def cyan      "#2aa198") (def green     "#859900")

(def content-background-color base03)
(def content-color base0)
(def content-font-size "1.5em")
(def status-color green)
(def status-error-color red)
(def link-hover-color orange)

(defn main []
  (css
    {:pretty-print? false}
    [:html :body {:height "100%"}]
    [:body
     {:background-color content-background-color
      :color content-color
      :width "100%"
      :min-height "100%"
      :margin "0%"
      :padding "0%"}
     [:a:hover {:color link-hover-color}]]
    [:.status-error {:color (str status-error-color " !important")}]))
