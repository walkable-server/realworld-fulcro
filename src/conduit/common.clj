(ns conduit.common
  (:require [cheshire.core :refer [generate-string]]))

(def profile-query
  [:user/id :user/username :user/bio :user/image
   {:user/followed-by-me? [:agg/count]}
   {:user/followed-by [:user/id :user/username]}])

(def articles-query
  [:article/slug :article/title :article/description :article/body
   :article/created-at :article/updated-at
   {:article/liked-by-count [:agg/count]}
   {:article/liked-by [:user/username]}
   {:article/liked-by-me? [:agg/count]}
   {:article/tags [:tag/tag]}
   {:article/comments [:comment/id :comment/created-at :comment/updated-at :comment/body]}
   {:article/author profile-query}])

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

    :articles/all
    "articles"

    (name k)))

(defn clj->json
  ([data]
   (generate-string data {:key-fn json-key})))

(defn with-articles-count [{:articles/keys [all] :as data}]
  (assoc data "articlesCount" (count all)))
