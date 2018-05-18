(ns conduit.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.ui.form-state :as fs]
    [conduit.handler.mutations :as mutations]
    [fulcro.tempid :refer [tempid?]]
    #?(:cljs [fulcro.client.mutations :as m :refer [defmutation]])
    #?(:cljs [fulcro.client.data-fetch :as df])
    [fulcro.client.routing :as r]
    #?(:cljs [fulcro.client.dom :as dom] :clj [fulcro.client.dom-server :as dom])))

(declare SettingsForm)

#?(:cljs
   (defn go-to-settings [component {:user/keys [id]}]
     (prim/transact! component
       `[(use-settings-as-form {:user/id ~id})
         (r/route-to {:handler :screen/settings})])))

#?(:cljs
   (defn go-to-new-article [component]
     (prim/transact! component
       `[(create-temp-article-if-not-found)
         (use-current-temp-article-as-form)
         (r/route-to {:handler      :screen/editor
                      :route-params {:screen-id :current-temp-article}})
         :article-to-edit])))

#?(:cljs
   (defn edit-article [component {:article/keys [id] :as article}]
     (prim/transact! component
       `[(use-article-as-form ~article)
         (r/route-to {:handler      :screen/editor
                      :route-params {:screen-id ~id}})
         :article-to-edit])))

#?(:cljs
   (defn go-to-profile [component {:user/keys [id] :as profile}]
     (prim/transact! component
       `[(load-profile-to-screen ~profile)
         (load-owned-articles-to-screen ~profile)
         (r/route-to {:handler      :screen.profile/owned-articles
                      :route-params {:screen-id ~id}})
         :profile-to-view])))

#?(:cljs
   (defn go-to-article [component {:article/keys [id] :as article}]
     (prim/transact! component
       `[(load-article-to-screen ~article)
         (r/route-to {:handler      :screen/article
                      :route-params {:screen-id ~id}})
         :article-to-view])))

(declare ArticlePreview)

#?(:cljs
   (defn do-login [component]
     (prim/transact! component `[(login {:email "jake@jake.jake" :password "foobar"})])
     (df/load component :articles/all ArticlePreview)
     (df/load component :articles/feed ArticlePreview)))

