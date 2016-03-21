(ns spaghetti.node
  (:require [reagent.core :as r]

            [spaghetti.state :refer [dispatch! Action]]
            [spaghetti.webaudio :as wa]
            [spaghetti.ioport :as io]))



;; Actions

(defmethod Action ::drag [{:keys [x y] :as c} state]
  (if-not (= {:x x :y y} {:x 0 :y 0}) ; Workaround for glitch on last dragevent
   (assoc state :x (- x 200) :y (- y 120))
   state))

;; NODE

(defmulti node (fn [n _] (:node-type n)))

(defn standard-node-ui [{:keys [node-type x y]} address]
  [:div.node {:draggable true
              :style {:transform (str "translate(" x "px," y "px)")}
              :on-drag #(dispatch! {:type ::drag :x (.. % -nativeEvent -clientX)
                                    :y (.. % -nativeEvent -clientY) :address address})}
   [:h2 (str "node:" node-type)]
   [io/ports-container (get-in wa/node-types [node-type :io]) address]])

(defmethod node :default [{:keys [node-type node-instance] :as n} address]
  (let [node-info (get-in wa/node-types [node-type])]
    (r/create-class
      {:component-did-mount
       #(if-let [mount-fn (:mount-fn node-info)]
         (mount-fn node-instance))

       :component-will-unmount
       #(if-let [unmount-fn (:mount-fn node-info)]
         (unmount-fn node-instance))

       :display-name (str "node-" (:node-type n) (:key n))
       :reagent-render standard-node-ui})))



;; NODES CONTAINER

(defmethod Action ::update-node [{:keys [node-id nested-action]} state]
  (update-in state [:nodes node-id] #(Action nested-action %)))

(defn node-container [!state]
  [:div
   (for [[k n] (:nodes @!state)]
     ^{:key k} [node n {:type ::update-node :node-id k}])])
