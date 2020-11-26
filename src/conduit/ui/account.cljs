(ns conduit.ui.account
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.ui-state-machines :as uism]
   [com.fulcrologic.fulcro.mutations :as m]
   [conduit.app :refer [route-to!]]
   [conduit.session :as session]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc LoginForm [this {:ui/keys [email password current-user] :as props}]
  {:query         [:ui/email :ui/password [::uism/asm-id '_]
                   {:ui/current-user (comp/get-query session/CurrentUser)}]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state (fn [_] {:ui/email "" :ui/password ""
                           :ui/current-user (comp/get-initial-state session/CurrentUser)})}
  (let [current-state         (uism/get-active-state this ::session/sessions)
        busy?                 (= :state/checking-credentials current-state)
        bad-credentials?      (= :state/bad-credentials current-state)
        error?                (= :state/server-failed current-state)
        
        {:user/keys [name valid?]} current-user]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if valid?
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " (or name (:user/email current-user))))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Log in")
              (dom/p :.text-xs-center
                (dom/a {:href "/sign-up"}
                  "Don't have an account?"))
              (when bad-credentials?
                (dom/ul :.error-messages
                  (dom/li "Incorrect username or password!")))
              (when error?
                (dom/ul :.error-messages
                  (dom/li "Server is down")))
              (dom/form {:classes [(when bad-credentials? "error")]
                         :onSubmit
                         #(do (.preventDefault %)
                              (uism/trigger! this
                                ::session/sessions :event/login
                                {:user/email    email
                                 :user/password password}))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:disabled    busy?
                     :onChange    #(m/set-string! this :ui/email :event %)
                     :placeholder "Email"
                     :type        "text"
                     :value       (or email "")}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:type        "password"
                     :disabled    busy?
                     :onChange    #(m/set-string! this :ui/password :event %)
                     :placeholder "Password"

                     :value (or password "")}))
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:classes [(when busy? "disabled")]
                   :type    "submit" :value "submit"}
                  "Log in")))))))))

(defsc SignUpForm [this {:ui/keys [name email password current-user]}]
  {:query         [:ui/name :ui/email :ui/password [::uism/asm-id '_]
                   {:ui/current-user (comp/get-query session/CurrentUser)}]
   :ident         (fn [] [:component/id :sign-up])
   :route-segment ["sign-up"]
   :initial-state (fn [_] {:ui/name         ""
                           :ui/email        ""
                           :ui/password     ""
                           :ui/current-user (comp/get-initial-state session/CurrentUser)})}
  (let [current-state (uism/get-active-state this ::session/sessions)
        busy?         (= :state/checking-credentials current-state)
        email-taken?  (= :state/email-taken current-state)
        error?        (= :state/server-failed current-state)
        
        {:user/keys [valid?]} current-user]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if valid?
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " (or (:user/name current-user) (:user/email current-user))))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Sign up")
              (dom/p :.text-xs-center
                (dom/a {:href "/login"}
                  "Don't have an account?"))
              (when email-taken?
                (dom/ul :.error-messages
                  (dom/li "Email is taken")))
              (when error?
                (dom/ul :.error-messages
                  (dom/li "Server is down")))
              (dom/form {:classes [(when email-taken? "error")]
                         :onSubmit
                         #(do (.preventDefault %)
                              (uism/trigger! this
                                ::session/sessions :event/sign-up
                                {:user/name     name
                                 :user/email    email
                                 :user/password password}))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:disabled    busy?
                     :onChange    #(m/set-string! this :ui/name :event %)
                     :placeholder "Name"
                     :type        "text"
                     :value       (or name "")}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:disabled    busy?
                     :onChange    #(m/set-string! this :ui/email :event %)
                     :placeholder "Email"
                     :type        "text"
                     :value       (or email "")}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:type        "password"
                     :disabled    busy?
                     :onChange    #(m/set-string! this :ui/password :event %)
                     :placeholder "Password"

                     :value (or password "")}))
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:classes [(when busy? "disabled")]
                   :type    "submit" :value "submit"}
                  "Sign up")))))))))

#_
(defsc Settings [this props]
  {:query [:user/image :user/name :user/bio :user/email]})

#_
(defsc SettingsForm
  [this {:user/keys [id image name bio email password] :as props}]
  {:query [:user/id :user/image :user/name :user/bio :user/email :user/password
           fs/form-config-join]
   :ident [:user/by-id :user/id]
   :form-fields #{:user/image :user/name :user/bio :user/email :user/password}}
  (dom/div :.settings-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-6.offset-md-3.col-xs-12
          (dom/h1 :.text-xs-center
            "Your Settings")
          (dom/form {:onSubmit #(do (.preventDefault %) (comp/transact! this `[(mutations/submit-settings ~(fs/dirty-fields props false))]))}
            (dom/fieldset
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "URL of profile picture",
                   :type        "text"
                   :value       image
                   :onBlur      #(comp/transact! this
                                   `[(fs/mark-complete! {:field :user/image})])
                   :onChange    #(m/set-string! this :user/image :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name",
                   :type        "text"
                   :value       name
                   :onBlur      #(comp/transact! this
                                   `[(fs/mark-complete! {:field :user/name})])
                   :onChange    #(m/set-string! this :user/name :event %)}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control.form-control-lg
                  {:rows        "8",
                   :placeholder "Short bio about you"
                   :value       (or bio "")
                   :onBlur      #(comp/transact! this
                                   `[(fs/mark-complete! {:field :user/bio})])
                   :onChange    #(m/set-string! this :user/bio :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email",
                   :type        "text"
                   :value       email
                   :onBlur      #(comp/transact! this
                                   `[(fs/mark-complete! {:field :user/email})])
                   :onChange    #(m/set-string! this :user/email :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password",
                   :type        "password"
                   :value       (or password "")
                   :onBlur      #(comp/transact! this
                                   `[(fs/mark-complete! {:field :user/password})])
                   :onChange    #(m/set-string! this :user/password :event %)}))
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:type "submit" :value "submit"}
                "Update Settings"))))))))

#_
(def ui-settings-form (comp/factory SettingsForm))

#_
(defmutation use-settings-as-form [_]
  (action [{:keys [state] :as env}]
    (swap! state #(let [id (-> % :user/whoami second)]
                    (-> %
                      (assoc-in [:user/by-id id :user/password] "")
                      (fs/add-form-config* SettingsForm [:user/by-id id])
                      (assoc-in [:root/settings-form :user] [:user/by-id id]))))))
