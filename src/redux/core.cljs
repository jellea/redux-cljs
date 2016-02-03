(ns redux.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(def initial-state {:counter 0
                    :list []})

(defonce !state (r/atom initial-state))

(defonce !actions (r/atom []))

(defn dispatch! [action]
  (swap! !actions conj action))

(defprotocol Action
  (reducer [action state]))

(add-watch !actions :reduce
  (fn [_ _ _ new-state]
    (let [new-action (last new-state)]
      (swap! !state #(reducer new-action %)))))

(defn rereduce []
  (reset! !state (reduce #(reducer %2 %1) initial-state @!actions)))

;; Making an Action

(defrecord Change [])
(defrecord Increment [num])

(extend-protocol Action
  Change
  (reducer [_ state]
    (update state :list #(conj % "more")))

  Increment
  (reducer [{:keys [num]} state]
    (update state :counter #(+ % num))))

(defn root []
  [:div
   [:pre "state: " (str @!state)]
   [:pre "actions: " (str @!actions)]
   [:button {:on-click #(dispatch! (->Change))} "more"]
   [:button {:on-click #(dispatch! (->Increment 10))} "+10"]])

(r/render [root] (js/document.getElementById "app"))


(defn on-js-reload []
  (rereduce)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
