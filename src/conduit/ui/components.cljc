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
   (defn go-to-login [component]
     (prim/transact! component
       `[(r/route-to {:handler :screen/log-in})
         :screen])))

#?(:cljs
   (defn go-to-sign-up [component]
     (prim/transact! component
       `[(load-sign-up-form)
         (r/route-to {:handler :screen/sign-up})])))

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
                      :route-params {:article-id :current-temp-article}})
         :article-to-edit])))

#?(:cljs
   (defn edit-article [component {:article/keys [id] :as article}]
     (prim/transact! component
       `[(load-article-to-editor ~article)
         (use-article-as-form ~article)
         (r/route-to {:handler      :screen/editor
                      :route-params {:article-id ~id}})
         :article-to-edit])))

#?(:cljs
   (defn go-to-profile [component {:user/keys [id] :as profile}]
     (prim/transact! component
       `[(load-profile-to-screen ~profile)
         (load-owned-articles-to-screen ~profile)
         (r/route-to {:handler      :screen.profile/owned-articles
                      :route-params {:user-id ~id}})
         :profile-to-view])))

#?(:cljs
   (defn go-to-article [component {:article/keys [id] :as article}]
     (prim/transact! component
       `[(load-article-to-screen ~article)
         (r/route-to {:handler      :screen/article
                      :route-params {:article-id ~id}})
         :article-to-view])))

(declare ArticlePreview)

(defsc NavBar [this {current-user-id :user/id :as props}]
  {:query         [:user/id
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
                {:className (when (= current-screen :screen/log-in) "active")
                 :onClick   #?(:cljs #(go-to-login this)
                               :clj nil)}
                (dom/i :.ion-gear-a)
                "Login")))

          (when-not logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/sign-up) "active")
                 :onClick   #?(:cljs #(go-to-sign-up this)
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
      (dom/p "A place to show off your tech stack."))))

(def ui-banner (prim/factory Banner))

(defsc UserTinyPreview [this props]
  {:query [:user/id :user/username :user/name]
   :initial-state (fn [params] #:user{:id :guest})
   :ident [:user/by-id :user/id]})

(defsc UserPreview [this props]
  {:query [:user/id :user/image :user/username :user/name :user/followed-by-me :user/followed-by-count]
   :initial-state (fn [params] #:user{:id :guest})
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
      (dom/p description)
      (dom/p {:onClick #?(:cljs #(on-edit {:article/id id})
                          :clj nil)}
        "Edit me")
      (dom/p {:onClick #?(:cljs #(on-delete {:article/id id})
                          :clj nil)}
        "Delete me")
      (dom/span "Read more..."))))

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

(declare ui-comment-form)

(defsc Comment [this {:comment/keys [id author body created-at] :as props}
                {:keys [delete-comment editing-comment-id set-editing-comment-id] :as computed-map}]
  {:ident         [:comment/by-id :comment/id]
   :initial-state (fn [{:comment/keys [id]}]
                    #:comment{:id     id
                              :body   ""
                              :author (prim/get-initial-state UserTinyPreview #:user{:id :guest})})
   :query         [:comment/id :comment/created-at :comment/body
                   {:comment/author (prim/get-query UserTinyPreview)}]}
  (if (= editing-comment-id id)
    (ui-comment-form (prim/computed props computed-map))
    (dom/div :.card
      (dom/div :.card-block
        (dom/p :.card-text
          body))
      (dom/div :.card-footer
        (dom/div :.comment-author {:onClick #?(:cljs #(go-to-profile this author)
                                               :clj nil)}
          (dom/img :.comment-author-img
            {:src (:user/image author)}))
        (dom/div :.comment-author {:onClick #?(:cljs #(go-to-profile this author)
                                               :clj nil)}
          (:user/name author))
        (dom/span :.date-posted
          #?(:clj  created-at
             :cljs (js-date->string created-at)))
        (dom/span :.mod-options
          (dom/i :.ion-edit {:onClick #?(:cljs #(set-editing-comment-id id) :clj nil)} " ")
          (dom/i :.ion-trash-a {:onClick #?(:cljs #(delete-comment id) :clj nil)} " "))))))

(def ui-comment (prim/factory Comment {:keyfn :comment/id}))

(defn focus-field [component ref-name]
  (let [input-field        (dom/node component ref-name)
        input-field-length (.. input-field -value -length)]
    (.focus input-field)
    (.setSelectionRange input-field input-field-length input-field-length)))

(defsc CommentForm [this {:comment/keys [id body author] :as props} {:keys [article-id set-editing-comment-id]}]
  {:query             [:comment/id :comment/body {:comment/author (prim/get-query UserTinyPreview)}]
   :initial-state     (fn [params] #:comment{:id     :none
                                             :body   ""
                                             :author (prim/get-initial-state UserTinyPreview #:user {:id :guest})})
   :ident             [:comment/by-id :comment/id]
   :componentDidMount #(when (number? (:comment/id (prim/props this)))
                         (focus-field this "comment_field"))}
  (let [state (prim/get-state this)]
    (dom/form :.card.comment-form
      (dom/div :.card-block
        (dom/textarea :.form-control
          {:placeholder "Write a comment..."
           :rows        "3"
           :ref         "comment_field"
           :value       (or (:comment/body state) body "")
           :onChange
           #?(:clj  nil
              :cljs #(prim/set-state! this {:comment/body (.. % -target -value)}))}))
      (dom/div :.card-footer
        (dom/img :.comment-author-img
          {:src (:user/image author)})
        (let [can-submit? (and (seq (:comment/body state))
                            (not= (:comment/body state) body))]
          (dom/button :.btn.btn-sm
            {:className (when (or (not state) can-submit?)
                          "btn-primary")
             :onClick
             #?(:clj  nil
                :cljs #(when can-submit?
                         (prim/transact! this
                           `[(mutations/submit-comment
                               {:article-id ~article-id
                                :diff       {[:comment/by-id ~(if (= :none id) (prim/tempid) id)]
                                             ~state}})])
                         (if (= :none id)
                           (prim/set-state! this {})
                           (set-editing-comment-id :none))))}
            (if (number? id)
              "Update Comment"
              "Post Comment")))))))

