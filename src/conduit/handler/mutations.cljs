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

(defn remove-ref-by-id
  [xs id-to-remove]
  (filterv (fn [[_ id]] (not= id id-to-remove)) xs))

(defmutation delete-article [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> % (update :article/by-id dissoc id)
                    (update :articles/all remove-ref-by-id id)
                    (update :articles/feed remove-ref-by-id id))))
  (remote [env] true))
