(ns conduit.middleware.fulcro
  (:require [com.fulcrologic.fulcro.server.api-middleware :as server]
            [integrant.core :as ig]))

(defmethod ig/init-key ::wrap-transit
  [_ {:keys [session-auth]}]
  (fn [handler]
    (-> handler
      (server/wrap-transit-params)
      session-auth
      (server/wrap-transit-response))))
