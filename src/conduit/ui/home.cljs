(ns conduit.ui.home
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.routing :as r]
   [fulcro.client.dom :as dom]
   [conduit.ui.routes :as routes]))

(defsc NavBar [this props]
  {:query [[r/routers-table '_]]}
  (let [[current-screen _]         (r/current-route props :router/top)
        whoami                     (prim/shared this :user/whoami)
        {current-user-id :user/id} whoami
        logged-in?                 (number? current-user-id)]
    (dom/nav :.navbar.navbar-light
      (dom/div :.container
        (dom/div :.navbar-brand
          "conduit")
        (dom/ul :.nav.navbar-nav.pull-xs-right
          (dom/li :.nav-item
            (dom/div :.nav-link
              {:className (when (= current-screen :screen/home) "active")
               :onClick   #(prim/transact! this `[(r/route-to {:handler :screen/home})])}
              "Home") )
          (when logged-in?
            (dom/li :.nav-item
              (dom/a :.nav-link
                {:className (when (= current-screen :screen/editor) "active")
                 :onClick   #(routes/go-to-new-article this)}
                (dom/i :.ion-compose)
                "New Post")))
          (when logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/settings) "active")
                 :onClick   #(routes/go-to-settings this {:user/id current-user-id})}
                (dom/i :.ion-gear-a)
                "Settings")))
          (when-not logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/log-in) "active")
                 :onClick   #(routes/go-to-log-in this)}
                "Login")))

          (when-not logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:className (when (= current-screen :screen/sign-up) "active")
                 :onClick   #(routes/go-to-sign-up this)}
                "Sign up")))

          (when logged-in?
            (dom/li :.nav-item
              (dom/div :.nav-link
                {:onClick #(routes/log-out this)}
                "Log out"))))))))

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

(defsc PersonalFeed [this {:keys [screen articles]}]
  {:initial-state {:screen :screen.feed/personal
                   :articles []}
   :ident         (fn [] [screen :top])
   :query         [:screen {:articles (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles "You have no article!"))

(defsc GlobalFeed [this {:keys [screen articles]}]
  {:initial-state {:screen :screen.feed/global}
   :ident         (fn [] [screen :top])
   :query         [:screen {:articles (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles "No article!"))

(r/defrouter FeedsRouter :router/feeds
  (fn [this props] [(:screen props) :top])
  :screen.feed/global   GlobalFeed
  :screen.feed/personal PersonalFeed)

(def ui-feeds-router (prim/factory FeedsRouter))

(defsc FeedSelector [this props]
  {:query [[r/routers-table '_]]}
  (let [[current-screen _] (r/current-route props :router/feeds)
        whoami             (prim/shared this :user/whoami)
        not-logged-in      (= :guest (:user/id whoami))]
    (dom/div :.feed-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (when (or (not not-logged-in)
                (and not-logged-in (= current-screen :screen.feed/personal)))
          (dom/li :.nav-item
            (dom/div :.nav-link
              {:className (if (= current-screen :screen.feed/personal) "active" "disabled")
               :onClick   #(if not-logged-in
                             (js/alert "You must log in first")
                             (routes/go-to-personal-feed this))}
              "Your Feed")))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (if (= current-screen :screen.feed/global) "active" "disabled")
             :onClick   #(routes/go-to-global-feed this)}
            "Global Feed"))))))

(def ui-feed-selector (prim/factory FeedSelector))

(defsc Home [this {tags          :tags/all
                   router        :router/feeds
                   feed-selector :feed-selector}]
  {:initial-state (fn [params] {:screen        :screen/home
                                :screen-id     :top
                                :feed-selector {}
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

;; mutations

(defmutation load-personal-feed [_]
  (action [{:keys [state] :as env}]
    (df/load-action env :articles/feed preview/ArticlePreview {:target [:screen.feed/personal :top :articles]}))
  (remote [env]
    (df/remote-load env)))

(defmutation load-global-feed [_]
  (action [{:keys [state] :as env}]
    (df/load-action env :articles/all preview/ArticlePreview {:target [:screen.feed/global :top :articles]}))
  (remote [env]
    (df/remote-load env)))
