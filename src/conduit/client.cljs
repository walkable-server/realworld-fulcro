(ns conduit.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom :refer [div button h3 label a input]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.ui-state-machines :as uism]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [conduit.app :refer [APP routing-start! route-to!]]
   [conduit.session :as session :refer [CurrentUser ui-current-user]]
   [com.fulcrologic.fulcro.inspect.preload]
   [com.fulcrologic.fulcro.inspect.dom-picker-preload]))

(defsc LoginForm [this {:ui/keys [email password error? busy?] :as props}]
  {:query         [:ui/email :ui/password :ui/error? :ui/busy?]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {:ui/email    "foo@bar.com"
                   :ui/error?   false
                   :ui/busy?    false
                   :ui/password "letmein"}}
  (div :.ui.container.segment
    (dom/div :.ui.form {:classes [(when error? "error")]}
      (div :.field
        (label "Username")
        (input {:value    email
                :disabled busy?
                :onChange #(m/set-string! this :ui/email :event %)}))
      (div :.field
        (label "Password")
        (input {:type      "password"
                :value     password
                :disabled  busy?
                :onKeyDown (fn [evt]
                             (when (evt/enter-key? evt)
                               (comp/transact! this [(session/login {:user/email    email
                                                                     :user/password password})])))
                :onChange  #(m/set-string! this :ui/password :event %)}))
      (when error?
        (div :.ui.error.message
          (div :.content
            "Invalid Credentials")))
      (button :.ui.primary.button
        {:classes [(when busy? "loading")]
         :onClick #(comp/transact! this [(session/login {:user/email    email
                                                         :user/password password})])}
        (when busy? "loading... ") "Login"))))

(defsc SignUpForm [this {:ui/keys [email password error? busy?] :as props}]
  {:query         [:ui/email :ui/password :ui/error? :ui/busy?]
   :ident         (fn [] [:component/id :sign-up])
   :route-segment ["sign-up"]
   :initial-state {:ui/email    "foo@bar.com"
                   :ui/error?   false
                   :ui/busy?    false
                   :ui/password "letmein"}}
  (div :.ui.container.segment
    (dom/div :.ui.form {:classes [(when error? "error")]}
      (div :.field
        (label "Username")
        (input {:value    email
                :disabled busy?
                :onChange #(m/set-string! this :ui/email :event %)}))
      (div :.field
        (label "Password")
        (input {:type      "password"
                :value     password
                :disabled  busy?
                :onKeyDown (fn [evt]
                             (when (evt/enter-key? evt)
                               (comp/transact! this [(session/sign-up {:user/email    email
                                                                       :user/password password})])))
                :onChange  #(m/set-string! this :ui/password :event %)}))
      (when error?
        (div :.ui.error.message
          (div :.content
            "Email is taken")))
      (button :.ui.primary.button
        {:classes [(when busy? "loading")]
         :onClick #(comp/transact! this [(session/sign-up {:user/email    email
                                                           :user/password password})])}
        (when busy? "loading...") "Sign-Up"))))

(defsc Home [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Home Screen")))

(defsc Settings [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Settings Screen")))

(defrouter MainRouter [this props]
  {:router-targets [LoginForm SignUpForm Home Settings]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [_ {:root/keys    [ready? router]
                :session/keys [current-user]}]
  {:query         [:root/ready? {:root/router (comp/get-query MainRouter)}
                   {:session/current-user (comp/get-query CurrentUser)}]
   :initial-state {:root/router {}}}
  (let [logged-in? (:user/valid? current-user)]
    (div
      (div :.ui.top.fixed.menu
        (div :.item
          (div :.content "My Cool App"))
        (when logged-in?
          (comp/fragment
            (div :.item
              (div :.content (a {:href "/home"} "Home")))
            (div :.item
              (div :.content (a {:href "/settings"} "Settings")))))
        (div :.right.floated.item
          (ui-current-user current-user)))
      (when ready?
        (div :.ui.grid {:style {:marginTop "4em"}}
          (ui-main-router router))))))

(defmutation finish-login [_]
  (action [{:keys [app state]}]
    (let [logged-in? (get-in @state [:session/current-user :user/valid?])]
      (when-not logged-in?
        (route-to! "/login"))
      (swap! state assoc :root/ready? true))))

(defn refresh []
  (app/mount! APP Root "app"))

(defn ^:export start []
  (app/mount! APP Root "app")
  (dr/initialize! APP)
  (routing-start!)
  (uism/begin! APP session/session-machine ::session/sessions
    {:actor/user session/CurrentUser
     :actor/login-form LoginForm
     :actor/sign-up-form SignUpForm}
    {:desired-path (some-> js/window .-location .-pathname)})
  (df/load! APP :session/current-user CurrentUser {:post-mutation `finish-login}))

(start)

(comment
  (refresh))
