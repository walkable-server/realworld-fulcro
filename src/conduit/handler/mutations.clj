(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [clojure.set :refer [rename-keys]]
            [conduit.boundary.article :as article]
            [walkable.sql-query-builder :as sqb]
            [conduit.util :as util]
            [duct.logger :refer [log]]
            [fulcro.server :as server :refer [defmutation]]))

(def remove-user-namespace
  (util/remove-namespace "user" [:username :name :email :bio :image :password]))

(def remove-article-namespace
  (util/remove-namespace "article" [:title :body :slug :description]))

(defmutation submit-article [diff]
  (action [{:keys [duct/logger] ::sqb/keys [sql-db] :app/keys [current-user]}]
    ;;(log logger :info :article diff)
    (if current-user
      (article/update-article sql-db current-user
        (second (util/get-ident diff)) ;; article-id
        (-> (util/get-item diff)
          (rename-keys remove-article-namespace)))
      {})))

(defmutation submit-settings [diff]
  (action [{:keys [duct/logger] ::sqb/keys [sql-db] :app/keys [current-user]}]
    ;;(log logger :info :settings diff)
    (if current-user
      (user/update-user sql-db current-user
        (-> (util/get-item diff) (rename-keys remove-user-namespace)))
      {})))
