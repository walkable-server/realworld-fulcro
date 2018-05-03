(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]))

(defmutation login [{:keys [email password]}]
  (action [{:keys [state]}] state)
  (refresh [env] [:user/whoami])
  (remote [env] true))
