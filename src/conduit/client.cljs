(ns conduit.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.inspect.preload]
   [conduit.app :refer [APP init!]]
   [conduit.ui.account :as account]
   [com.fulcrologic.fulcro.dom :as dom]))

(defrouter RootRouter [this props]
  {:router-targets [account/SignUpForm]})

(def ui-route-router (comp/factory RootRouter))

(defsc Root [this {:keys [root-router]}]
  {:initial-state (fn [_params] {:root-router (comp/get-initial-state RootRouter)})
   :query [{:root-router (comp/get-query RootRouter)}]}
  (ui-route-router root-router))

(defn refresh []
  (app/set-root! APP Root {:initialize-state? true})
  (dr/change-route! APP ["sign-up"])
  (app/mount! APP Root "app"))

(defn ^:export init
  []
  (refresh)
  (init!))

(init)
