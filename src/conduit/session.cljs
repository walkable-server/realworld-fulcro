(ns conduit.session
  (:require
   [conduit.app :refer [route-to!]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
   [com.fulcrologic.fulcro.dom :as dom :refer [div a]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom.html-entities :as ent]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
   [taoensso.timbre :as log]))

(defn handle-login [{::uism/keys [event-data] :as env}]
  (let [user-class (uism/actor-class env :actor/user)]
    (-> env
      (uism/trigger-remote-mutation :actor/login-form `login
        (merge event-data
          {::m/returning      user-class
           ::targeting/target [:session/current-user]
           ::uism/ok-event    :event/ok
           ::uism/error-event :event/error}))
      (uism/activate :state/checking-credentials))))

(defn handle-sign-up [{::uism/keys [event-data] :as env}]
  (let [user-class (uism/actor-class env :actor/user)]
    (-> env
      (uism/trigger-remote-mutation :actor/sign-up-form `sign-up
        (merge event-data
          {::m/returning      user-class
           ::targeting/target [:session/current-user]
           ::uism/ok-event    :event/ok
           ::uism/error-event :event/error}))
      (uism/activate :state/checking-credentials))))

(def main-events
  {:event/logout
   {::uism/handler
    (fn [env]
      (route-to! "/login")
      (-> env
        (uism/trigger-remote-mutation :actor/login `logout {})
        (uism/apply-action assoc-in [::session :current-user]
          {:user/id :nobody :user/valid? false})))}
   :event/login
   {::uism/handler handle-login}})

(defstatemachine session-machine
  {::uism/actor-name
   #{:actor/user
     :actor/login-form
     :actor/sign-up-form}

   ::uism/aliases
   {:logged-in? [:actor/user :user/valid?]}

   ::uism/states
   {:initial
    {::uism/handler
     (fn [{::uism/keys [event-data] :as env}]
       (-> env
         ;; save desired path for later routing
         (uism/store :config event-data)
         (uism/load :session/current-user :actor/user
           {::uism/ok-event    :event/ok
            ::uism/error-event :event/error})
         (uism/activate :state/checking-existing-session)))}

    :state/checking-existing-session
    {::uism/events
     {:event/ok
      {::uism/handler
       (fn [env]
         (let [logged-in? (uism/alias-value env :logged-in?)]
           (when-not logged-in?
             (route-to! "/login"))
           (uism/activate env :state/idle)))}
      :event/error
      {::uism/handler
       (fn [env] (uism/activate env :state/server-failed))}}}

    :state/bad-credentials
    {::uism/events main-events}

    :state/email-taken
    {::uism/events main-events}

    :state/idle
    {::uism/events main-events}

    :state/checking-credentials
    {::uism/events
     {:event/ok
      {::uism/handler
       (fn [env]
         (let [logged-in? (uism/alias-value env :logged-in?)
               {:keys [desired-path]} (uism/retrieve env :config)]
           (when (and logged-in? desired-path)
             (route-to! desired-path))
           (-> env
             (uism/activate (if logged-in?
                              :state/idle
                              :state/bad-credentials)))))}
      :event/error
      {::uism/handler
       (fn [env] (uism/activate env :state/server-failed))}}}
    
    :state/signing-up
    {::uism/events
     {:event/ok
      {::uism/handler
       (fn [env]
         (let [logged-in? (uism/alias-value env :logged-in?)
               {:keys [desired-path]} (uism/retrieve env :config)]
           (when (and logged-in? desired-path)
             (route-to! desired-path))
           (-> env
             (uism/activate (if logged-in?
                              :state/idle
                              :state/email-taken)))))}
      :event/error
      {::uism/handler
       (fn [env] (uism/activate env :state/server-failed))}}}

    :state/server-failed
    {::uism/events main-events}}})

(defsc CurrentUser [this {:keys [:user/email :user/valid?] :as props}]
  {:query         [:user/id :user/email :user/valid?]
   :initial-state {:user/id :nobody :user/valid? false}
   :ident         (fn [] [::session :current-user])}
  (dom/div :.item
    (if valid?
      (div :.content
        email ent/nbsp (a {:onClick
                           (fn [] (uism/trigger! this ::sessions :event/logout))}
                         "Logout"))
      (div 
        (a {:onClick #(dr/change-route this ["login"])} "Login")
        (a {:onClick #(dr/change-route this ["sign-up"])} "Sign up")))))

(def ui-current-user (comp/factory CurrentUser))

(defn show-login-busy* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/busy?] tf))

(defn show-login-error* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/error?] tf))

(defmutation login [_]
  (action [{:keys [state]}]
    (swap! state show-login-busy* true))
  (error-action [{:keys [state]}]
    (log/error "Error action")
    (swap! state (fn [s]
                   (-> s
                     (show-login-busy* false)
                     (show-login-error* true)))))
  (ok-action [{:keys [state]}]
    (log/info "OK action")
    (let [logged-in? (get-in @state [:session/current-user :user/valid?])]
      (if logged-in?
        (do
          (swap! state (fn [s]
                         (-> s
                           (show-login-busy* false)
                           (show-login-error* false))))
          (route-to! "/home"))
        (swap! state (fn [s]
                       (-> s
                         (show-login-busy* false)
                         (show-login-error* true)))))))
  (refresh [_]
    [:ui/error? :ui/busy?])
  (remote [env]
    (-> env
      (m/returning CurrentUser)
      (m/with-target [:session/current-user]))))

(defn show-sign-up-busy* [state-map tf]
  (assoc-in state-map [:component/id :sign-up :ui/busy?] tf))

(defn show-sign-up-error* [state-map tf]
  (assoc-in state-map [:component/id :sign-up :ui/error?] tf))

(defmutation sign-up [_]
  (action [{:keys [state]}]
    (swap! state show-sign-up-busy* true))
  (error-action [{:keys [state]}]
    (log/error "Error action")
    (swap! state (fn [s]
                   (-> s
                     (show-sign-up-busy* false)
                     (show-sign-up-error* true)))))
  (ok-action [{:keys [state]}]
    (log/info "OK action")
    (let [logged-in? (get-in @state [:session/current-user :user/valid?])]
      (if logged-in?
        (do
          (swap! state (fn [s]
                         (-> s
                           (show-sign-up-busy* false)
                           (show-sign-up-error* false))))
          (route-to! "/home"))
        (swap! state (fn [s]
                       (-> s
                         (show-sign-up-busy* false)
                         (show-sign-up-error* true)))))))
  (refresh [_]
    [:ui/error? :ui/busy?])
  (remote [env]
    (-> env
      (m/returning CurrentUser)
      (m/with-target [:session/current-user]))))

(defmutation logout [_]
  (action [{:keys [state]}]
    (route-to! "/login")
    (swap! state assoc :session/current-user {:user/id :nobody :user/valid? false}))
  (remote [env] true))
