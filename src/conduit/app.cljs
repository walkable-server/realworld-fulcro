(ns conduit.app
  (:require [com.fulcrologic.fulcro.networking.http-remote :as http]
            [com.fulcrologic.fulcro.application :as app]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [clojure.string :as str]
            [pushy.core :as pushy]))

(def secured-request-middleware
  (-> (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!")
    (http/wrap-csrf-token)
    (http/wrap-fulcro-request)))

(def app-remote
  (http/fulcro-http-remote {:url                "/api"
                            :request-middleware secured-request-middleware}))

(defonce APP (app/fulcro-app {:remotes {:remote app-remote}}))

(defonce history
  (pushy/pushy
   (fn [p]
     (let [route-segments (vec (rest (str/split p "/")))]
       (log/spy :info route-segments)
       (dr/change-route APP route-segments)))
   identity))

(defn routing-start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given route-string (e.g. \"/home\"."
  [route-string]
  (pushy/set-token! history route-string))

(defn init! []
  (routing-start!)
  (dr/initialize! APP))
