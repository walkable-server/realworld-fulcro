(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [conduit.boundary.article :as article]
            [clojure.set :refer [rename-keys]]
            [walkable.sql-query-builder :as sqb]
            [conduit.util :as util]
            [duct.logger :refer [log]]
            [fulcro.tempid :refer [tempid?]]
            [fulcro.client.primitives :as prim]
            [fulcro.server :as server :refer [defmutation]]))

(def remove-user-namespace
  (util/remove-namespace "user" [:username :name :email :bio :image :password]))

(def remove-article-namespace
  (util/remove-namespace "article" [:title :body :slug :description]))

(def remove-comment-namespace
  (util/remove-namespace "comment" [:body]))

(defmutation submit-article [diff]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    ;;(log logger :info :article diff)
    (if current-user
      (let [[_ article-id] (util/get-ident diff)
            article        (-> (util/get-item diff)
                             (rename-keys remove-article-namespace))]
        (if (tempid? article-id)
          (let [new-id (article/create-article db current-user article)]
            {::prim/tempids {article-id new-id}})
          (article/update-article db current-user article-id article)))
      {})))

(defmutation submit-comment [{:keys [article-id diff]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if current-user
      (let [[_ comment-id] (util/get-ident diff)
            comment-item   (-> (util/get-item diff)
                             (rename-keys remove-comment-namespace))]
        (if (tempid? comment-id)
          (let [new-id (article/create-comment db current-user article-id comment-item)]
            {::prim/tempids {comment-id new-id}})
          (article/update-comment db current-user comment-id comment-item)))
      {})))

(defmutation submit-settings [diff]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    ;;(log logger :info :settings diff)
    (if current-user
      (user/update-user db current-user
        (-> (util/get-item diff) (rename-keys remove-user-namespace)))
      {})))

(defmutation delete-article [{:article/keys [id]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if current-user
      (article/destroy-article db current-user id)
      {})))

(defmutation follow [{:user/keys [id]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if (and current-user (not= current-user id))
      (user/follow db current-user id)
      {})))

(defmutation unfollow [{:user/keys [id]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if current-user
      (user/unfollow db current-user id)
      {})))

(defmutation like [{:article/keys [id]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if current-user
      (article/like db current-user id)
      {})))

(defmutation unlike [{:article/keys [id]}]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    (if current-user
      (article/unlike db current-user id)
      {})))
