(ns redux.core
  (:require [reagent.core :as r]
            [redux.utils :as utils]
            [alandipert.storage-atom :refer [local-storage load-local-storage]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(enable-console-print!)

(def initial-state (or (load-local-storage :todomvc) {}))

(defonce !state (local-storage (r/atom initial-state) :todomvc))

(defonce !actions (r/atom []))

(defn dispatch! [action]
  (swap! !actions conj action))

(defmulti Action (fn [action state] (:type action)))

(add-watch !actions :reduce
  (fn [_ _ _ new-state]
    (let [new-action (last new-state)]
      (swap! !state #(Action new-action %)))))

(defn rereduce!
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce #(Action %2 %1) initial-state @!actions)))



;; HELPERS --------------------------------------------------------------------

(defn active-todos [todos]
  (filterv #(not (:completed? %)) todos))

(defn completed-todos [todos]
  (filterv :completed? todos))

(defn count-todos [todos]
  (-> (active-todos todos)
    (count)))



;; ACTIONS --------------------------------------------------------------------

(defmethod Action :default [{:keys [type] :as action-data} state]
  (js/console.warn (str "Action " type " with data " (str action-data) " not defined."))
  state)

(defmethod Action :AddTodo [{:keys [val]} state]
  (update state :todos #(conj % {:title val})))

(defmethod Action :EditingTodo [{:keys [index]} state]
  (update-in state [:todos index :editing?] not))

(defmethod Action :EditTodo [{:keys [index val]} state]
  (-> state (assoc-in [:todos index :title] val)
            (assoc-in [:todos index :editing?] false)))

(defmethod Action :DelTodo [{:keys [index]} state]
  (update state :todos #(utils/vec-remove % index)))

(defmethod Action :ToggleTodo [{:keys [index checked]} state]
  (assoc-in state [:todos index :completed?] checked))

(defmethod Action :SelectFilter [{:keys [filter]} state]
  (assoc state :filter filter))

(defmethod Action :ClearCompleted [_ state]
  (update state :todos active-todos))

(defmethod Action :ToggleAll [{:keys [checked]} state]
  (update state :todos #(map (fn [t] (assoc t :completed? checked)) %)))



;; VIEW -----------------------------------------------------------------------

(defn header-ui []
  [:header.header
   [:h1 "todos"]
   [:form {:on-submit (fn [e]
                        (let [value (.. (utils/get-elem "todo-input") -value)]
                         (when (not= value "")
                           (dispatch! {:type :AddTodo :val value})
                           (set! (.. (utils/get-elem "todo-input") -value) "")))
                        (.preventDefault e))}
    [:input.new-todo {:autofocus "autofocus" :id "todo-input"
                      :placeholder "What needs to be done?"}]]])


(defn filter-ui [f filter]
  ^{:key f}
  [:li [:a {:class (when (= f filter) "selected")
            :on-click #(dispatch! {:type :SelectFilter :filter f})
            :href (str "#/" (when-not (= f :All) (name f)))}
         (name f)]])


(defn footer-ui [state]
  [:footer.footer
   [:span.todo-count [:strong (count-todos (:todos state)) " item left"]]
   [:ul.filters
    (map #(filter-ui % (:filter state)) [:All :Active :Completed])]
   [:button.clear-completed
    {:on-click #(dispatch! {:type :ClearCompleted})} "Clear completed"]])


(defn todo-ui [i todo]
  ^{:key (:title todo)}
  [:li {:class (str (when (:completed? todo) "completed")
                 (when (:editing? todo) "editing"))
        :style (when (or (and
                           (not (:completed? todo))
                           (= (:filter @!state) :Completed))
                         (and (:completed? todo)
                           (= (:filter @!state) :Active)))
                 {:display "none"})}
   [:div.view
    [:input.toggle {:type "checkbox"
                    :checked (:completed? todo)
                    :on-change #(dispatch! {:type :ToggleTodo :index i
                                            :checked (.-checked (.-target %))})}]
    [:label {:on-double-click #(dispatch! {:type :EditingTodo :index i})}
     (:title todo)]
    [:button.destroy {:on-click #(dispatch! {:type :DelTodo :index i})}]]

   [:form {:on-submit (fn [e] (let [value (-> e .-target (.querySelector ".edit") .-value)]
                                (dispatch! {:type :EditTodo :index i :val value}))
                        (.preventDefault e))}
    [:input.edit {:default-value (:title todo)}]]])


(defn todo-app-ui [state]
  [:section.todoapp
   [header-ui]
   (when (:todos state)
     [:section.main
      [:input.toggle-all {:type "checkbox"
                          :on-click #(dispatch! {:type :ToggleAll
                                                 :checked (.-checked (.-target %))})}]
      [:label {:for "toggle-all"} "Mark all as complete"]
      [:ul.todo-list
       (doall (map-indexed todo-ui (:todos state)))]
      [footer-ui state]])])

(defn state-hud [state]
  (let [!display? (r/atom true)
        !pos (r/atom [20 20])]
    (fn [state]
      [:div.state-hud {:class (when-not @!display? "hide")
                       :on-double-click #(swap! !display? not)}
       [:p (str state)]
       [:p (str (take-last 10 @!actions))]])))

(defn root-ui [!state]
  (let [state @!state]
    [:div
     [todo-app-ui state]
     [state-hud state]]))

(r/render [root-ui !state] (js/document.getElementById "app"))

(defn on-js-reload
  "Hook for figwheel reload"
  []
  (rereduce!))



;; ROUTING --------------------------------------------------------------------

(secretary/set-config! :prefix "#")

(defroute all-route "/" []
  (dispatch! {:type :SelectFilter :filter :All}))

(defroute active-route "/Active" []
  (dispatch! {:type :SelectFilter :filter :Active}))

(defroute completed-route "/Completed" []
  (dispatch! {:type :SelectFilter :filter :Completed}))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