(def ui-comment-form (prim/factory CommentForm {:keyfn :comment/id}))

(defsc Article [this {:article/keys [id author-id slug title description body image comments]
                      :keys         [ph/article]}]
  {:ident         [:article/by-id :article/id]
   :initial-state (fn [params] #:article{:id :none :comments (prim/get-initial-state Comment {})})
   :query         [:article/id :article/author-id :article/slug :article/title :article/description
                   :article/body :article/image
                   {:article/comments (prim/get-query Comment)}
                   {:ph/article (prim/get-query ArticleMeta)}]}
  (let [delete-comment #?(:clj  nil
                          :cljs #(prim/transact! this
                                   `[(mutations/delete-comment {:article/id ~id :comment/id ~%})]))

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
            (ui-comment-form (prim/computed #:comment{:id :none} computed-map))
            (mapv #(ui-comment (prim/computed % computed-map))
              comments)))))))

(def ui-article (prim/factory Article {:keyfn :article/id}))

(defsc Tag [this {:tag/keys [tag]}]
  {:query [:tag/tag :tag/count]}
  (dom/a  :.tag-pill.tag-default {:href (str "/tag/" tag)} tag))

(def ui-tag (prim/factory Tag {:keyfn :tag/tag}))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p "Popular Tags")
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
  {:query         [[r/routers-table '_]]}
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
  [this {:keys [screen profile-to-view user-id]}]
  {:initial-state (fn [params]
                    {:screen          :screen.profile/liked-articles
                     :user-id         :guest
                     :profile-to-view (prim/get-initial-state LikedArticles #:user{:id :guest})})
   :ident         (fn [] [screen user-id])
   :query         [:screen :user-id {:profile-to-view (prim/get-query LikedArticles)}]}
  (ui-liked-articles profile-to-view))

(defsc OwnedArticles [this {:user/keys [id articles]}]
  {:ident         [:user/by-id :user/id]
   :query         [:user/id {:user/articles (prim/get-query ArticlePreview)}]}
  (article-list this articles "This user has no article!"))

(def ui-owned-articles (prim/factory OwnedArticles))

(defsc OwnedArticlesScreen
  [this {:keys [screen profile-to-view user-id]}]
  {:initial-state (fn [params]
                    {:screen          :screen.profile/owned-articles
                     :user-id         :guest
                     :profile-to-view (prim/get-initial-state OwnedArticles #:user {:id :guest})})
   :ident         (fn [] [screen user-id])
   :query         [:screen :user-id {:profile-to-view (prim/get-query OwnedArticles)}]}
  (ui-owned-articles profile-to-view))

(r/defrouter ProfileRouter :router/profile
  [:screen :user-id]
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
          (dom/h4 name)
          (dom/p bio)
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
           :article-id      :current-temp-article
           :article-to-edit [:article/by-id tempid]})))))

#?(:cljs
   (defmutation create-temp-article-if-not-found [_]
     (action [{:keys [state]}]
       (swap! state #(create-temp-article-if-not-found prim/tempid %)))))

(defn create-temp-comment-if-not-found
  [tempid-fn {:article/keys [id]} state]
  (if (tempid? (get-in state [:screen/article id :new-comment 1]))
    state
    (let [tempid              (tempid-fn)
          current-user        (:user/whoami state)
          [_ current-user-id] current-user

          new-item #:comment {:id     tempid
                              :body   ""
                              :author current-user}]
      (-> (assoc-in state [:comment/by-id tempid] new-item)
        #_(update-in [:article/by-id current-user-id :user/articles]
          (fnil conj []) [:article/by-id tempid])
        (assoc-in [:screen/article id :new-comment]
          [:comment/by-id tempid])
        (fs/add-form-config* CommentForm [:comment/by-id tempid])))))

#?(:cljs
   (defmutation create-temp-comment-if-not-found [article]
     (action [{:keys [state]}]
       (swap! state #(create-temp-comment-if-not-found prim/tempid article %)))
     (refresh [env] [:new-comment])))

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
                          :article-id      id
                          :article-to-edit [:article/by-id id]}))))
     (refresh [env] [:screen])))

(defsc TagItem [this {:tag/keys [tag]} {:keys [on-delete]}]
  {:query [:tag/tag]}
  (dom/span :.tag-pill.tag-default
    (dom/i :.ion-close-round
      {:onClick #?(:clj nil
                   :cljs #(on-delete tag))})
    tag))

(def ui-tag-item (prim/factory TagItem {:keyfn :tag/tag}))

(defsc ArticleEditor [this {:article/keys [id slug title description body tags] :as props}]
  {:initial-state (fn [{:article/keys [id]}] #:article{:id :none :body "" :title "" :description "" :slug ""})
   :query         [:article/id :article/slug  :article/title :article/description :article/body
                   {:article/tags [:tag/tag]}
                   fs/form-config-join]
   :ident         [:article/by-id :article/id]
   :form-fields   #{:article/slug  :article/title
                    :article/description :article/body}}
  (dom/div :.editor-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-10.offset-md-1.col-xs-12
          (dom/form
            (dom/fieldset
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Article Title",
                   :type        "text"
                   :name        "title"
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
                   :name        "description"
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
                   :name        "slug"
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
                   :name  "body"
                   :value body
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/body})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/body :event %))}))
              (dom/fieldset :.form-group
                (let [new-tag (prim/get-state this :new-tag)]
                  (dom/input :.form-control
                    {:placeholder "Enter tags"
                     :name        "tag"
                     :type        "text"
                     :onChange    #(prim/set-state! this {:new-tag (.. % -target -value)})
                     :onKeyDown   #(let [key (.-key %)]
                                     (when (and (= key "Enter") (seq new-tag))
                                       (prim/transact! this `[(mutations/add-tag {:article-id ~id :tag ~new-tag})])
                                       (prim/set-state! this {:new-tag ""})))
                     :value       (or new-tag "")}))
                (dom/div :.tag-list
                  (let [on-delete-tag #(prim/transact! this `[(mutations/remove-tag {:article-id ~id :tag ~%})])]
                    (map #(ui-tag-item (prim/computed % {:on-delete on-delete-tag})) tags))))
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
  {:query         [:user/image :user/name :user/bio :user/email]})

#?(:cljs
   (defmutation load-personal-feed [_]
     (action [{:keys [state] :as env}]
       (df/load-action env :articles/feed ArticlePreview))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation log-in [credentials]
     (action [{:keys [state] :as env}]
       (df/load-action env :user/whoami SettingsForm
         {:params  {:login credentials}
          :without #{:fulcro.ui.form-state/config :user/password}}))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation sign-up [new-user]
     (action [{:keys [state] :as env}]
       (df/load-action env :user/whoami SettingsForm
         {:params  {:sign-up new-user}
          :without #{:fulcro.ui.form-state/config :user/password}}))
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
               :user-id         id
               :profile-to-view [:user/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:screen :profile-to-view])))

#?(:cljs
   (defmutation load-article-to-screen [{:article/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:article/by-id id] Article)
       (swap! state
         #(update-in % [:screen/article id]
            (fn [x] (or x
                      {:screen          :screen/article
                       :article-id      id
                       :article-to-view [:article/by-id id]})))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:screen :article-to-view])))

(declare ArticleEditor)

#?(:cljs
   (defmutation load-article-to-editor [{:article/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:article/by-id id] ArticleEditor
         {:without #{:fulcro.ui.form-state/config}}))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:screen])))

#?(:cljs
   (defmutation load-liked-articles-to-screen [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (df/load-action env [:user/by-id id] LikedArticles)
       (swap! state
         #(-> %
            (assoc-in [:screen.profile/liked-articles id]
              {:screen          :screen.profile/liked-articles
               :user-id         id
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
               :user-id         id
               :profile-to-view [:user/by-id id]}))))
     (remote [env]
       (df/remote-load env))
     (refresh [env] [:profile-to-view])))

#?(:cljs
   (defmutation use-settings-as-form [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (swap! state #(-> %
                       (fs/add-form-config* SettingsForm [:user/by-id id])
                       (assoc-in [:root/settings-form :user] [:user/by-id id]))))))

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
          (dom/form
            (dom/fieldset
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

(defsc SettingScreen [this {user [:root/settings-form :user]}]
  {:initial-state (fn [params] {:screen             :screen/settings
                                :screen-id          :top})
   :query         [:screen :screen-id
                   {[:root/settings-form :user] (prim/get-query SettingsForm)}]}
  (ui-settings-form user))

(defsc EditorScreen [this {:keys [screen article-to-edit article-id]}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/editor
                                :article-id      :current-temp-article
                                :article-to-edit (prim/get-initial-state ArticleEditor #:article{:id :none})})
   :query         (fn [] [:screen :article-id
                          {:article-to-edit (prim/get-query ArticleEditor)}])}
  (ui-article-editor article-to-edit))

(defsc ProfileScreen [this {:keys  [screen profile-to-view user-id]
                            router [r/routers-table :router/profile]}]
  {:ident         (fn [] [screen user-id])
   :initial-state (fn [params] {:screen          :screen/profile
                                :user-id         :guest
                                :profile-to-view (prim/get-initial-state Profile #:user{:id :guest})
                                :router/profile  {}})
   :query         (fn [] [:screen :user-id
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
                                                       :route-params {:user-id ~user-id}})
                                          :profile-to-view])
                               :clj nil)}
                  "My Articles"))
              (dom/li :.nav-item
                (dom/div :.nav-link
                  {:onClick #?(:cljs #(prim/transact! this
                                        `[(load-liked-articles-to-screen {:user/id ~user-id})
                                          (r/route-to {:handler      :screen.profile/liked-articles
                                                       :route-params {:user-id ~user-id}})
                                          :profile-to-view])
                               :clj nil)}
                  "Favorited Articles"))))
          (ui-profile-router router))))))

(defsc ArticleScreen [this {:keys [screen article-id article-to-view]}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/article
                                :article-id      :none
                                :article-to-view (prim/get-initial-state Article #:article{:id :none})})
   :query         (fn [] [:screen :article-id
                          {:article-to-view (prim/get-query Article)}])}
  (ui-article article-to-view ))

(defsc SignUpForm [this {:user/keys [name email] :as props}]
  {:query         [:user/name :user/email fs/form-config-join]
   :initial-state (fn [params] #:user{:name "" :email ""})
   :ident         (fn [] [:root/sign-up-form :new-user])
   :form-fields   #{:user/name :user/email}}
  (let [{:user/keys [password] :as state} (prim/get-state this)]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (dom/div :.col-md-6.offset-md-3.col-xs-12
            (dom/h1 :.text-xs-center
              "Sign up")
            (dom/div :.text-xs-center
              {:href    "javascript:void(0)"
               :onClick #?(:clj nil
                           :cljs #(go-to-login this))}
              "Have an account?")
            #_
            (dom/ul :.error-messages
              (dom/li "That email is already taken") )
            (dom/form
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name"
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
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email"
                   :type        "text"
                   :value       email
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/email})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/email :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password"
                   :type        "password"
                   :value       (or password "")
                   :onChange
                   #?(:clj nil
                      :cljs #(prim/set-state! this {:user/password (.. % -target -value)}))}) )
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:onClick #?(:clj nil
                             :cljs #(prim/transact! this `[(sign-up ~(merge props state))]))}
                "Sign up"))))))))

