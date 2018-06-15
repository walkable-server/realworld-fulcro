(ns conduit.ui.root
  (:require [fulcro.client.routing :as r]
            [conduit.ui.article-preview :as preview]
            [conduit.ui.other :as other]
            [conduit.handler.mutations :as mutations]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [conduit.ui.home :as home]
            [fulcro.client.dom :as dom]
            [conduit.ui.editor :as editor]
            [conduit.ui.account :as account]
            [conduit.ui.article :as article]
            [conduit.ui.profile :as profile]
            [conduit.ui.pagination :as pagination]
            [conduit.util :as util]
            [conduit.ui.routes :as routes]))

(r/defrouter TopRouter :router/top
  (fn [this props]
    (let [screen-name   (:screen props)
          screen-id-key (case screen-name
                          (:screen/editor :screen/article)
                          :article-id

                          :screen.profile/by-user-id
                          :user-id

                          :screen/feed
                          :feed-id

                          :screen-id)
          screen-id (get props screen-id-key)]
      [screen-name screen-id]))

  :screen/feed     home/HomeScreen
  :screen/settings account/SettingScreen
  :screen/editor   editor/EditorScreen
  :screen/log-in   account/LogInScreen
  :screen/sign-up  account/SignUpScreen
  :screen/article  article/ArticleScreen
  :screen.profile/by-user-id  profile/ProfileScreen)

(def ui-top (prim/factory TopRouter))

(def routing-tree
  (r/routing-tree
    (r/make-route :screen/editor
      [(r/router-instruction :router/top [:screen/editor :param/article-id])])

    (r/make-route :screen/article
      [(r/router-instruction :router/top [:screen/article :param/article-id])])

    (r/make-route :screen/settings
      [(r/router-instruction :router/top [:screen/settings :top])])
    (r/make-route :screen/sign-up
      [(r/router-instruction :router/top [:screen/sign-up :top])])
    (r/make-route :screen/log-in
      [(r/router-instruction :router/top [:screen/log-in :top])])

    (r/make-route :screen/feed
      [(r/router-instruction :router/top [:screen/feed :top])
       (r/router-instruction :router/paginated-list [:screen/paginated-list :param/paginated-list])])

    (r/make-route :screen.profile/by-user-id
      [(r/router-instruction :router/top [:screen.profile/by-user-id :param/user-id])
       (r/router-instruction :router/paginated-list [:screen/paginated-list :param/paginated-list])])))

(defsc Root [this {router :router/top :as props}]
  {:initial-state (fn [params] (merge routing-tree
                                 {:article/by-id {:none #:article {:id          :none
                                                                   :body        ""
                                                                   :title       ""
                                                                   :slug        ""
                                                                   :description ""
                                                                   :comments    []
                                                                   :author      {:user/id :guest}}}
                                  :user/whoami   #:user {:id    :guest
                                                         :name  "Guest"
                                                         :image "https://static.productionready.io/images/smiley-cyrus.jpg"
                                                         :email "non@exist"}}

                                 {:router/top (prim/get-initial-state TopRouter {})}))
   :query         [{:router/top (prim/get-query TopRouter)}
                   {:user/whoami (prim/get-query other/UserTinyPreview)}]}
  (dom/div
    (home/ui-nav-bar)
    (ui-top router)
    #_
    (home/ui-footer)))

(defn started-callback [{:keys [reconciler] :as app}]
  (df/load app :user/whoami other/UserTinyPreview)
  (df/load app :tags/all home/Tag)
  ;;(routes/go-to-profile reconciler #:user{:id 2})
  (routes/go-to-feed reconciler :global))
