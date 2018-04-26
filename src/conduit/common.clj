(ns conduit.common
  (:require [cheshire.core :refer [generate-string]]))

(def profile-query
  [:user/id :user/username :user/bio :user/image
   {:user/articles-count [:agg/count]}
   {:user/followed-by-me? [:agg/count]}
   {:user/followed-by [:user/id :user/username]}])

(defn json-key
  [k]
  (case k
    :user/followed-by-me?
    "following"

    :article/liked-by-me?
    "favorited"

    :article/liked-by-count
    "favoritesCount"

    :article/tags
    "tagList"

    (:article/created-at :comment/created-at)
    "createdAt"

    (:article/updated-at :comment/updated-at)
    "updatedAt"

    :user/articles-count
    "articlesCount"

    (name k)))

(defn clj->json
  ([data]
   (generate-string data {:key-fn json-key}))
  ([data k]
   (generate-string {k data} {:key-fn json-key})))
