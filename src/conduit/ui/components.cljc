(ns conduit.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    #?(:cljs [fulcro.client.data-fetch :as df])
    #?(:cljs [fulcro.client.dom :as dom] :clj [fulcro.client.dom-server :as dom])))

(defsc Banner [this _]
  (dom/div :.banner
    (dom/div :.container
      (dom/h1 :.logo-font "conduit")
      (dom/p {} "A place to show off your tech stack."))))

(def ui-banner (prim/factory Banner))

(defsc UserPreview [this {:user/keys [username]}]
  {:query [:user/id :user/username]
   :ident [:user/by-id :user/id]})

(defsc ArticlePreviewMeta [this {:article/keys [author created-at liked-by-count]}]
  {:query [:article/id :article/created-at {:article/liked-by-count [:agg/count]}
           {:article/author (prim/get-query UserPreview)}]
   :ident [:article/by-id :article/id]}
  (dom/div :.article-meta
    (dom/a {:href (str "/users/" (:user/username author))}
      (dom/img {:src (:user/image author)}))
    (dom/div :.info
      (dom/a :.author {:href (str "/users/" (:user/username author))}
        (:user/username author))
      (dom/span :.date
        #?(:clj  created-at
           :cljs (when (instance? js/Date created-at)
                   (.toDateString created-at)))))
    (dom/button :.btn.btn-outline-primary.btn-sm.pull-xs-right
      (dom/i :.ion-heart)
      (:agg/count liked-by-count))))

(def ui-article-preview-meta (prim/factory ArticlePreviewMeta {:keyfn :article/id}))

(defsc ArticlePreview [this {:article/keys [slug title description] :keys [ph/article]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/slug :article/title :article/description
           {:ph/article (prim/get-query ArticlePreviewMeta)}]}
  (dom/div :.article-preview
    (ui-article-preview-meta article)
    (dom/a :.preview-link {:href (str "/articles/" slug)}
      (dom/h1 {} title)
      (dom/p {} description)
      (dom/span {} "Read more..."))))

(def ui-article-preview (prim/factory ArticlePreview {:keyfn :article/id}))

(defsc Tag [this {:tag/keys [tag]}]
  {:query [:tag/tag]}
  (dom/a  :.tag-pill.tag-default {:href (str "/tag/" tag)} tag))

(def ui-tag (prim/factory Tag))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p {} "Popular Tags")
      (dom/div :.tag-list
        (mapv ui-tag tags)))))

(def ui-tags (prim/factory Tags))

(defsc Feeds [this _]
  (dom/div :.feed-toggle
    (dom/ul :.nav.nav-pills.outline-active
      (dom/li :.nav-item
        (dom/a :.nav-link.disabled {:href ""} "Your Feed") )
      (dom/li :.nav-item
        (dom/a :.nav-link.active {:href ""} "Global Feed")))))

(def ui-feeds (prim/factory Feeds))

(defsc Home [this {articles :articles/all tags :tags/all}]
  {:query [{:articles/all (prim/get-query ArticlePreview)}
           {:tags/all (prim/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div #?(:cljs {:onClick #(do (df/load this :tags/all Tag)
                                     (df/load this :articles/all ArticlePreview))}) "update all")
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feeds)
          (mapv ui-article-preview articles))
        (ui-tags tags)))))

(defsc Profile [this {:user/keys [id username photo bio like]}]
  {:ident [:user/by-id :user/id]
   :query [:user/id :user/username :user/photo :user/bio
           {:user/like (prim/get-query ArticlePreview)}]}
  (dom/div :.profile-page
    ;;(dom/div {:onClick #(df/load this [:user/by-id 19] Profile)} "update this person")
    (dom/div :.user-info
      (dom/div :.container
        (dom/div :.row
          (dom/div :.col-xs-12.col-md-10.offset-md-1
            (dom/img :.user-img {:src photo})
            (dom/h4 {} username)
            (dom/p {} bio)
            (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
              (dom/i :.ion-plus-round) (str "Follow " username))))))
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/div :.articles-toggle
            (dom/ul :.nav.nav-pills.outline-active
              (dom/li :.nav-item
                (dom/a :.nav-link.active {:href ""}
                  "My Articles"))
              (dom/li :.nav-item
                (dom/a :.nav-link {:href ""}
                  "Favorited Articles"))))
          (mapv ui-article-preview like))))))

(def ui-profile (prim/factory Profile))
