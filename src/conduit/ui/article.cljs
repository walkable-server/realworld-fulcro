(ns conduit.ui.article
  (:require
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other]
   [conduit.session :as session]
   [conduit.ui.comment :as comment]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc ArticleMeta
  [this {:article/keys [id created-at liked-by-count liked-by-me author]
         :ui/keys [current-user]}]
  {:ident :article/id
   :initial-state
   (fn [_] {:article/id :none
            :ui/current-user (comp/get-initial-state session/CurrentUser)})
   :query [:article/id :article/created-at :article/liked-by-count :article/liked-by-me
           {:ui/current-user (comp/get-query session/CurrentUser)}
           {:article/author (comp/get-query other/UserPreview)}]}
  (let [current-user-id (:user/id current-user)]
    (dom/div :.article-meta
      (dom/a {:href (str "/profile/" (:user/id author))}
        (dom/img {:src (:user/image author other/default-user-image)}))
      (dom/div :.info
        (dom/a :.author {:href (str "/profile/" (:user/id author))}
          (:user/name author))
        (dom/span :.date
          (other/js-date->string created-at)))
      ;; don't show follow button to themselves
      (when (not= (:user/id author) current-user-id)
        (if (:user/followed-by-me author)
          (dom/button :.btn.btn-sm.btn-outline-primary
            {:onClick #(comp/transact! this [(mutations/unfollow author)])}
            (dom/i :.ion-plus-round)
            "Unfollow " (:user/name author)
            (dom/span :.counter "(" (:user/followed-by-count author) ")"))
          (dom/button :.btn.btn-sm.btn-outline-secondary
            {:onClick #(if (= :guest current-user-id)
                         (js/alert "You must log in first")
                         (comp/transact! this [(mutations/follow author)]))}
            (dom/i :.ion-plus-round)
            "Follow " (:user/name author)
            (dom/span :.counter "(" (:user/followed-by-count author) ")"))))

      (if liked-by-me
        (dom/button :.btn.btn-sm.btn-outline-primary
          {:onClick #(comp/transact! this [(mutations/unlike {:article/id id})])}
          (dom/i :.ion-heart)
          "Unfavorite Post"
          (dom/span :.counter
            " (" liked-by-count ")"))
        (dom/button :.btn.btn-sm.btn-outline-secondary
          {:onClick #(if (= :guest current-user-id)
                       (js/alert "You must log in first.")
                       (comp/transact! this [(mutations/like {:article/id id})]))}
          (dom/i :.ion-heart)
          "Favorite Post"
          (dom/span :.counter
            " (" liked-by-count ")"))))))

(def ui-article-meta (comp/factory ArticleMeta {:keyfn :article/id}))

(declare load-article)

(defsc Article
  [this {:article/keys [id slug title body image comments]
         :keys [ph/article] :as props}]
  {:ident         :article/id
   :initial-state (fn [_] #:article{:id :none :comments (comp/get-initial-state comment/Comment #:comment{:id :none})})
   :initLocalState (fn [this _] {:editing-comment-id :none})
   :route-segment ["article" :article/id]
   :will-enter    (fn [app {:article/keys [id]}]
                    (let [id (if (string? id) (js/parseInt id) id)]
                      (comp/transact! app [(mutations/ensure-ident {:ident [:article/id id]})])
                      (dr/route-deferred [:article/id id]
                        #(comp/transact! app [(load-article {:article/id id})]))))
   :query         [:article/id :article/slug :article/title
                   :article/body :article/image
                   {:article/comments (comp/get-query comment/Comment)}
                   {:ph/article (comp/get-query ArticleMeta)}
                   {[:session/session :current-user] (comp/get-query session/CurrentUser)}]}
  (let [delete-comment #(comp/transact! this
                          [(mutations/delete-comment {:article/id id :comment/id %})])

        editing-comment-id     (comp/get-state this :editing-comment-id)
        set-editing-comment-id #(comp/set-state! this {:editing-comment-id %})

        computed-map {:article-id id
                      :delete-comment delete-comment
                      :editing-comment-id editing-comment-id
                      :set-editing-comment-id set-editing-comment-id
                      :current-user (get props [:session/session :current-user])}]
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
            (when (= :none editing-comment-id)
              (comment/ui-comment-form #:comment{:id :none} computed-map))
            (mapv #(comment/ui-comment % computed-map)
              comments)))))))

(defmutation load-article [{:article/keys [id]}]
  (action [{:keys [app]}]
    (let [ident [:article/id id]]
      (df/load! app ident Article {:without #{[:session/session :current-user]}})
      (dr/target-ready! app ident))))
