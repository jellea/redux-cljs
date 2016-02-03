(ns redux.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(def initial-state {:counter 0
                    :list []})

(defonce !state (r/atom initial-state))

(defonce !actions (r/atom []))

(defn dispatch! [action]
  (swap! !actions conj action))

(defmulti Action (fn [action state] (:type action)))

(add-watch !actions :reduce
  (fn [_ _ _ new-state]
    (let [new-action (last new-state)]
      (swap! !state #(Action new-action %)))))

(defn rereduce
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce #(Action %2 %1) initial-state @!actions)))

;; Making an Action

(defmethod Action :Change [_ state]
  (update state :list #(conj % "more")))

(defmethod Action :Increment [{:keys [num]} state]
  (update state :counter #(- % num)))


(defn root []
  [:div
   [:pre "state: " (str @!state)]
   [:pre "actions: " (str @!actions)]
   [:button {:on-click #(dispatch! {:type :Change})} "more"]
   [:button {:on-click #(dispatch! {:type :Increment :num 10})} "+10"]])

(r/render [root] (js/document.getElementById "app"))

(defn on-js-reload
  "Hook for figwheel reload"
  []
  (rereduce))