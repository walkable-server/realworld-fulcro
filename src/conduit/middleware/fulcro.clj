(ns conduit.middleware.fulcro
  (:import [com.cognitect.transit WriteHandler])
  (:require [com.fulcrologic.fulcro.server.api-middleware :as server]
            [integrant.core :as ig]))

(deftype DefaultHandler []
  WriteHandler
  (tag [this v] "unknown")
  (rep [this v] (pr-str v)))

(defmethod ig/init-key ::wrap-transit
  [_ {:keys [session-auth]}]
  (fn [handler]
    (-> handler
      (server/wrap-transit-params)
      session-auth
      (server/wrap-transit-response {:opts {:default-handler (DefaultHandler.)}}))))
