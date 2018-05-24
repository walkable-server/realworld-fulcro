(ns conduit.ui.profile
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other]
   [conduit.ui.comment :as comment]
   [conduit.ui.routes :as routes]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.routing :as r]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]))

(defsc LikedArticles [this {:user/keys [id] articles :user/like}]
  {:ident         [:user/by-id :user/id]
   :initial-state (fn [params] #:user{:id :guest :like []})
   :query         [:user/id {:user/like (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles "This user liked no article!"))

(def ui-liked-articles (prim/factory LikedArticles))

(defsc LikedArticlesScreen
  [this {:keys [screen profile-to-view user-id]}]
  {:initial-state (fn [params]
                    {:screen          :screen.liked-articles/by-user-id
                     :user-id         :guest
                     :profile-to-view (prim/get-initial-state LikedArticles #:user{:id :guest})})
   :ident         (fn [] [screen user-id])
   :query         [:screen :user-id {:profile-to-view (prim/get-query LikedArticles)}]}
  (ui-liked-articles profile-to-view))

(defsc OwnedArticles [this {:user/keys [id articles]}]
  {:ident         [:user/by-id :user/id]
   :initial-state (fn [params] #:user{:id :guest :articles []})
   :query         [:user/id {:user/articles (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles "This user has no article!"))

(def ui-owned-articles (prim/factory OwnedArticles))

(defsc OwnedArticlesScreen
  [this {:keys [screen profile-to-view user-id]}]
  {:initial-state (fn [params]
                    {:screen          :screen.owned-articles/by-user-id
                     :user-id         :guest
                     :profile-to-view (prim/get-initial-state OwnedArticles #:user {:id :guest})})
   :ident         (fn [] [screen user-id])
   :query         [:screen :user-id {:profile-to-view (prim/get-query OwnedArticles)}]}
  (ui-owned-articles profile-to-view))

(r/defrouter ProfileRouter :router/profile
  [:screen :user-id]
  :screen.owned-articles/by-user-id OwnedArticlesScreen
  :screen.liked-articles/by-user-id LikedArticlesScreen)

(def ui-profile-router (prim/factory ProfileRouter))

(defsc Profile [this {:user/keys [id name username image bio followed-by-me]}]
  {:ident         [:user/by-id :user/id]
   :initial-state (fn [params] #:user {:id :guest :like [] :articles []})
   :query         [:user/id :user/name :user/username :user/image :user/bio :user/followed-by-me
                   {:user/like (prim/get-query preview/ArticlePreview)}
                   {:user/articles (prim/get-query preview/ArticlePreview)}]}
  (dom/div :.user-info
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/img :.user-img {:src image})
          (dom/h4 name)
          (dom/p bio)
          (let [current-user-id (-> (prim/shared this :user/whoami) :user/id)]
            (when (not= id current-user-id)
              (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
                {:onClick #(if (= :guest current-user-id)
                             (js/alert "You must log in first")
                             (if followed-by-me
                               (prim/transact! this `[(mutations/unfollow {:user/id ~id})])
                               (prim/transact! this `[(mutations/follow {:user/id ~id})])))}
                (dom/i :.ion-plus-round)
                (str (if followed-by-me "Unfollow " "Follow ") name)))))))))

(def ui-profile (prim/factory Profile))

(defsc ListSelector [this props {:user/keys [id name]}]
  {:query [[r/routers-table '_]]}
  (let [[current-screen _] (r/current-route props :router/profile)
        whoami             (prim/shared this :user/whoami)]
    (dom/div :.articles-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (when (= current-screen :screen.owned-articles/by-user-id) "active")
             :onClick   #(routes/go-to-profile this {:user/id id})}
            (if (= (:user/id whoami) id)
              "My"
              (str name "'s"))
            " Articles"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (when (= current-screen :screen.liked-articles/by-user-id) "active")
             :onClick #(routes/go-to-liked-article this {:user/id id})}
            "Favorited Articles"))))))

(def ui-list-selector (prim/factory ListSelector))

(defsc ProfileScreen [this {:keys  [screen profile-to-view user-id list-selector]
                            router [r/routers-table :router/profile]}]
  {:ident         (fn [] [screen user-id])
   :initial-state (fn [params] {:screen          :screen.profile/by-user-id
                                :user-id         :guest
                                :list-selector   {}
                                :profile-to-view (prim/get-initial-state Profile #:user{:id :guest})})
   :query         (fn [] [:screen :user-id
                          {[r/routers-table :router/profile] (prim/get-query ProfileRouter)}
                          {:list-selector (prim/get-query ListSelector)}
                          {:profile-to-view (prim/get-query Profile)}])}
  (dom/div :.profile-page
    (ui-profile profile-to-view)
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (ui-list-selector (prim/computed list-selector profile-to-view))
          (ui-profile-router router))))))

(defmutation load-profile-to-screen [{:user/keys [id]}]
  (action [{:keys [state] :as env}]
    (df/load-action env [:user/by-id id] Profile {:without #{:user/like}})
    (swap! state
      #(-> %
         (assoc-in [:screen.profile/by-user-id id]
           {:screen          :screen.profile/by-user-id
            :user-id         id
            :list-selector   {}
            :profile-to-view [:user/by-id id]})
         (assoc-in [:screen.owned-articles/by-user-id id]
           {:screen          :screen.owned-articles/by-user-id
            :user-id         id
            :profile-to-view [:user/by-id id]}))))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:screen :profile-to-view]))

(defmutation load-liked-articles-to-screen [{:user/keys [id]}]
  (action [{:keys [state] :as env}]
    (df/load-action env [:user/by-id id] Profile {:focus [:user/like]})
    (swap! state
      #(-> %
         (assoc-in [:screen.liked-articles/by-user-id id]
           {:screen          :screen.liked-articles/by-user-id
            :user-id         id
            :profile-to-view [:user/by-id id]}))))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:profile-to-view]))

(defmutation load-owned-articles-to-screen [{:user/keys [id]}]
  (action [{:keys [state] :as env}]
    (df/load-action env [:user/by-id id] Profile {:focus [:user/articles]}))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:profile-to-view]))
