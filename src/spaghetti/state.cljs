(ns spaghetti.state
  (:require [reagent.core :as r]
            [spaghetti.webaudio :as wa]))

(def initial-state {:nodes {:out {:x (- (.-innerWidth js/window) 380)
                                  :y (- (.-innerHeight js/window) 220)
                                  :node-type :AudioDestinationNode
                                  :node-instance (.createGain wa/ctx)}}
                    :wires {}
                    :creator-menu {:visible true}
                    :next-id 2})

(defonce !state (r/atom initial-state))

(defonce !actions (r/atom []))

(defmulti Action (fn [action state] (:type action)))

(defn traverse-actions [state action]
  (if-not (:address action)
    (Action action state)
    (traverse-actions state (assoc (:address action) :nested-action action))))

(defn dispatch! [action]
  (swap! !actions conj action)
  (swap! !state traverse-actions action)
  nil)  ;; Always return nil because reagent events

;; This is the catch all Action, gives an informative error when you didnt define an Action yet.
;; You can also use this as copy paste for future Actions ;)
(defmethod Action :default [{:keys [type] :as action-data} state]
  (js/console.warn (str "Action " type " with data " (str action-data) " not defined."))
  state)

(defn rereduce!
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce #(Action %2 %1) initial-state @!actions)))




;(defmethod Action :default [{:keys [type] :as action-data} state]
;  (prn (str "Action " type " with data " (str action-data) " not defined."))
;  state)
;
;(defmethod Action ::update-node [{:keys [node-id nested-action]} state]
;  (update-in state [:nodes node-id] #(Action nested-action %)))
;
;(defmethod Action ::update-port [{:keys [port-id nested-action]} state]
;  (update-in state [:io port-id] #(Action nested-action %)))
;
;(defmethod Action ::update-port-value [{:keys [value]} state]
;  (assoc state :value value))
;
;(def state
;  {:nodes {1 {:io {:out {:value 15}}}}})
;
;(def example-action
;  {:type ::update-port-value
;   :value 16
;   :address
;   {:type ::update-port
;    :port-id :out
;    :address
;    {:type ::update-node
;     :node-id 1}}})
;
;(defn traverse-actions [state action]
;  (if-not (:address action)
;    (Action action state)
;    (recur state (assoc (:address action) :nested-action action))))
;
;(time (traverse-actions {} example-action))
;(traverse-actions {} {:type ::update-port-value :value 14})

