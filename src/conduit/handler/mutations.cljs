(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]
            [fulcro.ui.form-state :as fs]))

(defmutation submit-article [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* [:article/by-id id]))
  (remote [env] true))