(defsc NavBar [this {current-user-id :user/id :as props}]
  {:initial-state (fn [params] {})
   :query         [:user/id
                   [r/routers-table '_]]}
  (let [[current-screen _] (r/current-route props :router/top)
        logged-in? (number? current-user-id)]
    (dom/nav :.navbar.navbar-light
      (dom/div :.container
        (dom/div :.navbar-brand
          "conduit")
        (dom/ul :.nav.navbar-nav.pull-xs-right
          (dom/li :.nav-item
            (dom/div :.nav-link
              {:className (when (= current-screen :screen/home) "active")
               :onClick   #?(:cljs #(prim/transact! this `[(r/route-to {:handler :screen/home})])
                             :clj nil)}
              "Home") )
          (when logged-in?
            (dom/li :.nav-item
              (dom/a :.nav-link
                {:className (when (= current-screen :screen/editor) "active")
                 :onClick   #?(:cljs #(go-to-new-article this)
                               :clj nil)}
                (dom/i :.ion-compose)
                "New Post")))
          (when logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/settings) "active")
                 :onClick   #?(:cljs #(go-to-settings this {:user/id current-user-id})
                               :clj nil)}
                (dom/i :.ion-gear-a)
                "Settings")))
          (when-not logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/login) "active")
                 :onClick   #?(:cljs #(do-login this)
                               :clj nil)}
                (dom/i :.ion-gear-a)
                "Login")))

          (when-not logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/sign-up) "active")
                 :onClick   #?(:cljs #(prim/transact! this
                                        `[(sign-up #:user{:username "jake",
                                                          :name     "Jake Ekaj"
                                                          :email    "jake@jake.jake"
                                                          :password "foobar"
                                                          :bio      "I work at statefarm",
                                                          :image    "https://static.productionready.io/images/smiley-cyrus.jpg"})])
                               :clj nil)}
                "Sign up"))))))))

(def ui-nav-bar (prim/factory NavBar))

(defsc Footer [this _]
  (dom/footer
    (dom/div :.container
      (dom/div :.logo-font "conduit")
      (dom/span :.attribution
        "An interactive learning project from "
        (dom/a {:href "https://thinkster.io"} "Thinkster")
        ". Code &amp; design licensed under MIT."))))

(def ui-footer (prim/factory Footer))

(defsc Banner [this _]
  (dom/div :.banner
    (dom/div :.container
      (dom/h1 :.logo-font "conduit")
      (dom/p {} "A place to show off your tech stack."))))

(def ui-banner (prim/factory Banner))

(defsc UserTinyPreview [this props]
  {:query [:user/id :user/username :user/name]
   :ident [:user/by-id :user/id]})

(defsc UserPreview [this props]
  {:query [:user/id :user/image :user/username :user/name :user/followed-by-me :user/followed-by-count]
   :ident [:user/by-id :user/id]})

#?(:cljs
   (defn js-date->string [date]
     (when (instance? js/Date date)
       (.toDateString date))))

(defsc ArticlePreviewMeta [this {:article/keys [author created-at liked-by-count]}]
  {:query [:article/id :article/created-at :article/liked-by-count :article/liked-by-me
           {:article/author (prim/get-query UserPreview)}]
   :ident [:article/by-id :article/id]}
  (dom/div :.article-meta
    (dom/a {:href (str "/users/" (:user/username author))}
      (dom/img {:src (:user/image author)}))
    (dom/div :.info
      (dom/div :.author
        {:onClick #?(:cljs #(go-to-profile this author)
                     :clj nil)}
        (:user/name author))
      (dom/span :.date
        #?(:clj  created-at
           :cljs (js-date->string created-at))))
    (dom/button :.btn.btn-outline-primary.btn-sm.pull-xs-right
      (dom/i :.ion-heart)
      liked-by-count)))

(def ui-article-preview-meta (prim/factory ArticlePreviewMeta {:keyfn :article/id}))

(defsc ArticlePreview [this {:article/keys [id author-id slug title description] :keys [ph/article]}
                       {:keys [on-delete on-edit]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/author-id :article/slug :article/title :article/description :article/body
           {:ph/article (prim/get-query ArticlePreviewMeta)}]}
  (dom/div :.article-preview
    (ui-article-preview-meta article)
    (dom/div :.preview-link
      (dom/h1 {:onClick #?(:cljs #(go-to-article this {:article/id id})
                           :clj nil)}
        title)
      (dom/p {} description)
      (dom/p {:onClick #?(:cljs #(on-edit {:article/id id})
                          :clj nil)}
        "Edit me")
      (dom/p {:onClick #?(:cljs #(on-delete {:article/id id})
                          :clj nil)}
        "Delete me")
      (dom/span {} "Read more..."))))

(def ui-article-preview (prim/factory ArticlePreview {:keyfn :article/id}))

(defsc ArticleMeta [this {:article/keys [id created-at article liked-by-count liked-by-me
                                         author]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/created-at :article/liked-by-count :article/liked-by-me
           {:article/author (prim/get-query UserPreview)}]}
  (dom/div :.article-meta
    (dom/div {:onClick #?(:cljs #(go-to-profile this author)
                          :clj nil)}
      (dom/img {:src (:user/image author)}))
    (dom/div :.info
      (dom/div :.author {:onClick #?(:cljs #(go-to-profile this author)
                                     :clj nil)}
        (:user/name author))
      (dom/span :.date
        #?(:clj  created-at
           :cljs (js-date->string created-at))))

    (if (:user/followed-by-me author)
      (dom/button :.btn.btn-sm.btn-outline-primary
        {:onClick #?(:cljs #(prim/transact! this `[(mutations/unfollow ~author)])
                     :clj nil)}
        (dom/i :.ion-plus-round)
        "Unfollow " (:user/name author)
        (dom/span :.counter "(" (:user/followed-by-count author) ")"))
      (dom/button :.btn.btn-sm.btn-outline-secondary
        {:onClick #?(:cljs #(prim/transact! this `[(mutations/follow ~author)])
                     :clj nil)}
        (dom/i :.ion-plus-round)
        "Follow " (:user/name author)
        (dom/span :.counter "(" (:user/followed-by-count author) ")")))

    (if liked-by-me
      (dom/button :.btn.btn-sm.btn-outline-primary
        {:onClick #?(:cljs #(prim/transact! this `[(mutations/unlike {:article/id ~id})])
                     :clj nil)}
        (dom/i :.ion-heart)
        "Unfavorite Post"
        (dom/span :.counter
          "(" liked-by-count ")"))
      (dom/button :.btn.btn-sm.btn-outline-secondary
        {:onClick #?(:cljs #(prim/transact! this `[(mutations/like {:article/id ~id})])
                     :clj nil)}
        (dom/i :.ion-heart)
        "Favorite Post"
        (dom/span :.counter
          "(" liked-by-count ")")))))

