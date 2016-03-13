(ns spaghetti.state
  (:require [reagent.core :as r]))

(def initial-state {:nodes {}
                    :wires []})

(defonce !state (r/atom initial-state))

(defonce !actions (r/atom []))

(defmulti Action (fn [action state] (:type action)))
(defmulti Address (fn [address state action] (:address address)))

; TODO: log Addresses
; TODO: Allow recursive addresses
; TODO: simplify

(defn dispatch!
  ([address action]
   ;(let []
   ;  (dispatch! action))
   (swap! !actions conj action)
   (swap! !state (partial Address address @!state) #(Action action %))
   nil) ;; Always return nil because reagent events

  ([action]
   (swap! !actions conj action)
   (swap! !state #(Action action %))
   nil))

(defmethod Action :default [{:keys [type] :as action-data} state]
  (js/console.warn (str "Action " type " with data " (str action-data) " not defined."))
  state)

(defmethod Action :default [{:keys [type] :as action-data} state]
  (js/console.warn (str "Address " type " with data " (str action-data) " not defined."))
  state)

(defn rereduce
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce #(Action %2 %1) initial-state @!actions)))