(def ui-sign-up-form (prim/factory SignUpForm))

#?(:cljs
   (defmutation load-sign-up-form [_]
     (action [{:keys [state] :as env}]
       (swap! state
         #(fs/add-form-config* % SignUpForm [:root/sign-up-form :new-user])))
     (refresh [env] [:screen])))

(defsc SignUpScreen [this {user :new-user}]
  {:initial-state (fn [params] {:screen    :screen/sign-up
                                :screen-id :top
                                :new-user  #:user{:name "" :email ""}})
   :query         [:screen :screen-id
                   {:new-user (prim/get-query SignUpForm)}]}
  (ui-sign-up-form user))

(defsc LogInForm [this props]
  (let [{:user/keys [email password] :as credentials} (prim/get-state this)]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (dom/div :.col-md-6.offset-md-3.col-xs-12
            (dom/h1 :.text-xs-center
              "Log in")
            (dom/div :.text-xs-center
              {:href    "javascript:void(0)"
               :onClick #?(:clj nil
                           :cljs #(go-to-sign-up this))}
              "Don't have an account?")
            (dom/form
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email"
                   :type        "text"
                   :value       (or email "")
                   :onChange
                   #?(:clj  nil
                      :cljs #(prim/update-state! this assoc :user/email (.. % -target -value)))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password"
                   :type        "password"
                   :value       (or password "")
                   :onChange
                   #?(:clj  nil
                      :cljs #(prim/update-state! this assoc :user/password (.. % -target -value)))}) )
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:onClick #?(:clj nil
                             :cljs #(prim/transact! this `[(log-in ~credentials)]))}
                "Log in"))))))))

(def ui-log-in-form (prim/factory LogInForm))

(defsc LogInScreen [this props]
  {:initial-state (fn [params] {:screen    :screen/log-in
                                :screen-id :top})
   :query         [:screen :screen-id]}
  (ui-log-in-form {}))
