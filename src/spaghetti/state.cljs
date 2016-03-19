(ns spaghetti.state
  (:require [reagent.core :as r]))

(def initial-state {:nodes {}
                    :wires []
                    :next-id 1})

(defonce !state (r/atom initial-state))

(defonce !actions (r/atom []))

(defmulti Action (fn [action state] (:type action)))

(defn dispatch! [action]
  (swap! !actions conj action)

  ;; nest Actions by passing a map with the key :address
  (if (:address action)
    ;; TODO be able to recursively nest actions.
    (swap! !state #(Action (assoc (:address action) :nested-action action) %))
    (swap! !state #(Action action %))))  ;; Always return nil because reagent events

(defmethod Action :default [{:keys [type] :as action-data} state]
  (js/console.warn (str "Action " type " with data " (str action-data) " not defined."))
  state)

(defn rereduce!
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce #(Action %2 %1) initial-state @!actions)))
