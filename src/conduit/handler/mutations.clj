(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [walkable.sql-query-builder :as sqb]
            [duct.logger :refer [log]]
            [fulcro.server :as server :refer [defmutation]]))

(defn token [user jwt-secret]
  {:token (jwt/sign {:user/id (:id user)} jwt-secret)})

(defmutation login [{:keys [email password]}]
  (action [{:keys [duct/logger app/jwt-secret] ::sqb/keys [sql-db] :as env}]
    (if-let [user (user/find-login sql-db email password)]
      (token user jwt-secret)
      {})))
