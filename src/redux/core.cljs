(ns redux.core
  (:require [reagent.core :as r]
            [redux.utils :as utils]
            [alandipert.storage-atom :refer [local-storage load-local-storage]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(enable-console-print!)

;; Load storage should be an action
(def initial-state {:new-id 1})

(defonce !state (local-storage (r/atom initial-state) :todomvc))

(def initial-state {:count 0
                    :self nil
                    :checkout {}})

(def ^:dynamic *updating* false)

(defn validate-state
  [new-val]
  (when-not *updating*
    (js/console.log "Deprecated: updating state outside of an Action"))
  true)

(defonce !state (r/atom initial-state))

(defonce _validator
  (set-validator! !state validate-state))

(defonce !actions (r/atom []))

(defmulti Action
  "An Action is a pure function which manipulates the state.


  Create an Action like this:
  (defmethod Action :my-first-action [{:keys [type custom-data]} state]
    (assoc state :data custom-data))

  It takes the action data and the current state. Make sure you always return the state!
  "
  (fn [state action-data] (:type action-data)))

;; Handlers for side-effects

(defprotocol IHandler
  (get-result [this]))

(extend-protocol IHandler
  nil
  (get-result [this] {})
  object
  (get-result [this] {:state this}))

(defrecord FX [fx state]
  IHandler
  (get-result [this] {:state state :fx fx}))

(defn effects
  ;; feedback confusion because inconsisten with Actions
  ([fx] (effects fx nil))
  ([fx state]
   (assert (sequential? fx) "Effects must be sequential")
   (FX. fx state)))

(defmulti Effect (fn [_ effect-data] (:type effect-data)))

(defn dispatch!
  "Add Action to the history for re-reduces and directly applies Action to state.

  e.g. (dispatch! {:type :cool-action :custom-cool-data \"Well, hello!\"})"
  [action-data]
  (when js/goog.DEBUG
    (swap! !actions conj action-data)
    (log/info (str "[" (:type action-data) "]" " Action dispatched! Data: " (pretty/redact (dissoc action-data :type)))))

  (binding [*updating* true]
    (let [prev-state @!state
          {:keys [state fx] :as result} (get-result (Action prev-state action-data))]
      ;; do not update state if fn returned nil
      (when state
        (reset! !state state))
      (doseq [effect-data fx]
        (Effect (or state prev-state) effect-data))))
  nil) ;; Always return nil because reagent events

;; This is the catch all Action, gives an informative error when you didnt define an Action yet.
;; You can also use this as copy paste for future Actions ;)
(defmethod Action :default [state {:keys [type] :as action-data}]
  (js/console.warn (str "Action " type " with data " (str action-data) " not defined."))
  state)

(defmethod Effect :default [state {:keys [type] :as effect-data}]
  (js/console.warn (str "Effect " type " with data " (str effect-data) " not defined.")))

(defn rereduce!
  "Reduce past actions over state to get a fresh copy. As all actions
  are stored as data, changes in actions will be projected on state."
  []
  (reset! !state (reduce (fn [state action]
                           (or (get-result (Action state action)) state))
                         initial-state
                         @!actions)))

(defn undo! []
  (swap! !actions drop-last)
  (rereduce!))

(defn replay-action [actions]
  ;; TODO: make screenshot!
  (when (seq actions)
    ;; this doesnt do side-effects yet
    (swap! !state Action (first actions))
    (r/after-render #(replay-action (rest actions)))))

(defn replay! [actions]
  (reset! !state initial-state)
  (r/after-render #(replay-action actions)))

(defn prn-state []
  (prn @!state))

(defmethod Action ::reset
  [state _]
  (merge state initial-state))


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

(defmethod Action :LoadLocalStorage [_ _]
  (load-local-storage :todomvc))

(defmethod Action :AddTodo [{:keys [val]} state]
  (-> state
      (update :todos #(vec (conj % {:title val :id (:new-id state)})))
      (update :new-id inc)))

(defmethod Action :EditingTodo [{:keys [todo]} state]
  (update state :todos (fn [t] (map #(assoc % :editing? (= todo %))
                                    t))))

(defmethod Action :EditTodo [{:keys [todo val]} state]
  (update state :todos (fn [t] (map #(if (= todo %)
                                         (assoc % :title val :editing? false)
                                         %)
                                 t))))

(defmethod Action :DelTodo [{:keys [todo]} state]
  (update state :todos (fn [t] (filter (partial not= todo) t))))

(defmethod Action :ToggleTodo [{:keys [todo checked]} state]
  (update state :todos (fn [t] (map #(if (= todo %) (assoc % :completed? checked) %) t))))

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
    [:input.new-todo {:autoFocus "autofocus" :id "todo-input"
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


(defn todo-ui [{:keys [id] :as todo}]
  ^{:key id}
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
                    :on-change #(dispatch! {:type :ToggleTodo :todo todo
                                            :checked (.-checked (.-target %))})}]
    [:label {:on-double-click #(dispatch! {:type :EditingTodo :todo todo})}
     (:title todo)]
    [:button.destroy {:on-click #(dispatch! {:type :DelTodo :todo todo})}]]

   [:form {:on-submit (fn [e] (let [value (-> e .-target (.querySelector ".edit") .-value)]
                                (dispatch! {:type :EditTodo :todo todo :val value}))
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
      (into
        [:ul.todo-list]
        (map todo-ui (:todos state)))
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
  (prn "reload!")
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
