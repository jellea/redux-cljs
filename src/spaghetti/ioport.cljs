(ns spaghetti.ioport
  (:require [spaghetti.state :refer [dispatch! Action]]))

(defmethod Action ::create-wire [action state]
  (let [new-wiring (select-keys action [:to-node :to-port :from-node :from-port])]
    (if (and (:wiring state) (not= (:wiring state) new-wiring))
      (-> state
        (update :next-id inc)
        (update-in [:wires] assoc (keyword "wire" (:next-id state))
          (merge (:wiring state) new-wiring))
        (dissoc :wiring))

      (assoc state :wiring new-wiring))))

(defmethod Action ::update-port-value [{:keys [value]} state]
  (assoc state :value value))

;; PORT VARIANTS

(defmulti port #(select-keys %1 [:dir :type]))

(defmethod port {:dir :output} [{:keys [n]} _ address]
  [:div.io.output
   {:onClick #(dispatch! {:type ::create-wire :from-node address :from-port n})}
   (name n)])

(defmethod port {:dir :input :type :choices} [{:keys [choices value n]} port-address _]
  [:div.io.choise.input {} (str (name n))
   [:span.value
    [:select {:on-change #(dispatch! {:type ::update-port-value
                                      :value (-> % .-target .-value)
                                      :address port-address})
              :defaultValue value}
     (for [choice choices]
      ^{:key choice} [:option {:value choice} choice])]]])

(defmethod port {:dir :input :type :number} [{:keys [n value]} node-id]
  [:div.io.number.input
   (str (name n))
   [:span.value {:on-click #(prn "num" (js/prompt))}
                value]])

(defmethod port :default [{:keys [n]} _ node-id]
  [:div.io.input
   {:on-click #(dispatch! {:type ::create-wire :to-node node-id :to-port n})}
   (name n)])

;; PORTS CONTAINER

(defmethod Action ::update-port [{:keys [port-id nested-action]} state]
  (update-in state [port-id] #(Action nested-action %)))

(defn ports-container [ports node-address]
  [:div
   (for [p ports]
    ^{:key (:n p)} [port p {:type ::update-port :port-id (:n p) :address node-address}
                    node-address])])

