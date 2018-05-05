(ns conduit.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.ui.form-state :as fs]
    [conduit.handler.mutations :as mutations]
    #?(:cljs [fulcro.client.mutations :as m :refer [defmutation]])
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
   :query [:article/id :article/slug :article/title :article/description :article/body
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

(declare ArticleEditor)

#?(:cljs
   (defmutation use-article-as-form [{:article/keys [id]}]
     (action [{:keys [state]}]
       (swap! state #(-> %
                       (fs/add-form-config* ArticleEditor [:article/by-id id])
                       (assoc-in [:root/article-editor :article-to-edit] [:article/by-id id]))))))

(defsc ArticleEditor [this {:article/keys [id slug title description body] :as props}]
  {:query       [:article/id :article/slug  :article/title :article/description :article/body
                 fs/form-config-join]
   :ident       [:article/by-id :article/id]
   :form-fields #{:article/slug  :article/title
                  :article/description :article/body}}
  (dom/div :.editor-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-10.offset-md-1.col-xs-12
          (dom/form {}
            (dom/fieldset {}
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Article Title",
                   :type        "text"
                   :value       title
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/title})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/title :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "What's this article about?",
                   :type        "text"
                   :value       description
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/description})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/description :event %))}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control
                  {:rows  "8", :placeholder "Write your article (in markdown)"
                   :value body
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/body})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/body :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "Enter tags",
                   :type        "text"})
                (dom/div :.tag-list))
              (dom/button :.btn.btn-lg.pull-xs-right.btn-primary
                {:type "button"
                 :onClick
                 #?(:clj  nil
                    :cljs #(prim/transact! this `[(mutations/submit-article ~{:article/id id :diff (fs/dirty-fields props)})]))}
                "Publish Article"))))))))

(def ui-article-editor (prim/factory ArticleEditor))
