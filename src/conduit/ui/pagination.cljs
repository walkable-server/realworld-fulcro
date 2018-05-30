(ns conduit.ui.pagination
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.routing :as r]
   [conduit.util :as util]))

(defsc Pagination [this props]
  {:query [:pagination/total :pagination/last-id]})

(defsc PageButton [this {:keys [page-number]} {:keys [go-to-page current-page]}]
  (dom/li :.page-item {:className (when (= current-page page-number)
                                    "active")}
    (dom/a :.page-link {:onClick #(go-to-page page-number)}
      page-number)))

(def ui-page-button (prim/factory PageButton {:keyfn :page-number}))

(defrecord PaginatedListId [list-type list-id])

(defn paginated-list-ident [list-type list-id]
  [:screen/paginated-list (PaginatedListId. list-type list-id)])

(defrecord PageId [list-type list-id page])

(defn page-ident [list-type list-id page]
  [:pagination/page (PageId. list-type list-id page)])

(defsc Page [this {:keys [list-type list-id page articles]}]
  {:ident         (fn [] (page-ident list-type list-id page))
   :initial-state (fn [params] {:list-type :articles/by-feed
                                :list-id   :global
                                :page      1
                                :articles  []})
   :query         [:list-type :list-id :page {:articles (prim/get-query preview/ArticlePreview)}]}
  (preview/article-list this articles
    (cond
      (and (= list-type :articles/by-feed) (= list-id :personal))
      "You have no article!"

      :default
      "No article!") ))

(def ui-page (prim/factory Page))

(defsc PaginatedList [this {:keys [list-type list-id pagination current-page page-item]} {:keys [go-to-page]}]
  {:initial-state (fn [params] {:list-type    :articles/by-feed
                                :list-id      :global
                                :pagination   #:pagination {:total 0 :last-id nil}
                                :current-page 1
                                :page-item    (prim/get-initial-state Page {})})
   :ident         (fn [] (paginated-list-ident list-type list-id))
   :query         [:list-type :list-id :pagination :current-page {:page-item (prim/get-query Page)}]}
  (dom/div
    (ui-page page-item)
    (let [{:pagination/keys [total last-id]} pagination
          items-per-page                     5]
      (when total
        (map #(ui-page-button (prim/computed {:page-number %}
                                {:go-to-page   go-to-page :current-page current-page}))
          (range 1 (inc (util/page-number total items-per-page))))))))

(r/defrouter PaginatedListRouter :router/paginated-list
  (fn [this {:keys [list-type list-id] :or {list-type :articles/by-feed :list-id :global}}]
    (paginated-list-ident list-type list-id))
  :screen/paginated-list PaginatedList)

(def ui-paginated-list-router (prim/factory PaginatedListRouter))

;; load helpers
(defn load-page-opts [{:keys [list-type list-id page] :or {page 1}}]
   (let [items-per-page 5]
    {:target (conj (page-ident list-type list-id page) :articles)
     :params {:offset   (* items-per-page (dec page))
              :limit    items-per-page
              :order-by [:article/id :desc]}}))

(defn load-pagination-opts [{:keys [list-type list-id]}]
  {:target (conj (paginated-list-ident list-type list-id) :pagination)})

;; mutation helpers

(defn initialize-paginated-list [state {:keys [list-type list-id]}]
  (update-in state (paginated-list-ident list-type list-id)
    (fn [x] (or x {:list-type  list-type
                   :list-id    list-id
                   :pagination #:pagination {:total   0
                                             :last-id nil}}))))

(defn initialize-page [state {:keys [list-type list-id page]}]
  (update-in state (page-ident list-type list-id page)
    (fn [x] (or x {:list-type list-type
                   :list-id   list-id
                   :page      page
                   :articles  []}))))

(defn set-current-page [state {:keys [list-type list-id page]}]
  (update-in state (paginated-list-ident list-type list-id)
    merge {:current-page page
           :page-item    (page-ident list-type list-id page)}))

(defn navigate-to [state paginated-list]
  (-> (initialize-paginated-list state paginated-list)
    (initialize-page paginated-list)
    (set-current-page paginated-list)))

(defn set-paginated-list [state ident {:keys [list-type list-id]}]
  (assoc-in state (conj ident :paginated-list-router)
    (paginated-list-ident list-type list-id)))

(defmethod r/coerce-param :param/paginated-list
  [k {:keys [list-type list-id]}]
  (PaginatedListId. list-type list-id))

(defn current-paginated-list [routers-table]
  (let [[_ paginated-list] (r/current-route routers-table :router/paginated-list)]
    paginated-list))
