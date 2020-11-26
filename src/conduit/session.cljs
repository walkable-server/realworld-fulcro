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
           ;; for Root component
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
      (uism/activate :state/signing-up))))

(def main-events
  {:event/logout
   {::uism/handler
    (fn [env]
      (route-to! "/login")
      (-> env
        (uism/trigger-remote-mutation :actor/login `logout {})
        (uism/apply-action assoc-in [:session/session :current-user]
          {:user/id :nobody :user/valid? false})))}
   :event/sign-up
   {::uism/handler handle-sign-up}
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
           (-> (uism/activate env :state/idle)
             (uism/apply-action assoc :root/ready? true))))}
      :event/error
      {::uism/handler
       (fn [env]
         (-> (uism/activate env :state/server-failed)
           (uism/apply-action assoc :root/ready? true)))}}}

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

(defsc CurrentUser [this {:keys [:user/email :user/valid?]}]
  {:query         [:user/id :user/name :user/email :user/valid?]
   :initial-state {:user/id :nobody :user/valid? false}
   :ident         (fn [] [:session/session :current-user])}
  (dom/div :.item
    (when valid?
      (div :.content
        email
        ent/nbsp
        (a {:onClick
            (fn [] (uism/trigger! this ::sessions :event/logout))}
          "Logout")))))

(def ui-current-user (comp/factory CurrentUser))
