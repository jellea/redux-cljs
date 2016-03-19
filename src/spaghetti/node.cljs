(ns spaghetti.node
  (:require [reagent.core :as r]
            [spaghetti.state :refer [dispatch! Action]]))



;; Actions

(defmethod Action ::drag [{:keys [x y]} state]
  (assoc state :x x :y y :dragging? true))

(defmethod Action ::drag-end [{:keys [x y]} state]
  (assoc state :x x :y y :dragging? false))



;; Node

(defmulti node (fn [n _] (:node-type n)))

(defn standard-node-ui [{:keys [dragging? node-type x y]} address]
  [:div.node {:draggable true
              :style {:transform (str "translate(" x "px," y "px)")
                      ;:opacity (if dragging? 0 1)
                      }
              :on-mouse-down #(js/console.log (.-nativeEvent %))
              :on-drag #(dispatch! {:type ::drag
                                    :x (.-clientX %) :y (.-clientY %)
                                    :address address})
              :on-drag-end #(dispatch! {:type ::drag-end
                                        :x (.-clientX %) :y (.-clientY %)
                                        :address address})}
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

(defmethod Action ::update-node [{:keys [node-id nested-action]} state]
  (update-in state [:nodes node-id] #(Action nested-action %)))

(defn node-container [!state]
  [:div
   (for [[k n] (:nodes @!state)]
     ^{:key k} [node n {:type ::update-node :node-id k}])])
