(ns conduit.ui.pagination
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.routing :as r]
   [conduit.util :as util]))

(defsc Page
  [this {:app.articles.page/keys [start end items]
         :app.articles.list/keys [list-type list-id]
         :as                     props}
   {:keys [article-list]}]
  {:ident         (fn [] (page-ident props))
   :initial-state (fn [params]
                    (merge #:pagination{:list-type   :articles/by-feed
                                        :list-id     :global
                                        :size        5
                                        :start       :empty
                                        :previous-id nil
                                        :next-id     nil
                                        :items       (prim/get-initial-state preview/ArticlePreview {})}
                      params))
   :query         [:pagination/list-type :pagination/list-id :pagination/size
                   :pagination/start :pagination/end
                   :pagination/next-id  :pagination/previous-id
                   {:pagination/items (prim/get-query preview/ArticlePreview)}]}
  (dom/div
    (preview/article-list this
      (if (number? end) (reverse items) items)
      (cond
        (and (= list-type :articles/by-feed) (= list-id :personal))
        "You have no article!"

        :default
        "No article!"))
    (dom/div
      (dom/button :.btn.btn-sm
        (if previous-id
          {:onClick #(load-page previous-id) :className "action-btn btn-outline-primary"}
          {:className "btn-outline-secondary"})
        "Previous")
      (dom/button :.btn.btn-sm
        (if next-id
          {:onClick #(load-page next-id) :className "action-btn btn-outline-primary"}
          {:className "btn-outline-secondary"})
        "Next"))))

(def ui-page (prim/factory Page))

(r/defsc-router PageRouter [this props]
  {:router-id      :router/page
   :ident          (fn [] (page-ident props))
   :default-route  Page
   :router-targets {:pagination/page Page}}
  (dom/div "Bad route!"))

(def ui-page-router (prim/factory PageRouter))
