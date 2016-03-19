(ns spaghetti.wire
  (:require [reagent.core :as r]))

(defn wire-ui [{:keys [x1 x2 y1 y2]}]
  [:path {:d (str "M" x1 "," y1 " Q" (/ (+ x1 x2) 2) "," (+ (max y1 y2) 40) " " x2 "," y2)
          :fill "none" :stroke "#444" :stroke-width 1}])

(defn wire [wire-data]
  (r/create-class
    {:reagent-render wire-ui}))

(defn wire-canvas [wires]
  [:svg.maincanvas
   [wire {:x1 10 :y1 10 :x2 100 :y2 200}]])

(defn wire-container [!state]
  [wire-canvas])
