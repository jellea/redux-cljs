(ns spaghetti.hud
  (:require [reagent.core :as r]))

(defn state-hud [!state !actions]
  (let [!display? (r/atom true)]
    (fn [!state !actions]
      [:div.state-hud {:class (when-not @!display? "hide")
                       :on-double-click #(swap! !display? not)}
       [:p (str @!state)]
       [:p (str (take-last 10 @!actions))]])))