(def ui-article-meta (prim/factory ArticleMeta {:keyfn :article/id}))

(defsc Comment [this {:comment/keys [author body created-at]}]
  {:ident [:comment/by-id :comment/id]
   :query [:comment/id :comment/created-at
           {:comment/author (prim/get-query UserTinyPreview)}]}
  (dom/div :.card
    (dom/div :.card-block
      (dom/p :.card-text
        body))
    (dom/div :.card-footer
      (dom/div :.comment-author #_{:onClick #?(:cljs identity :clj nil)}
        (dom/img :.comment-author-img
          {:src (:user/image author)}))
      (dom/a :.comment-author #_{:onClick #?(:cljs identity :clj nil)}
        (:user/name author))
      (dom/span :.date-posted
        #?(:clj  created-at
           :cljs (js-date->string created-at)))
      (dom/span :.mod-options
        (dom/i :.ion-edit #_{:onClick #?(:cljs identity :clj nil)} " ")
        (dom/i :.ion-trash-a #_{:onClick #?(:cljs identity :clj nil)} " ")))))

(def ui-comment (prim/factory Comment {:keyfn :comment/id}))

(defn focus-field [component ref-name]
  (let [input-field        (dom/node component ref-name)
        input-field-length (.. input-field -value -length)]
    (.focus input-field)
    (.setSelectionRange input-field input-field-length input-field-length)))

(defsc CommentForm [this {:comment/keys [id body author]} {:keys [on-focus]}]
  {:query       [:comment/id :comment/body {:comment/author (prim/get-query UserTinyPreview)}
                 fs/form-config-join]
   :ident       [:comment/by-id :comment/id]
   :componentDidMount #(when (tempid? (:comment/id (prim/props this)))
                         (focus-field this "comment_field"))
   :form-fields #{:comment/body}}
  (dom/form :.card.comment-form
    (dom/div :.card-block
      (dom/textarea :.form-control
        {:placeholder (if (= id :none) "Placeholder" "Write a comment...")
         :rows "3"
         :ref         "comment_field"
         :value       body
         :onFocus     on-focus
         :onBlur
         #?(:clj  nil
            :cljs #(prim/transact! this
                     `[(fs/mark-complete! {:field :comment/body})]))
         :onChange
         #?(:clj nil
            :cljs #(m/set-string! this :comment/body :event %))}))
    (dom/div :.card-footer
      (dom/img :.comment-author-img
        {:src (:user/image author)})
      (dom/button :.btn.btn-sm.btn-primary
        "Post Comment"))))

(def ui-comment-form (prim/factory CommentForm {:keyfn :comment/id}))

(defsc Article [this {:article/keys [id author-id slug title description body image comments]
                      :keys         [ph/article]}
                {:keys [new-comment]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/author-id :article/slug :article/title :article/description
           :article/body :article/image
           {:article/comments (prim/get-query Comment)}
           {:ph/article (prim/get-query ArticleMeta)}]}
  (let [on-focus-comment #?(:clj  nil
                            :cljs #(prim/transact! this
                                     `[(create-temp-comment-if-not-found {:article/id ~id})]))]
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
            (ui-comment-form (prim/computed new-comment {:on-focus on-focus-comment}))
            (map ui-comment comments)))))))

(def ui-article (prim/factory Article {:keyfn :article/id}))

(defsc Tag [this {:tag/keys [tag]}]
  {:query [:tag/tag :tag/count]}
  (dom/a  :.tag-pill.tag-default {:href (str "/tag/" tag)} tag))

(def ui-tag (prim/factory Tag {:keyfn :tag/tag}))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p {} "Popular Tags")
      (dom/div :.tag-list
        (mapv ui-tag tags)))))

(def ui-tags (prim/factory Tags))

