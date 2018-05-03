(ns conduit.intro
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [conduit.handler.mutations :as mutations]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.network :as net]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.cards :refer [defcard-fulcro]]
            [conduit.ui.components :as comp]
            [fulcro.client.dom :as dom]))

(defsc Me [this props]
  {:query [{:user/whoami (prim/get-query comp/Profile)}]}
  (let [user (get props :user/whoami)]
    (dom/div {}
      (dom/div {:onClick #(prim/transact! this `[(mutations/login {:email "foobar@yep.com" :password "foobar"})])} "Login")
      (dom/div {:onClick #(df/load this :user/whoami comp/Profile)} "Update me")
      (comp/ui-profile user))))

(def token-store (atom "No token"))

(defn wrap-remember-token [res]
  (when-let [new-token (-> (:body res) (get `mutations/login) :token)]
    (reset! token-store (str "Token " new-token)))
  res)

(defn wrap-with-token [req]
  (assoc-in req [:headers "Authorization"] @token-store))

(defcard-fulcro yolo
  Me
  {} ; initial state. Leave empty to use :initial-state from root component
  {:inspect-data true
   :fulcro       {:networking {:remote (net/fulcro-http-remote {:url "/api"
                                                                :response-middleware (net/wrap-fulcro-response wrap-remember-token)
                                                                :request-middleware  (net/wrap-fulcro-request wrap-with-token)
                                                                })}}})
(dc/start-devcard-ui!)
