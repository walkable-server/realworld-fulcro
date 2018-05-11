(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]
            [conduit.util :as util]
            [fulcro.ui.form-state :as fs]))

(defmutation submit-article [diff]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* (util/get-ident diff)))
  (remote [env] true))

(defmutation submit-settings [diff]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* (util/get-ident diff)))
  (remote [env] true))
