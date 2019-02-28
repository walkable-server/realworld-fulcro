(ns conduit.ui.article
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other]
   [conduit.ui.comment :as comment]
   [conduit.ui.routes :as routes]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.dom :as dom]))

(defsc ArticleMeta [this {:article/keys [id created-at article liked-by-count liked-by-me
                                         author]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/created-at :article/liked-by-count :article/liked-by-me
           {:article/author (prim/get-query other/UserPreview)}]}
  (let [whoami                     (prim/shared this :user/whoami)
        {current-user-id :user/id} whoami]
    (dom/div :.article-meta
      (dom/a {:href (routes/profile-url author)}
        (dom/img {:src (:user/image author other/default-user-image)}))
      (dom/div :.info
        (dom/a :.author {:href (routes/profile-url author)}
          (:user/name author))
        (dom/span :.date
          (other/js-date->string created-at)))
      ;; don't show follow button to themselves
      (when (not= (:user/id author) current-user-id)
        (if (:user/followed-by-me author)
          (dom/button :.btn.btn-sm.btn-outline-primary
            {:onClick #(prim/transact! this `[(mutations/unfollow ~author)])}
            (dom/i :.ion-plus-round)
            "Unfollow " (:user/name author)
            (dom/span :.counter "(" (:user/followed-by-count author) ")"))
          (dom/button :.btn.btn-sm.btn-outline-secondary
            {:onClick #(if (= :guest current-user-id)
                         (js/alert "You must log in first")
                         (prim/transact! this `[(mutations/follow ~author)]))}
            (dom/i :.ion-plus-round)
            "Follow " (:user/name author)
            (dom/span :.counter "(" (:user/followed-by-count author) ")"))))

      (if liked-by-me
        (dom/button :.btn.btn-sm.btn-outline-primary
          {:onClick #(prim/transact! this `[(mutations/unlike {:article/id ~id})])}
          (dom/i :.ion-heart)
          "Unfavorite Post"
          (dom/span :.counter
            " (" liked-by-count ")"))
        (dom/button :.btn.btn-sm.btn-outline-secondary
          {:onClick #(if (= :guest current-user-id)
                       (js/alert "You must log in first.")
                       (prim/transact! this `[(mutations/like {:article/id ~id})]))}
          (dom/i :.ion-heart)
          "Favorite Post"
          (dom/span :.counter
            " (" liked-by-count ")"))))))

(def ui-article-meta (prim/factory ArticleMeta {:keyfn :article/id}))

(defsc Article [this {:article/keys [id author-id slug title description body image comments]
                      :keys         [ph/article]}]
  {:ident         [:article/by-id :article/id]
   :initial-state (fn [params] #:article{:id :none :comments (prim/get-initial-state comment/Comment #:comment{:id :none})})
   :query         [:article/id :article/author-id :article/slug :article/title :article/description
                   :article/body :article/image
                   {:article/comments (prim/get-query comment/Comment)}
                   {:ph/article (prim/get-query ArticleMeta)}]}
  (let [delete-comment #(prim/transact! this
                          `[(mutations/delete-comment {:article/id ~id :comment/id ~%})])

        editing-comment-id     (prim/get-state this :editing-comment-id)
        set-editing-comment-id #(prim/set-state! this {:editing-comment-id %})

        computed-map {:article-id             id
                      :delete-comment         delete-comment
                      :editing-comment-id     editing-comment-id
                      :set-editing-comment-id set-editing-comment-id}]
    (dom/div :.article-page
      (dom/div :.banner
        (dom/div :.container
          (dom/h1 title)
          (ui-article-meta article)))
      (dom/div :.container.page
        (dom/div :.row.article-content
          (dom/div :.col-md-12
            body))
        (dom/hr)
        (dom/div :.article-actions (ui-article-meta article))
        (dom/div :.row
          (dom/div :.col-xs-12.col-md-8.offset-md-2
            (comment/ui-comment-form (prim/computed #:comment{:id :none} computed-map))
            (mapv #(comment/ui-comment (prim/computed % computed-map))
              comments)))))))

(def ui-article (prim/factory Article {:keyfn :article/id}))

(defsc ArticleScreen [this {:keys [screen article-id article-to-view]}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/article
                                :article-id      :none
                                :article-to-view (prim/get-initial-state Article #:article{:id :none})})
   :query         (fn [] [:screen :article-id
                          {:article-to-view (prim/get-query Article)}])}
  (ui-article article-to-view))

(defmutation load-article-to-screen [{:keys [article-id]}]
  (action [{:keys [state] :as env}]
    (df/load-action env [:article/by-id article-id] Article)
    (swap! state
      #(update-in % [:screen/article article-id]
         (fn [x] (or x
                   {:screen          :screen/article
                    :article-id      article-id
                    :article-to-view [:article/by-id article-id]})))))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:screen :article-to-view]))
