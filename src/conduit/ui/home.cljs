(ns conduit.ui.home
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.pagination :as pagination]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.routing :as r]
   [fulcro.client.dom :as dom]
   [conduit.ui.routes :as routes]
   [conduit.util :as util]))

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
              {:className (when (= current-screen :screen/feed) "active")
               :onClick   #(prim/transact! this
                             `[(r/route-to {:handler :screen/feed
                                            :params  {:feed-id ~(if logged-in? :personal :global)}})])}
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
  (dom/a  :.tag-pill.tag-default {:href ""} tag))

(def ui-tag (prim/factory Tag {:keyfn :tag/tag}))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p "Popular Tags")
      (dom/div :.tag-list
        (mapv ui-tag tags)))))

(def ui-tags (prim/factory Tags))

(defsc FeedSelector [this props {:keys [feed-id]}]
  {:query []}
  (let [whoami        (prim/shared this :user/whoami)
        not-logged-in (= :guest (:user/id whoami))]
    (dom/div :.feed-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (when (or (not not-logged-in)
                (and not-logged-in (= feed-id :personal)))
          (dom/li :.nav-item
            (dom/div :.nav-link
              {:className (if (= feed-id :personal) "active" "disabled")
               :onClick   #(if not-logged-in
                             (js/alert "You must log in first")
                             (routes/go-to-feed this :personal))}
              "Your Feed")))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (if (= feed-id :global) "active" "disabled")
             :onClick   #(routes/go-to-feed this :global)}
            "Global Feed"))))))

(def ui-feed-selector (prim/factory FeedSelector))

(defsc HomeScreen [this {:keys [feed-id current-page] tags :tags/all}]
  {:ident [:screen/feed :feed-id]
   :initial-state (fn [params] {:screen       :screen/feed
                                :feed-id      :global
                                :current-page (prim/get-initial-state pagination/Page {})})

   :query [:screen :feed-id
           {:current-page (prim/get-query pagination/Page)}
           {[:tags/all '_] (prim/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feed-selector (prim/computed {} {:feed-id feed-id}))
          (pagination/ui-page (prim/computed current-page {:load-page #(prim/transact! this `[(load-feed ~%)])})))
        (ui-tags tags)))))

;; mutations
(defmutation load-feed [{:pagination/keys [list-id] :as feed}]
  (action [{:keys [state] :as env}]
    (df/load-action env :paginated-list/articles
      pagination/Page {:params feed
                       :target [:screen/feed list-id :current-page]}))
  (remote [env]
    (df/remote-load env))
  (refresh [env]
    [:pagination/list-type :current-page]))
