(ns conduit.middleware.fulcro
  (:require [com.fulcrologic.fulcro.server.api-middleware :as server]
            [integrant.core :as ig]))

(defmethod ig/init-key ::wrap-transit
  [_ options]
  (fn [handler]
    (-> handler
      (server/wrap-transit-params)
      (server/wrap-transit-response))))
