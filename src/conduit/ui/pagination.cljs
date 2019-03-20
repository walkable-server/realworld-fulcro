(ns conduit.ui.pagination
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.routing :as r]
   [conduit.util :as util]))

(defn list-ident-value
  [{:app.article.list/keys [list-type list-id direction size]
    :or                    {list-type :app.articles.list/feed
                            list-id   :global
                            direction :forward
                            size      5}}]
  #:app.articles.list{:list-type list-type
                      :list-id   list-id
                      :direction direction
                      :size      size})

(defn list-ident
  [props]
  [:app.articles/list (list-ident-value props)])

(defn page-ident
  [{:app.article.list.page/keys [start end] :as props}]
  [:app.articles.list/page
   (merge (list-ident-value props)
     #:app.articles.list.page{:start start :end end})])

(defn has-previous-page?
  [article-list {:app.article.list.page/keys [start]}]
  (and (not (nil? start))
    (= start (:app.articles.list/first-item-id article-list))))

(defn has-next-page?
  [article-list {:app.article.list.page/keys [end]}]
  (and (not (nil? end))
    (= start (:app.articles.list/last-item-id article-list))))

(defsc Page
  [this {:app.articles.page/keys [start end items]
         :app.articles.list/keys [list-type list-id]
         :as                     props}
   {:keys [article-list]}]
  {:ident         (fn [] (page-ident props))
   :initial-state (fn [params]
                    (merge (list-ident-value params)
                      #:app.articles.by-page{:start :none
                                             :end   :none
                                             :items (prim/get-initial-state preview/ArticlePreview {})}
                      params))
   :query         [:app.articles.list/list-type
                   :app.articles.list/list-id
                   :app.articles.list/direction
                   :app.articles.list/size
                   :app.articles.list.page/start
                   :app.articles.list.page/end
                   {:app.articles.list.page/items (prim/get-query preview/ArticlePreview)}]}
  (dom/div
    (preview/article-list this
      (if (= :forward direction) items (reverse items))
      (cond
        (and (= list-type :app.articles.list/on-feed) (= list-id :personal))
        "You have no article! Try to follow more people."

        :default
        "No article!"))
    (dom/div
      (dom/button :.btn.btn-sm
        (if (has-previous-page? article-list props)
          {:onClick #(previous-page props) :className "action-btn btn-outline-primary"}
          {:className "btn-outline-secondary"})
        "Previous")
      (dom/button :.btn.btn-sm
        (if (has-next-page? article-list props)
          {:onClick #(next-page props) :className "action-btn btn-outline-primary"}
          {:className "btn-outline-secondary"})
        "Next"))))

(def ui-page (prim/factory Page))

(defsc List
  [this {:app.articles.list/keys [list-type list-id direction size current-page]
         :as                     props}]
  {:ident         (fn [] (list-ident props))
   :initial-state (fn [params]
                    (merge (list-ident-value params)
                      #:app.articles.list{:first-item-id nil
                                          :last-item-id  nil
                                          :total-items   0
                                          :streak        []
                                          :current-page  (prim/get-initial-state Page params)}
                      params))
   :query         [:app.articles.list/list-type
                   :app.articles.list/list-id
                   :app.articles.list/direction
                   :app.articles.list/size
                   :app.articles.list/first-item-id
                   :app.articles.list/last-item-id
                   :app.articles.list/total-items
                   {:app.articles.list/current-page (prim/get-query Page)}]}
  (if current-page
    (dom/div "No article")
    (ui-page (prim/computed current-page {:article-list props}))))

(def ui-list (prim/factory List))

(r/defsc-router PageRouter [this props]
  {:router-id      :router/page
   :ident          (fn [] (page-ident props))
   :default-route  Page
   :router-targets {:app.articles.list/page Page}}
  (dom/div "Bad route!"))

(def ui-page-router (prim/factory PageRouter))
