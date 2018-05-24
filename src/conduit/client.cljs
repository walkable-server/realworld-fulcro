(ns conduit.client
  (:require [fulcro.client :as fc]
            [conduit.ui.root :as root]
            [fulcro.client.network :as net]))

(def token-store (atom "No token"))

(defn wrap-remember-token [res]
  (when-let [new-token (-> (:body res) (get :user/whoami) :app/token)]
    ;;(println (str "found token: " new-token))
    (reset! token-store (str "Token " new-token)))
  res)

(defn wrap-with-token [req]
  (assoc-in req [:headers "Authorization"] @token-store))

(def remote
  (net/fulcro-http-remote
    {:url                 "/api"
     :response-middleware (net/wrap-fulcro-response wrap-remember-token)
     :request-middleware  (net/wrap-fulcro-request wrap-with-token)}))

(defonce app (atom (fc/new-fulcro-client
                     :reconciler-options {:shared-fn #(select-keys % [:user/whoami])}
                     :started-callback root/started-callback
                     :networking {:remote remote})))
