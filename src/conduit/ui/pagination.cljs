(ns conduit.ui.pagination
  (:refer-clojure :exclude [List])
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.routing :as r]
   [conduit.util :as util]))

(defn list-ident-value
  [{:app.articles.list/keys [list-type list-id direction size]
    :or                     {list-type :app.articles/on-feed
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
  [{:app.articles.list.page/keys [start end] :as props}]
  [:app.articles.list/page
   (merge (list-ident-value props)
     #:app.articles.list.page{:start start :end end})])

(defn has-previous-page?
  [{:app.articles.list/keys [current-page first-item-id] :as article-list}]
  (and (not (nil? first-item-id))
    (= first-item-id (:app.articles.list.page/start current-page))))

(defn has-next-page?
  [{:app.articles.list/keys [current-page last-item-id] :as article-list}]
  (and (not (nil? last-item-id))
    (= last-item-id (:app.articles.list.page/end current-page))))

(defsc Page
  [this {:app.articles.list/keys      [list-type list-id direction]
         :app.articles.list.page/keys [start end items]
         :as                          props}
   {:keys [article-list]}]
  {:ident         (fn [] (page-ident props))
   :initial-state (fn [params]
                    (merge (list-ident-value params)
                      #:app.articles.list.page{:start :none
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
  (preview/article-list this
    (if (= :forward direction) items (reverse items))
    (cond
      (and (= list-type :app.articles/on-feed) (= list-id :personal))
      "You have no article! Try to follow more people."

      :default
      "No article!")))

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
    (dom/div
      (ui-page current-page)
      (dom/div
        (dom/button :.btn.btn-sm
          (if (has-previous-page? props)
            {:onClick #(previous-page props) :className "action-btn btn-outline-primary"}
            {:className "btn-outline-secondary"})
          "Previous")
        (dom/button :.btn.btn-sm
          (if (has-next-page? props)
            {:onClick #(next-page props) :className "action-btn btn-outline-primary"}
            {:className "btn-outline-secondary"})
          "Next")))))

(def ui-list (prim/factory List))

(r/defsc-router PageRouter [this props]
  {:router-id      :router/page
   :ident          (fn [] (page-ident props))
   :default-route  Page
   :router-targets {:app.articles.list/page Page}}
  (dom/div "Bad route!"))

(def ui-page-router (prim/factory PageRouter))
