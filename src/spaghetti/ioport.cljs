(ns spaghetti.ioport)

(defn start-wiring [{:keys [ab portid cursor]}]
  (swap! wiring assoc ab portid))

;(add-watch wiring :b
;  #(let [wires @wiring]
;    (if (and (not (nil? (:a wires)))
;          (not (nil? (:b wires))))
;      (do
;        (swap! app-state (fn [w] (update-in w [:wires] (fn [o] (conj o {:a (:a wires) :b (:b wires)})))))
;        (reset! wiring {:a nil :b nil})))))

(defmethod port {:dir :output :type :number} []
  [:div.io.output
   {;:onClick #(start-wiring {:ab :a :portid parentid})
    }
   "output"])

(defmethod port {:dir :input :type :choises} []
  [:div.io.choise.input {} (str (name (:n app)))
   [:span.value [:select {                                  ;:onChange #(change-port-choice (-> % .-target .-value) owner audio-node)
                          :value value}
                 (for [choice (:choices app)]
                   [:option {:value choice} choice])]]])

(defmethod port {:dir :input :type :number} []
  [:div.io.number.input (str (name (:n app)))
   [:span.value {;:onClick #(change-port-value (js/prompt) (:n app) owner audio-node)
                 } value]])

(defmethod port :default []
  [:div.io.input {:onClick #(start-wiring {:ab :b :portid parentid})} (name app)])

(defmulti port #(:type %))
