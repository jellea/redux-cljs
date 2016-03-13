(ns spaghetti.nodes
  (:require [reagent.core :as r]

            [spaghetti.state :as s :refer [dispatch! Action Address]]))

;; Actions

(defmethod Action :drag-start [_ state]
  (assoc-in state :dragging? true))

(defmethod Action :drag [{:keys [x y]} state]
  (assoc state :x x :y y))

(defmethod Action :drag-end [{:keys [x y]} state]
  (assoc state :x x :y y :dragging? false))


;; Standard Node

(defmulti node (fn [n _] (:node-type n)))

(defn standard-node-ui [{:keys [dragging? node-type x y key]} address]
  [:div.node {:draggable true
              :style {:transform (str "translate(" x "px," y "px)")
                      ;:opacity (if dragging? 0 1)
                      }
              :on-drag-start #(dispatch! address {:type :drag-start})
              :on-drag #(dispatch! address {:type :drag :x (.-clientX %) :y (.-clientY %)})
              :on-drag-end #(dispatch! address {:type :drag-end :x (.-clientX %) :y (.-clientY %)})}
   (str "node:" node-type)])

(defmethod node :gain [n]
  [:div.node "GAIN"])

(defmethod node :default [n address]
  (r/create-class
   {:component-did-mount
    #(println "component-did-mount")

    :component-will-mount
    #(prn "component-will-mount")
    :display-name (str "node-" (:node-type n) (:key n))
    :reagent-render standard-node-ui}))

;; Container

(defmethod Address :node [{:keys [id]} state action]
  (update-in state [:nodes id] action))

(defn node-container [!state]
  [:div
   (for [[k n] (:nodes @!state)]
     ^{:key k} [node n {:address :node :id k}])])
