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
  (dom/a  :.tag-pill.tag-default {:href ""} tag))

(def ui-tag (prim/factory Tag {:keyfn :tag/tag}))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p "Popular Tags")
      (dom/div :.tag-list
        (mapv ui-tag tags)))))

(def ui-tags (prim/factory Tags))

(defn page-ident [feed page]
  [:article-list/page (other/ArticleListPageId. :home/feed feed page)])

(defsc FeedPage [this {:keys [feed page articles]}]
  {:ident         (fn [] (page-ident feed page))
   :initial-state (fn [params] {:feed     :global
                                :page     1
                                :articles []})
   :query         [:feed :page {:articles (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles
    (if (= feed :personal)
      "You have no article!"
      "No article!")))

(def ui-feed-page (prim/factory FeedPage))

(defsc Feed [this {:keys [screen feed current-page page pagination]}]
  {:initial-state (fn [params] {:screen       :screen/feed
                                :feed         :global
                                :pagination   #:pagination {:total 0 :last-id nil}
                                :current-page 1
                                :page         (prim/get-initial-state FeedPage {:feed :global :page 1})})
   :ident         (fn [] [screen feed])
   :query         [:screen :feed :current-page :pagination {:page (prim/get-query FeedPage)}]}
  (dom/div
    (ui-feed-page page)
    (let [{:pagination/keys [total last-id]} pagination
          items-per-page                     5
          go-to-page                         #(routes/go-to-feed this {:feed feed :page %})]
      (when total
        (map #(other/ui-page-item (prim/computed {:page %} {:go-to-page   go-to-page
                                                            :current-page current-page}))
          (range 1 (inc (util/page-number total items-per-page))))))))

(r/defrouter FeedsRouter :router/feeds
  (fn [this props] [(:screen props) (:feed props)])
  :screen/feed Feed)

(def ui-feeds-router (prim/factory FeedsRouter))

(defsc FeedSelector [this props]
  {:query [[r/routers-table '_]]}
  (let [[_screen current-feed] (r/current-route props :router/feeds)
        whoami             (prim/shared this :user/whoami)
        not-logged-in      (= :guest (:user/id whoami))]
    (dom/div :.feed-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (when (or (not not-logged-in)
                (and not-logged-in (= current-feed :personal)))
          (dom/li :.nav-item
            (dom/div :.nav-link
              {:className (if (= current-feed :personal) "active" "disabled")
               :onClick   #(if not-logged-in
                             (js/alert "You must log in first")
                             (routes/go-to-feed this {:feed :personal}))}
              "Your Feed")))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (if (= current-feed :global) "active" "disabled")
             :onClick   #(routes/go-to-feed this {:feed :global})}
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

(defn load-page-opts [{:keys [feed page] :or {page 1}}]
  (let [items-per-page 5]
    {:target (conj (page-ident feed page) :articles)
     :params {:offset   (* items-per-page (dec page))
              :limit    items-per-page
              :order-by [:article/id :desc]}}))

(defn load-pagination-opts [{:keys [feed]}]
  {:target [:screen/feed feed :pagination]})

(defmutation load-feed [{:keys [feed page] :or {page 1}}]
  (action [{:keys [state] :as env}]
    (swap! state
      #(-> (update-in % [:screen/feed feed]
             (fn [x] (or x {:screen     :screen/feed
                            :feed       feed
                            :pagination #:pagination {:total   0
                                                      :last-id nil}})))
         (update-in (page-ident feed page)
           (fn [x] (or x {:feed     feed
                          :page     page
                          :articles []})))
         (update-in [:screen/feed feed] merge
           {:current-page page
            :page         (page-ident feed page)})))
    (df/load-action env (if (= feed :personal) :articles/feed :articles/all)
      preview/ArticlePreview (load-page-opts {:feed feed :page page}))
    (df/load-action env (if (= feed :personal) :articles/count-feed :articles/count-all)
      other/Pagination (load-pagination-opts {:feed feed})))
  (remote [env]
    (df/remote-load env)))
