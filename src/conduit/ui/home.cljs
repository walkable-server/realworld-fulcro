(ns conduit.ui.home
  (:require
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.article-preview :as preview]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [conduit.session :as session :refer [ui-current-user]]
   [com.fulcrologic.fulcro.dom :as dom]))

;; FIXME
(defn match-route? [current-route route])

(defn ui-nav-bar [{:keys [logged-in? current-route current-user]}]
  (dom/nav :.navbar.navbar-light
    (dom/div :.container
      (dom/div :.navbar-brand
        "conduit")
      (dom/ul :.nav.navbar-nav.pull-xs-right
        (dom/li :.nav-item
          (dom/a :.nav-link
            {:className (when-not (match-route? current-route #{:screen/editor :screen/log-in :screen/sign-up})
                          "active")
             :href      "/home"}
            "Home"))
        (when logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/new) "active")
               :href      "/new"}
              (dom/i :.ion-compose)
              "New Post")))
        (when logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/settings) "active")
               :href "/settings"}
              (dom/i :.ion-gear-a)
              "Settings")))
        (when-not logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/log-in) "active")
               :href      "/login"}
              "Login")))

        (when-not logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/sign-up) "active")
               :href      "/sign-up"}
              "Sign up")))

        (ui-current-user current-user)))))

(defn ui-footer [{}]
  (dom/footer
    (dom/div :.container
      (dom/div :.logo-font "conduit")
      (dom/span :.attribution
        "An interactive learning project from "
        (dom/a {:href "https://thinkster.io"} "Thinkster")
        ". Code &amp; design licensed under MIT."))))

(defn ui-banner []
  (dom/div :.banner
    (dom/div :.container
      (dom/h1 :.logo-font "conduit")
      (dom/p "A place to show off your tech stack."))))

(defsc Tag [this {:tag/keys [tag]}]
  {:query [:tag/tag :tag/count]}
  (dom/a :.tag-pill.tag-default
    {:href (str "/tag/" tag)} tag))

(def ui-tag (comp/factory Tag {:keyfn :tag/tag}))

(defn ui-tags [tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p "Popular Tags")
      (dom/div :.tag-list
        (map ui-tag tags)))))

(defn ui-feed-selector
  [this {:ui/keys [tag personal? global?]}]
  (let [not-logged-in false]
    (dom/div :.feed-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (when (or (not not-logged-in)
                (and not-logged-in personal?))
          (dom/li :.nav-item
            (dom/a :.nav-link
              (merge {:className (if personal? "active" "disabled")
                      :href      "/personal"}
                (when not-logged-in
                  {:onClick #(js/alert "You must log in first")}))
              "Your Feed")))
        (dom/li :.nav-item
          (dom/a :.nav-link
            {:className (if global? "active" "disabled")
             :href "/home"}
            "Global Feed"))
        (when tag
          (dom/li :.nav-item
            (dom/div :.nav-link.active
              "Tagged with `" tag "`")))))))

(defmutation load-global-feed [_]
  (action [{:keys [app]}]
    (df/load! app :app.global-feed/articles preview/ArticlePreview
      {:target [:component/id :global-feed :articles]})
    (df/load! app :app.tags/top-list Tag)
    (dr/target-ready! app [:component/id :global-feed])))

(defmutation load-personal-feed [_]
  (action [{:keys [app]}]
    (df/load! app :app.personal-feed/articles preview/ArticlePreview
      {:target [:component/id :personal-feed :articles]})
    (df/load! app :app.tags/top-list Tag)
    (dr/target-ready! app [:component/id :personal-feed])))

(defmutation load-tag-articles [{tag :tag/tag}]
  (action [{:keys [app]}]
    (df/load! app :app.global-feed/articles preview/ArticlePreview
      {:target [:articles/by-tag tag :articles]
       :params {:filters {:article/tags [:= tag :tag/tag]}}})
    (df/load! app :app.tags/top-list Tag)
    (dr/target-ready! app [:articles/by-tag tag])))

(defsc GlobalFeed [this {:keys [articles] tags :app.tags/top-list}]
  {:ident         (fn [_] [:component/id :global-feed])
   :route-segment ["home"]
   :will-enter
   (fn [app _route-params]
     (comp/transact! app [(mutations/ensure-ident {:ident [:component/id :global-feed]})])
     (dr/route-deferred [:component/id :global-feed]
       #(comp/transact! app [(load-global-feed {})])))
   :initial-state (fn [_params]
                    {:articles (comp/get-initial-state preview/ArticlePreview {})})

   :query [{:articles (comp/get-query preview/ArticlePreview)}
           {[:app.tags/top-list '_] (comp/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feed-selector this {:ui/global? true})
          (preview/ui-article-list this
            {:ui/articles articles
             :ui/empty-message "No articles"}))
        (ui-tags tags)))))

(defsc PersonalFeed [this {:keys [articles] tags :app.tags/top-list}]
  {:ident         (fn [_] [:component/id :personal-feed])
   :route-segment ["personal"]
   :will-enter
   (fn [app _route-params]
     (comp/transact! app [(mutations/ensure-ident {:ident [:component/id :personal-feed]})])
     (dr/route-deferred [:component/id :personal-feed]
       #(comp/transact! app [(load-personal-feed {})])))
   :initial-state (fn [_params]
                    {:articles (comp/get-initial-state preview/ArticlePreview {})})

   :query [{:articles (comp/get-query preview/ArticlePreview)}
           {[:app.tags/top-list '_] (comp/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feed-selector this {:ui/personal? true})
          (preview/ui-article-list this
            {:ui/articles articles
             :ui/empty-message "No articles. Try to follow more people."}))
        (ui-tags tags)))))

(defsc ArticleByTag
  [this {:keys [articles] tags :app.tags/top-list tag :tag/tag}]
  {:ident         [:articles/by-tag :tag/tag]
   :route-segment ["tag" :tag/tag]
   :will-enter
   (fn [app {:tag/keys [tag]}]
     (comp/transact! app [(mutations/ensure-ident {:ident [:articles/by-tag tag]
                                                   :state {:tag/tag tag}})])
     (dr/route-deferred [:articles/by-tag tag]
       #(comp/transact! app [(load-tag-articles {:tag/tag tag})])))
   :initial-state (fn [_params]
                    {:tag/tag  "fulcro"
                     :articles (comp/get-initial-state preview/ArticlePreview {})})

   :query [:tag/tag
           {:articles (comp/get-query preview/ArticlePreview)}
           {[:app.tags/top-list '_] (comp/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (ui-feed-selector this {:ui/tag tag})
          (preview/ui-article-list this
            {:ui/articles articles
             :ui/empty-message (str "No articles tagged with `" tag "`")}))
        (ui-tags tags)))))
