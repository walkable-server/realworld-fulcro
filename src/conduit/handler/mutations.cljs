(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]
            [fulcro.ui.form-state :as fs]))

(defmutation login [{:keys [email password]}]
  (action [{:keys [state]}] state)
  (refresh [env] [:user/whoami])
  (remote [env] true))

(defmutation submit-article [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* [:article/by-id id]))
  (remote [env] true))
