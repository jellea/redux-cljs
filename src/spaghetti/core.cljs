(ns spaghetti.core
  (:require [reagent.core :as r]

            [spaghetti.state :as s :refer [!state]]
            [spaghetti.node :as node]
            [spaghetti.wire :as wire]
            [spaghetti.hud :as hud]
            [spaghetti.menu :as menu]))

(enable-console-print!)

(defn root []
  [:div
   [wire/wire-container !state]
   [menu/creator-menu (:creator-menu @!state)]
   [node/node-container !state]
   [hud/state-hud s/!state s/!actions]])

(r/render [root] (js/document.getElementById "app"))

(defn on-js-reload
  "Hook for figwheel reload"
  []
  (s/rereduce!))