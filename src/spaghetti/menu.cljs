(ns spaghetti.menu
  (:require [spaghetti.state :refer [dispatch! Action]]
            [spaghetti.webaudio :as wa]))

(defmethod Action :add-node [{:keys [node-type x y] :as action-data} state]
  ;; TODO: get rid of side effect!
  (let [instance ((get-in wa/node-types [node-type :create-fn]))]
    (-> state
      (update :next-id inc)
      (update-in [:nodes] assoc (keyword "node" (:next-id state))
        {:node-type node-type :x x :y y :node-instance instance})
      (assoc-in [:creator-menu :visible?] false))))

(defmethod Action ::toggle-creator [{:keys [x y]} state]
  (update state :creator-menu
    assoc :x x :y y :visible? (not (-> state :creator-menu :visible?))))

(defn creator-menu [{:keys [visible? x y]}]
  [:ul.contextmenu {:style {:display (if visible? "block" "none")
                            :transform (str "translate(" x "px," y "px)")}}
   (for [n (keys wa/node-types)]
     ^{:key n} [:li {:onClick #(dispatch! {:type :add-node :node-type n :x (- x 100) :y y})}
                 (str "+ "(name n))])])
