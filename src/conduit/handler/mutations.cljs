(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]
            [conduit.util :as util]
            [fulcro.ui.form-state :as fs]))

(defmutation submit-article [diff]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* (util/get-ident diff)))
  (remote [env] true))

(defmutation submit-comment [{:keys [article-id diff]}]
  (action [{:keys [state]}]
    (swap! state #(let [ident (util/get-ident diff)]
                    (-> %
                      (fs/entity->pristine* ident)
                      (update-in [:article/by-id article-id :article/comments]
                        (fnil conj []) ident)))))
  (refresh [env] [:article/comments])
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

;; todo: increase/decrease counts by 1
(defmutation follow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(assoc-in % [:user/by-id id :user/followed-by-me] true)))
  (remote [env] true))

(defmutation unfollow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(assoc-in % [:user/by-id id :user/followed-by-me] false)))
  (remote [env] true))

(defmutation like [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(assoc-in % [:article/by-id id :article/liked-by-me] true)))
  (remote [env] true))

(defmutation unlike [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(assoc-in % [:article/by-id id :article/liked-by-me] false)))
  (remote [env] true))
