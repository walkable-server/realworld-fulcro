(ns conduit.client
  (:require [fulcro.client :as fc]
            [conduit.ui.root :as root]
            [conduit.ui.other :refer [token-store]]
            [fulcro.client.network :as net]))

(defn wrap-with-token [req]
  (if-let [token @token-store]
    (assoc-in req [:headers "Authorization"] token)
    req))

(def remote
  (net/fulcro-http-remote
    {:url                 "/api"
     :request-middleware  (net/wrap-fulcro-request wrap-with-token)}))

(defonce app (atom (fc/new-fulcro-client
                     :reconciler-options {:shared-fn #(select-keys % [:user/whoami])}
                     :started-callback root/started-callback
                     :networking {:remote remote})))
