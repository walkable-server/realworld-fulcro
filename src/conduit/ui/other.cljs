(ns conduit.ui.other
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc UserTinyPreview [this props]
  {:query [:user/id :user/username :user/name :user/image]
   :initial-state (fn [params] #:user{:id :guest})
   :ident [:user/by-id :user/id]})

(defsc UserPreview [this props]
  {:query [:user/id :user/image :user/username :user/name :user/followed-by-me :user/followed-by-count]
   :initial-state (fn [params] #:user{:id :guest})
   :ident [:user/by-id :user/id]})
