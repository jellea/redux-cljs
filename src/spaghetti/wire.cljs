(ns spaghetti.wire
  (:require [reagent.core :as r]
            [spaghetti.state :refer [dispatch! Action]]))

(defn calc-wire [{:keys [from-node to-node]} nodes]
  (let [node-a (get-in nodes [from-node])
        node-b (get-in nodes [to-node])
        x1     (+ (:x node-a) 300)
        y1     (+ (:y node-a) 165)
        x2     (+ (:x node-b) 100)
        y2     (+ (:y node-b) 65)]
    {:x1 x1 :y1 y1 :x2 x2 :y2 y2}))


(defn wire-ui [wire-data !state]
  (let [{:keys [x1 y1 x2 y2]} (calc-wire wire-data (:nodes @!state))]
    [:path {:d (str "M" x1 "," y1 " Q" (/ (+ x1 x2) 2) ","
                 (+ (max y1 y2) 40) " " x2 "," y2)
            :on-click #(js/console.log "click")
            :fill "none" :stroke "#444" :stroke-width 1}]))


(defn connect-nodes [wire-data !state]
  (.connect (get-in @!state [:nodes (:from-node wire-data) :node-instance])
    (get-in @!state [:nodes (:to-node wire-data) :node-instance])))

(defn disconnect-nodes [wire-data !state]
  (.disconnect (get-in @!state [:nodes (:from-node wire-data) :node-instance])
    (get-in @!state [:nodes (:to-node wire-data) :node-instance])))


(defn wire [wire-data !state]
  (r/create-class
    {:component-did-mount #(connect-nodes wire-data !state)
     :component-will-unmount #(disconnect-nodes wire-data !state)
     :reagent-render #(wire-ui wire-data !state)}))

(defmethod Action :remove-wire [{:keys [id]} state]
  state)


(defn wire-container [!state]
  [:svg.maincanvas
   {:on-click #(dispatch! {:type :spaghetti.menu/toggle-creator
                           :x (.-clientX %)
                           :y (.-clientY %)})}
   (for [[k w] (:wires @!state)]
     ^{:key k} [wire w !state])])
