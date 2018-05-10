(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [clojure.set :refer [rename-keys]]
            [conduit.boundary.article :as article]
            [walkable.sql-query-builder :as sqb]
            [duct.logger :refer [log]]
            [fulcro.server :as server :refer [defmutation]]))

(defmutation submit-article [{id :article/id article :diff}]
  (action [{:keys [duct/logger] ::sqb/keys [sql-db] :app/keys [current-user]}]
    ;;(log logger :info :article article)
    (if current-user
      (article/update-article sql-db current-user id
        (-> (get article [:article/by-id id])
          (rename-keys {:article/title :title :article/body :body
                        :article/slug :slug :article/description :description})))
      {})))