(defn article-list
  [component articles msg-when-empty]
  (let [edit-article   (fn [{:article/keys [id] :as article}]
                         #?(:cljs (edit-article component article)))
        delete-article (fn [{:article/keys [id] :as article}]
                         (prim/transact! component `[(mutations/delete-article ~article)]))]
    (dom/div
      (if (seq articles)
        (map (fn [a] (ui-article-preview (prim/computed a {:on-delete delete-article
                                                           :on-edit   edit-article})))
          articles)
        msg-when-empty))))

(defsc PersonalFeed [this {:keys [screen] articles :articles/feed}]
  {:initial-state {:screen :screen.feed/personal}
   :ident         (fn [] [screen :top])
   :query         [:screen
                   {[:articles/feed '_] (prim/get-query ArticlePreview)}]}
  (article-list this articles "You have no article!"))

(defsc GlobalFeed [this {:keys [screen] articles :articles/all}]
  {:initial-state {:screen :screen.feed/global}
   :ident         (fn [] [screen :top])
   :query         [:screen
                   {[:articles/all '_] (prim/get-query ArticlePreview)}]}
  (article-list this articles "No article!"))

(r/defrouter FeedsRouter :router/feeds
  (fn [this props] [(:screen props) :top])
  :screen.feed/global   GlobalFeed
  :screen.feed/personal PersonalFeed)

(def ui-feeds-router (prim/factory FeedsRouter))

(defsc FeedSelector [this props]
  {:initial-state (fn [params] {})
   :query         [[r/routers-table '_]]}
  (let [[current-screen _] (r/current-route props :router/feeds)]
    (dom/div :.feed-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (if (= current-screen :screen.feed/personal) "active" "disabled")
             :onClick #(prim/transact! this `[(load-personal-feed)
                                              (r/route-to {:handler :screen.feed/personal})])}
            "Your Feed"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (if (= current-screen :screen.feed/global) "active" "disabled")
             :onClick #(prim/transact! this `[(r/route-to {:handler :screen.feed/global})])}
            "Global Feed"))))))

(def ui-feed-selector (prim/factory FeedSelector))

(defsc Home [this {tags          :tags/all
                   router        :router/feeds
                   feed-selector :feed-selector}]
  {:initial-state (fn [params] {:screen        :screen/home
                                :screen-id     :top
                                :feed-selector (prim/get-initial-state FeedSelector {})
                                :router/feeds  (prim/get-initial-state FeedsRouter {})})

   :query [:screen :screen-id
           {:feed-selector (prim/get-query FeedSelector)}
           {:router/feeds (prim/get-query FeedsRouter)}
           {[:tags/all '_] (prim/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feed-selector feed-selector)
          (ui-feeds-router router))
        (ui-tags tags)))))

(defsc LikedArticles [this {:user/keys [id] articles :user/like}]
  {:ident         [:user/by-id :user/id]
   :query         [:user/id {:user/like (prim/get-query ArticlePreview)}]}
  (article-list this articles "This user liked no article!"))

(def ui-liked-articles (prim/factory LikedArticles))

(defsc LikedArticlesScreen
  [this {:keys [screen profile-to-view] user-id :screen-id}]
  {:initial-state (fn [params]
                    {:screen          :screen.profile/liked-articles
                     :screen-id       :guest
                     :profile-to-view [:user/by-id :guest]})
   :ident         (fn [] [screen user-id])
   :query         [:screen :screen-id {:profile-to-view (prim/get-query LikedArticles)}]}
  (ui-liked-articles profile-to-view))

(defsc OwnedArticles [this {:user/keys [id articles]}]
  {:ident         [:user/by-id :user/id]
   :query         [:user/id {:user/articles (prim/get-query ArticlePreview)}]}
  (article-list this articles "This user has no article!"))

(def ui-owned-articles (prim/factory OwnedArticles))

(defsc OwnedArticlesScreen
  [this {:keys [screen profile-to-view] user-id :screen-id}]
  {:initial-state (fn [params]
                    {:screen          :screen.profile/owned-articles
                     :screen-id       :guest
                     :profile-to-view [:user/by-id :guest]})
   :ident         (fn [] [screen user-id])
   :query         [:screen :screen-id {:profile-to-view (prim/get-query OwnedArticles)}]}
  (ui-owned-articles profile-to-view))

(r/defrouter ProfileRouter :router/profile
  [:screen :screen-id]
  :screen.profile/owned-articles OwnedArticlesScreen
  :screen.profile/liked-articles LikedArticlesScreen)

(def ui-profile-router (prim/factory ProfileRouter))

(defsc Profile [this {:user/keys [id name username image bio followed-by-me]}]
  {:ident         [:user/by-id :user/id]
   :query         [:user/id :user/name :user/username :user/image :user/bio :user/followed-by-me]}
  (dom/div :.user-info
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/img :.user-img {:src image})
          (dom/h4 {} name)
          (dom/p {} bio)
          (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
            {:onClick #?(:cljs #(if followed-by-me
                                  (prim/transact! this `[(mutations/unfollow {:user/id ~id})])
                                  (prim/transact! this `[(mutations/follow {:user/id ~id})]))
                         :clj nil)}
            (dom/i :.ion-plus-round)
            (str (if followed-by-me "Unfollow " "Follow ") name)))))))

