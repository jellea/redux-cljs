(ns spaghetti.core
  (:require [reagent.core :as r]

            [spaghetti.state :as s :refer [Action]]
            [spaghetti.nodes :as nodes]
            [spaghetti.hud :as hud]))

(enable-console-print!)

;; Fallback Action, gives an informative warning in console when triggering undefined Action.

(defmethod Action :add-node [{:keys [node-type] :as action-data} state]
  (update-in state [:nodes] assoc (keyword (str "node" (rand-int 10000))) {:node-type node-type}))

(defn root []
  [:div
   [:button {:on-click #(s/dispatch! {:type :add-node :node-type :oscillator})} "osc"]
   [:button {:on-click #(s/dispatch! {:type :add-node :node-type :gain})} "vca"]

   [hud/state-hud s/!state s/!actions]

   [nodes/node-container s/!state]
   ])

(r/render [root] (js/document.getElementById "app"))

(defn on-js-reload
  "Hook for figwheel reload"
  []
  (s/rereduce))