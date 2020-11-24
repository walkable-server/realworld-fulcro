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
                      #(comp/transact! app `[(load-sign-up-form {})])))
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
                ;; FIXME:
                (dom/a {:onClick #(dr/change-route-relative! this this [:.. "log-in"])}
                  "Have an account?"))
              (when error
                (dom/ul :.error-messages
                  (ui-sign-up-error error)))

              (dom/form {:onSubmit #(do (.preventDefault %)
                                        (comp/transact! this `[(sign-up ~(merge (select-keys props [:user/name :user/email]) state))
                                                               #_(finish-sign-up {})]))}

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
  (action [{:keys [state] :as env}]
    (swap! state
           #(fs/add-form-config* % SignUpForm (comp/get-ident SignUpForm {})))))

(defsc LogInForm [this {:submission/keys [status result]}]
  (let [{:user/keys [email password] :as credentials} (prim/get-state this)

        whoami     (prim/shared this :user/whoami)
        logged-in? (number? (:user/id whoami))]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if logged-in?
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " (:user/name whoami)))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Log in")
              (dom/p :.text-xs-center
                (dom/a {:href (routes/to-path {:handler :screen/sign-up})}
                  "Don't have an account?"))
              (when (= status :failed)
                (dom/ul :.error-messages
                  (dom/li "Incorrect username or password!")))
              (dom/form {:onSubmit #(do (.preventDefault %)
                                        (prim/ptransact! this `[(log-in ~credentials)
                                                                (finish-log-in {})]))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Email"
                     :type        "text"
                     :value       (or email "")
                     :onChange    #(prim/update-state! this assoc :user/email (.. % -target -value))}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Password"
                     :type        "password"
                     :value       (or password "")
                     :onChange    #(prim/update-state! this assoc :user/password (.. % -target -value))}) )
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:type "submit" :value "submit"}
                  "Log in")))))))))

(def ui-log-in-form (prim/factory LogInForm))

(defsc SignUpScreen [this {user :new-user}]
  {:initial-state (fn [params] {:screen    :screen/sign-up
                                :screen-id :top
                                :new-user  #:user {:name "" :email ""}})
   :ident         (fn [] [:screen/sign-up :top])
   :query         [:screen :screen-id
                   {:new-user (prim/get-query SignUpForm)}]}
  (ui-sign-up-form user))

(defsc LogInScreen [this {log-in-status [:submission/by-id :app/log-in]}]
  {:initial-state (fn [params] {:screen    :screen/log-in
                                :screen-id :top})
   :ident         (fn [] [:screen/log-in :top])
   :query         [:screen :screen-id
                   {[:submission/by-id :app/log-in] (prim/get-query LogInSubmission)}]}
  (ui-log-in-form log-in-status))

(defsc SettingScreen [this {user [:root/settings-form :user]}]
  {:initial-state (fn [params] {:screen             :screen/settings
                                :screen-id          :top})
   :query         [:screen :screen-id
                   {[:root/settings-form :user] (prim/get-query SettingsForm)}]}
  (ui-settings-form user))

(defmutation log-in [credentials]
  (remote [{:keys [ast state]}]
    (m/returning ast state LogInSubmission)))

(defn finish-log-in-or-sign-up
  [submission-key {:keys [state reconciler]}]
  (when (= :ok (get-in @state [:submission/by-id submission-key
                               :submission/status]))
    (when-let [current-user
               (get-in @state [:submission/by-id submission-key
                               :submission/result])]
      (let [new-token (get-in @state (conj current-user :user/token))]
        (reset! other/token-store (str "Token " new-token)))
      (swap! state assoc :user/whoami current-user)
      (prim/transact! reconciler `[(mutations/rerender-root)]))))

(defmutation finish-log-in [_]
  (action [env]
    (finish-log-in-or-sign-up :app/log-in env)))

(defmutation finish-sign-up [_]
  (action [env]
    (finish-log-in-or-sign-up :app/sign-up env)))

(defmutation log-out [_]
  (action [{:keys [state] :as env}]
    (df/load-action env :user/whoami other/UserTinyPreview
      {:params        {:logout true}
       :post-mutation `mutations/rerender-root}))
  (remote [env]
    (df/remote-load env)))

(defmutation sign-up [new-user]
  (remote [{:keys [ast state]}]
    (m/returning ast state SignUpSubmission)))

(defmutation use-settings-as-form [_]
  (action [{:keys [state] :as env}]
    (swap! state #(let [id (-> % :user/whoami second)]
                    (-> %
                      (assoc-in [:user/by-id id :user/password] "")
                      (fs/add-form-config* SettingsForm [:user/by-id id])
                      (assoc-in [:root/settings-form :user] [:user/by-id id]))))))
