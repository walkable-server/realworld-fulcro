(ns conduit.ui.other
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.dom :as dom]))

(defsc Pagination [this props]
  {:query [:pagination/total
           :pagination/last-id]})

(defsc PageItem [this {:keys [page]} {:keys [go-to-page current-page]}]
  (dom/li :.page-item {:className (when (= current-page page)
                                    "active")}
    (dom/a :.page-link {:onClick #(go-to-page page)}
      page)))

(def ui-page-item (prim/factory PageItem {:keyfn :page}))

(defrecord ArticleListPageId [list-type list-id page])

(defsc UserTinyPreview [this props]
  {:query [:user/id :user/username :user/name :user/image]
   :initial-state (fn [params] #:user{:id :guest})
   :ident [:user/by-id :user/id]})

(defsc UserPreview [this props]
  {:query [:user/id :user/image :user/username :user/name :user/followed-by-me :user/followed-by-count]
   :initial-state (fn [params] #:user{:id :guest})
   :ident [:user/by-id :user/id]})

(defn focus-field [component ref-name]
  (let [input-field        (dom/node component ref-name)
        input-field-length (.. input-field -value -length)]
    (.focus input-field)
    (.setSelectionRange input-field input-field-length input-field-length)))

(defn js-date->string [date]
  (when (instance? js/Date date)
    (.toDateString date)))
