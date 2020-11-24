(ns conduit.ui.account
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   ;; [conduit.ui.other :as other]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [conduit.handler.mutations :as mutations]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc Settings [this props]
  {:query [:user/image :user/name :user/bio :user/email]})

(defsc SettingsForm [this {:user/keys [id image name bio email password] :as props}]
  {:query       [:user/id :user/image :user/name :user/bio :user/email :user/password
                 fs/form-config-join]
   :ident       [:user/by-id :user/id]
   :form-fields #{:user/image :user/name :user/bio :user/email :user/password}}
  (dom/div :.settings-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-6.offset-md-3.col-xs-12
          (dom/h1 :.text-xs-center
            "Your Settings")
          (dom/form {:onSubmit #(do (.preventDefault %) (prim/transact! this `[(mutations/submit-settings ~(fs/dirty-fields props false))]))}
            (dom/fieldset
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "URL of profile picture",
                   :type        "text"
                   :value       image
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/image})])
                   :onChange    #(m/set-string! this :user/image :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name",
                   :type        "text"
                   :value       name
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/name})])
                   :onChange    #(m/set-string! this :user/name :event %)}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control.form-control-lg
                  {:rows        "8",
                   :placeholder "Short bio about you"
                   :value       (or bio "")
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/bio})])
                   :onChange    #(m/set-string! this :user/bio :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email",
                   :type        "text"
                   :value       email
                   :onBlur      #(prim/transact! this
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

(defsc SignUpError [this error]
  (dom/li
    (condp = error
      :error/email-taken
      "That email is already taken"
      "Unknown error")))

(def ui-sign-up-error (comp/factory SignUpError {:keyfn identity}))

(defsc SignUpForm [this {:user/keys [id name email] :keys [error] :as props}]
  {:query         [:user/id :user/name :user/email
                   fs/form-config-join :error]
   :initial-state #:user {:name "" :email ""}
   :ident         (fn [] [:app.top-level/sign-up-form :new-user])
   :route-segment ["sign-up"]
   :will-enter    (fn [app _route-params]
                    (dr/route-deferred [:app.top-level/sign-up-form :new-user]
                      #(comp/transact! app `[(load-sign-up-form {})
                                             (dr/target-ready {:target [:app.top-level/sign-up-form :new-user]})])))
   :form-fields   #{:user/name :user/email}}
  (let [{:user/keys [password] :as state} (comp/get-state this)]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if id
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                (str "You have successfully signed up as " name)))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Sign up")
              (dom/p  :.text-xs-center
                (dom/a {:href "#"
                        :onClick #(do (.preventDefault %)
                                      (dr/change-route-relative! this this [:.. "log-in"]))}
                  "Have an account?"))
              (when error
                (dom/ul :.error-messages
                  (ui-sign-up-error error)))

              (dom/form {:onSubmit #(do (.preventDefault %)
                                        (comp/transact! this `[(sign-up ~(merge (select-keys props [:user/name :user/email]) state))]))}

                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg.form-control-success.form-control-warning.form-control-danger
                    {:placeholder "Your Name"
                     :type        "text"
                     :value       (or name "")
                     :onBlur      #(comp/transact! this
                                     `[(fs/mark-complete! {:field :user/name})])
                     :onChange    #(m/set-string! this :user/name :event %)}))
                (dom/fieldset {:classes ["form-group"
                                         (when error
                                           "has-danger has-feedback")]}
                  (dom/input :.form-control.form-control-lg.form-control-success.form-control-warning.form-control-danger
                    {:placeholder "Email"
                     :type        "text"
                     :value       (or email "")
                     :onBlur      #(comp/transact! this
                                     `[(fs/mark-complete! {:field :user/email})])
                     :onChange    #(m/set-string! this :user/email :event %)}))

                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg.form-control-success.form-control-warning.form-control-danger
                    {:placeholder "Password"
                     :type        "password"
                     :value       (or password "")
                     :onChange    #(comp/set-state! this {:user/password (.. % -target -value)})}) )
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:type "submit" :value "submit"}
                  "Sign up")))))))))

(def ui-sign-up-form (comp/factory SignUpForm))

(defmutation load-sign-up-form [_]
  (action [{:keys [app state] :as env}]
    (swap! state
      #(fs/add-form-config* % SignUpForm (comp/get-ident SignUpForm {})))))

(defsc LogInForm
  [this {:user/keys [id name] :keys [error]}]
  {:query [:user/id :user/name :user/email :error]
   :initial-state #:user {:name "" :email ""}
   :ident (fn [] [:app.top-level/log-in-form :log-in])
   :route-segment ["log-in"]}
  (let [{:user/keys [email password] :as credentials} (comp/get-state this)]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if id
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " name))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Log in")
              (dom/p :.text-xs-center
                (dom/a {:href "#abc"
                        :onClick #(do (.preventDefault %)
                                      (dr/change-route-relative! this this [:.. "sign-up"]))}
                  "Don't have an account?"))
              (when error
                (dom/ul :.error-messages
                  (dom/li "Incorrect username or password!")))
              (dom/form {:onSubmit #(do (.preventDefault %)
                                        (comp/transact! this `[(log-in ~credentials)]))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Email"
                     :type        "text"
                     :value       (or email "")
                     :onChange    #(comp/update-state! this assoc :user/email (.. % -target -value))}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Password"
                     :type        "password"
                     :value       (or password "")
                     :onChange    #(comp/update-state! this assoc :user/password (.. % -target -value))}) )
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:type "submit" :value "submit"}
                  "Log in")))))))))

(def ui-log-in-form (comp/factory LogInForm))

(defmutation log-in [credentials]
  (remote [env] (m/returning env LogInForm)))

(defmutation log-out [_]
  ;; FIXME
  (remote [env] true))

(defmutation sign-up [new-user]
  (remote [env] (m/returning env SignUpForm)))

(defmutation use-settings-as-form [_]
  (action [{:keys [state] :as env}]
    (swap! state #(let [id (-> % :user/whoami second)]
                    (-> %
                      (assoc-in [:user/by-id id :user/password] "")
                      (fs/add-form-config* SettingsForm [:user/by-id id])
                      (assoc-in [:root/settings-form :user] [:user/by-id id]))))))
