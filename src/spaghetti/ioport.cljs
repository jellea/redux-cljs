(ns spaghetti.ioport
  (:require [spaghetti.state :refer [Action]]))

;(defn start-wiring [{:keys [ab portid cursor]} wiring]
;  (swap! wiring assoc ab portid))

;(add-watch wiring :b
;  #(let [wires @wiring]
;    (if (and (not (nil? (:a wires)))
;          (not (nil? (:b wires))))
;      (do
;        (swap! app-state (fn [w] (update-in w [:wires] (fn [o] (conj o {:a (:a wires) :b (:b wires)})))))
;        (reset! wiring {:a nil :b nil})))))

(defmulti port #(select-keys %1 [:dir :type]))

(defmethod port {:dir :output} [_ _]
  (prn "output")
  [:div.io.output
   {};:onClick #(start-wiring {:ab :a :portid parentid})

   "output"])
;
(defmethod port {:dir :input :type :choices} [{:keys [choices value n]} _]
  [:div.io.choise.input {} (str (name n))
   [:span.value [:select {                                  ;:onChange #(change-port-choice (-> % .-target .-value) owner audio-node)
                          :value value}
                 (for [choice choices]
                  ^{:key choice} [:option {:value choice} choice])]]])

;
;(defmethod port {:dir :input :type :number} [app value]
;  [:div.io.number.input (str (name (:n app)))
;   [:span.value {};:onClick #(change-port-value (js/prompt) (:n app) owner audio-node)
;                value]])

(defmethod port :default [app _]
  [:div.io.input {};:onClick #(start-wiring {:ab :b :portid parentid})}
   (name (:n app))])

;; PORTS CONTAINER

(defmethod Action ::update-port [{:keys [port-id nested-action]} state]
  (update-in state [:io port-id] #(Action nested-action %)))

(defn ports-container [ports node-address]
  [:div
   (for [p ports]
    ^{:key (:n p)} [port p {:type ::update-port :port-id p}])])


