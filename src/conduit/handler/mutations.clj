(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [clojure.set :refer [rename-keys]]
            [conduit.boundary.article :as article]
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

(defmutation submit-settings [diff]
  (action [{:keys [duct/logger] :app/keys [db current-user]}]
    ;;(log logger :info :settings diff)
    (if current-user
      (user/update-user db current-user
        (-> (util/get-item diff) (rename-keys remove-user-namespace)))
      {})))