(def ui-profile (prim/factory Profile))

(declare ArticleEditor)

(defn create-temp-article-if-not-found
  [tempid-fn state]
  (if (tempid? (get-in state [:screen/editor :current-temp-article :article-to-edit 1]))
    state
    (let [tempid              (tempid-fn)
          current-user        (:user/whoami state)
          [_ current-user-id] current-user

          new-item #:article {:id          tempid
                              :title       ""
                              :slug        ""
                              :description ""
                              :body        ""
                              :author      current-user
                              :tags        []}]
      (-> (assoc-in state [:article/by-id tempid] new-item)
        (update-in [:user/by-id current-user-id :user/articles]
          (fnil conj []) [:article/by-id tempid])
        (assoc-in [:screen/editor :current-temp-article]
          {:screen          :screen/editor
           :screen-id       :current-temp-article
           :article-to-edit [:article/by-id tempid]})))))

#?(:cljs
   (defmutation create-temp-article-if-not-found [_]
     (action [{:keys [state]}]
       (swap! state #(create-temp-article-if-not-found prim/tempid %)))))

#?(:cljs
   (defmutation use-current-temp-article-as-form [_]
     (action [{:keys [state]}]
       (swap! state #(let [temp-ident (get-in % [:screen/editor :current-temp-article :article-to-edit])]
                       (fs/add-form-config* % ArticleEditor temp-ident))))))

#?(:cljs
   (defmutation use-article-as-form [{:article/keys [id]}]
     (action [{:keys [state]}]
       (swap! state #(-> %
                       (fs/add-form-config* ArticleEditor [:article/by-id id])
                       (assoc-in [:screen/editor id]
                         {:screen          :screen/editor
                          :screen-id       id
                          :article-to-edit [:article/by-id id]}))))
     (refresh [env] [:screen])))

(defsc ArticleEditor [this {:article/keys [id slug title description body] :as props}]
  {:initial-state (fn [{:article/keys [id]}] #:article{:id id})
   :query       [:article/id :article/slug  :article/title :article/description :article/body
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
                (dom/input :.form-control
                  {:placeholder "Slug",
                   :type        "text"
                   :value       slug
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/slug})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/slug :event %))}))
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
                    :cljs #(prim/transact! this `[(mutations/submit-article ~(fs/dirty-fields props false))]))}
                (if (tempid? id)
                  "Publish Article"
                  "Update Article")))))))))

(def ui-article-editor (prim/factory ArticleEditor))

(defsc Settings [this props]
  {:initial-state (fn [params] {})
   :query         [:user/image :user/name :user/bio :user/email]})

#?(:cljs
   (defmutation load-personal-feed [_]
     (action [{:keys [state] :as env}]
       (df/load-action env :articles/feed ArticlePreview))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation login [credentials]
     (action [{:keys [state] :as env}]
       (df/load-action env :user/whoami SettingsForm
         {:params  {:login credentials}
          :without #{:fulcro.ui.form-state/config}}))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation sign-up [new-user]
     (action [{:keys [state] :as env}]
       (df/load-action env :user/whoami SettingsForm
         {:params  {:sign-up new-user}
          :without #{:fulcro.ui.form-state/config}}))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation load-profile-to-screen [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:user/by-id id] Profile {:without #{:router/profile}})
       (swap! state
         #(-> %
            (assoc-in [:screen/profile id]
              {:screen          :screen/profile
               :screen-id       id
               :profile-to-view [:user/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:screen :profile-to-view])))

#?(:cljs
   (defmutation load-article-to-screen [{:article/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:article/by-id id] Article)
       (swap! state
         #(-> %
            (assoc-in [:screen/article id]
              {:screen          :screen/article
               :screen-id       id
               :article-to-view [:article/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:screen :article-to-view])))

#?(:cljs
   (defmutation load-liked-articles-to-screen [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:user/by-id id] LikedArticles)
       (swap! state
         #(-> %
            (assoc-in [:screen.profile/liked-articles id]
              {:screen          :screen.profile/liked-articles
               :screen-id       id
               :profile-to-view [:user/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:profile-to-view])))

#?(:cljs
   (defmutation load-owned-articles-to-screen [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:user/by-id id] OwnedArticles)
       (swap! state
         #(-> %
            (assoc-in [:screen.profile/owned-articles id]
              {:screen          :screen.profile/owned-articles
               :screen-id       id
               :profile-to-view [:user/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:profile-to-view])))

#?(:cljs
   (defmutation use-settings-as-form [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (swap! state #(-> %
                       (fs/add-form-config* SettingsForm [:user/by-id id])
                       (assoc-in [:root/settings-form :settings] [:user/by-id id]))))))

(defsc SettingsForm [this {:user/keys [id image name bio email] :as props}]
  {:query       [:user/id :user/image :user/name :user/bio :user/email
                 fs/form-config-join]
   :ident [:user/by-id :user/id]
   :form-fields #{:user/image :user/name :user/bio :user/email}}
  (dom/div :.settings-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-6.offset-md-3.col-xs-12
          (dom/h1 :.text-xs-center
            "Your Settings")
          (dom/form {}
            (dom/fieldset {}
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "URL of profile picture",
                   :type        "text"
                   :value       image
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/image})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/image :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name",
                   :type        "text"
                   :value       name
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/name})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/name :event %))}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control.form-control-lg
                  {:rows        "8",
                   :placeholder "Short bio about you"
                   :value       (or bio "")
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/bio})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/bio :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email",
                   :type        "text"
                   :value       email
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/email})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/email :event %))}))
              #_
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password",
                   :type        "password"}))
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:onClick
                 #?(:clj  nil
                    :cljs #(prim/transact! this `[(mutations/submit-settings ~(fs/dirty-fields props false))]))}
                "Update Settings"))))))))

