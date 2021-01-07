(ns conduit.handler.mutations 
  (:require [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [conduit.util :as util]
            [conduit.app :as app]
            [com.rpl.specter :as s
             :refer [MAP-VALS ALL pred= NONE ;; pred filterer FIRST LAST
                     ]
             :refer-macros [setval]
             #_[defprotocolpath defnav extend-protocolpath
              nav declarepath providepath select select-one select-one!
              select-first transform 
              replace-in
              select-any selected-any? collected? traverse
              multi-transform path dynamicnav recursive-path
              defdynamicnav traverse-all satisfies-protpath? end-fn
              vtransform]]
            [conduit.ui.other :as other]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]))

(defmutation ensure-ident [{:keys [ident] base-state :state}]
  (action [{:keys [state]}]
    (swap! state update-in ident merge (or base-state {}))))

(defmutation submit-article [diff]
  (action [{:keys [state]}]
    (swap! state fs/entity->pristine* (util/get-ident diff)))
  (ok-action [{:keys [:tempid->realid]}]
    (when (seq tempid->realid)
      (let [id (first (vals tempid->realid))]
        (app/route-to! (str "/edit/" id)))))
  (remote [env] true))

(defmutation submit-comment [{:keys [article-id diff]}]
  (action [{:keys [state]}]
    (swap! state #(let [ident (util/get-ident diff)
                        id    (second ident)
                        current-user (get-in % [:session/session :current-user])]
                    (-> %
                      (update-in ident merge
                        (if (number? id)
                          #:comment{:updated-at (js/Date.)}
                          #:comment{:id id
                                    :author current-user
                                    :can-edit true
                                    :created-at (js/Date.)})
                        (util/get-item diff))
                      (update-in [:article/id article-id :article/comments]
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

(defn remove-article-from-all-pages
  [state id]
  (setval [:pagination/page MAP-VALS
           :pagination/items ALL (pred= [:article/id id])]
    NONE state))

(defmutation delete-article [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> % (update :article/id dissoc id)
                    (remove-article-from-all-pages id))))
  (remote [env] true)
  (refresh [env] [:articles/all :articles/feed]))

(defmutation delete-comment [{comment-id :comment/id article-id :article/id}]
  (action [{:keys [state]}]
    (swap! state #(-> % (update :comment/id dissoc comment-id)
                    (update-in [:article/id article-id :article/comments] remove-ref-by-id comment-id))))
  (remote [env] true)
  (refresh [env] [:article/comments]))

(defmutation follow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                    (assoc-in [:user/id id :user/followed-by-me] true)
                    (update-in [:user/id id :user/followed-by-count] (fnil inc 0)))))
  (remote [env] true))

(defmutation unfollow [{:user/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                   (assoc-in [:user/id id :user/followed-by-me] false)
                   (update-in [:user/id id :user/followed-by-count] (fnil dec 1)))))
  (remote [env] true))

(defmutation like [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                    (assoc-in [:article/id id :article/liked-by-me] true)
                    (update-in [:article/id id :article/liked-by-count] (fnil inc 0)))))
  (remote [env] true))

(defmutation unlike [{:article/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                   (assoc-in [:article/id id :article/liked-by-me] false)
                   (update-in [:article/id id :article/liked-by-count] (fnil dec 1)))))
  (remote [env] true))

(defmutation add-tag [{:keys [article-id tag]}]
  (action [{:keys [state]}]
    (swap! state update-in [:article/id article-id :article/tags]
      (fnil conj [])
      {:tag/tag tag})))

(defmutation remove-tag [{:keys [article-id tag]}]
  (action [{:keys [state]}]
    (swap! state update-in [:article/id article-id :article/tags]
      #(filterv (fn [x] (not= (:tag/tag x) %2)) %1)
      tag)))
