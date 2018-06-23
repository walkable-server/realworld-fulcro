(ns conduit.handler.mutations
  (:require [fulcro.client.mutations :refer [defmutation]]
            [conduit.util :as util]
            [fulcro.client.primitives :as prim]
            [fulcro.ui.form-state :as fs]))

(defmutation submit-article [diff]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* (util/get-ident diff)))
  (remote [env] true))

(defmutation submit-comment [{:keys [article-id diff]}]
  (action [{:keys [state]}]
    (swap! state #(let [ident (util/get-ident diff)
                        id    (second ident)]
                    (-> %
                      (update-in ident merge
                        (if (number? id) #:comment{:updated-at (js/Date.)} #:comment{:id id :created-at (js/Date.)})
                        (util/get-item diff))
                      (update-in [:article/by-id article-id :article/comments]
                        (if (number? id)
                          (fn [comments _] comments)
                          (fnil conj []))
                        ident)))))
  (refresh [env] [:article/comments])
  (remote [env] true))

(defmutation submit-settings [diff]
  (action [{:keys [state]}]
    (let [ident (util/get-ident diff)]
      (swap! state #(-> %
                      (assoc-in (conj ident :user/password) "")
                      (fs/entity->pristine* ident)))))
  (remote [env] true))

(defn remove-ref-by-id
  [xs id-to-remove]
  (filterv (fn [[_ id]] (not= id id-to-remove)) xs))

(defmutation delete-article [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> % (update :article/by-id dissoc id)
                    (update :articles/all remove-ref-by-id id)
                    (update :articles/feed remove-ref-by-id id))))
  (remote [env] true)
  (refresh [env] [:articles/all :articles/feed]))

(defmutation delete-comment [{comment-id :comment/id article-id :article/id}]
  (action [{:keys [state]}]
    (swap! state #(-> % (update :comment/by-id dissoc comment-id)
                    (update-in [:article/by-id article-id :article/comments] remove-ref-by-id comment-id))))
  (remote [env] true)
  (refresh [env] [:article/comments]))

(defmutation follow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                    (assoc-in [:user/by-id id :user/followed-by-me] true)
                    (update-in [:user/by-id id :user/followed-by-count] (fnil inc 0)))))
  (remote [env] true))

(defmutation unfollow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                   (assoc-in [:user/by-id id :user/followed-by-me] false)
                   (update-in [:user/by-id id :user/followed-by-count] (fnil dec 1)))))
  (remote [env] true))

(defmutation like [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                    (assoc-in [:article/by-id id :article/liked-by-me] true)
                    (update-in [:article/by-id id :article/liked-by-count] (fnil inc 0)))))
  (remote [env] true))

(defmutation unlike [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                   (assoc-in [:article/by-id id :article/liked-by-me] false)
                   (update-in [:article/by-id id :article/liked-by-count] (fnil dec 1)))))
  (remote [env] true))

(defmutation add-tag [{:keys [article-id tag]}]
  (action [{:keys [state]}]
    (swap! state update-in [:article/by-id article-id :article/tags]
      (fnil conj [])
      {:tag/tag tag})))

(defmutation remove-tag [{:keys [article-id tag]}]
  (action [{:keys [state]}]
    (swap! state update-in [:article/by-id article-id :article/tags]
      #(filterv (fn [x] (not= (:tag/tag x) %2)) %1)
      tag)))

(defmutation rerender-root [_]
  (action [{:keys [reconciler] :as env}]
    (prim/force-root-render! reconciler)))