(def ui-settings-form (prim/factory SettingsForm))

(defsc SettingScreen [this {settings [:root/settings-form :settings] :as props}]
  {:initial-state (fn [params] {:screen             :screen/settings
                                :screen-id          :top

                                [:root/settings-form :settings]
                                (prim/get-initial-state SettingsForm {})})
   :query         [:screen :screen-id
                   {[:root/settings-form :settings] (prim/get-query SettingsForm)}]}
  (ui-settings-form settings))

(defsc EditorScreen [this {:keys [screen article-to-edit] article-id :screen-id}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/editor
                                :screen-id       :current-temp-article
                                :article-to-edit {}})
   :query         (fn [] [:screen :screen-id
                          {:article-to-edit (prim/get-query ArticleEditor)}])}
  (ui-article-editor article-to-edit))

(defsc ProfileScreen [this {:keys   [screen profile-to-view]
                            user-id :screen-id
                            router  [r/routers-table :router/profile]}]
  {:ident         (fn [] [screen user-id])
   :initial-state (fn [params] {:screen          :screen/profile
                                :screen-id       :guest
                                :profile-to-view [:user/by-id :guest]
                                :router/profile  {}})
   :query         (fn [] [:screen :screen-id
                          {[r/routers-table :router/profile] (prim/get-query ProfileRouter)}
                          {:profile-to-view (prim/get-query Profile)}])}
  (dom/div :.profile-page
    (ui-profile profile-to-view)
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/div :.articles-toggle
            (dom/ul :.nav.nav-pills.outline-active
              (dom/li :.nav-item
                (dom/div :.nav-link.active
                  {:onClick #?(:cljs #(prim/transact! this
                                        `[(load-owned-articles-to-screen {:user/id ~user-id})
                                          (r/route-to {:handler      :screen.profile/owned-articles
                                                       :route-params {:screen-id ~user-id}})
                                          :profile-to-view])
                               :clj nil)}
                  "My Articles"))
              (dom/li :.nav-item
                (dom/div :.nav-link
                  {:onClick #?(:cljs #(prim/transact! this
                                        `[(load-liked-articles-to-screen {:user/id ~user-id})
                                          (r/route-to {:handler      :screen.profile/liked-articles
                                                       :route-params {:screen-id ~user-id}})
                                          :profile-to-view])
                               :clj nil)}
                  "Favorited Articles"))))
          (ui-profile-router router))))))

(defsc ArticleScreen [this {:keys [screen article-to-view new-comment] article-id :screen-id}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/article
                                :screen-id       :none
                                :article-to-view (prim/get-initial-state Article #:article{:id :none})
                                :new-comment     (prim/get-initial-state CommentForm #:comment{:id :none})})
   :query         (fn [] [:screen :screen-id
                          {:article-to-view (prim/get-query Article)}
                          {:new-comment (prim/get-query CommentForm)}])}
  (ui-article (prim/computed article-to-view {:new-comment new-comment})))
