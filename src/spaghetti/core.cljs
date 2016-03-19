(ns spaghetti.core
  (:require [reagent.core :as r]

            [spaghetti.state :as s :refer [Action]]
            [spaghetti.node :as node]
            [spaghetti.wire :as wire]
            [spaghetti.hud :as hud]))

(enable-console-print!)

;; Fallback Action, gives an informative warning in console when triggering undefined Action.

(defmethod Action :add-node [{:keys [node-type] :as action-data} state]
  (-> state
    (update :next-id inc)
    (update-in [:nodes] assoc (keyword "node" (:next-id state)) {:node-type node-type})))

(defn root []
  [:div
   [:button {:on-click #(s/dispatch! {:type :add-node :node-type :oscillator})}
    "osc"]
   #_[:button {:on-click #(s/dispatch! {:type :add-node :node-type :gain})}
    "vca"]

   [hud/state-hud s/!state s/!actions]

   [wire/wire-container s/!state]
   [node/node-container s/!state]])

(r/render [root] (js/document.getElementById "app"))

(defn on-js-reload
  "Hook for figwheel reload"
  []
  (s/rereduce!))