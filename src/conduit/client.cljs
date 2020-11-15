(ns conduit.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.http-remote :as http]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.inspect.preload]
   [com.fulcrologic.fulcro.dom :as dom]))

(def secured-request-middleware
  (-> (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!")
    (http/wrap-csrf-token)
    (http/wrap-fulcro-request)))

(def app-remote
  (http/fulcro-http-remote {:url                "/api"
                            :request-middleware secured-request-middleware}))

(def app
  (app/fulcro-app
    {:remotes {:remote app-remote}}))

(defsc Person [this {:person/keys [id name age] :as props} {:keys [onDelete]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])}
  (dom/li
    (dom/h5 (str name " (age: " age ")") (dom/button {:onClick #(onDelete id)} "X"))))

(defsc Root [this props]
  (dom/div "hello inspect"))

(defn ^:export init
  []
  (app/mount! app Root "app")
  (df/load app [:person/id 1] Person)
  (js/console.log "Loaded"))

(init)
