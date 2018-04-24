(ns conduit.boundary.user
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol User
  (create-user [db email username password])
  (find-user [db username password]))

(extend-protocol User
  duct.database.sql.Boundary
  (create-user [{db :spec} email username password]
    (let [pw-hash (hashers/derive password)
          results (jdbc/insert! db "\"user\"" {:username username, :email email, :password pw-hash})]
      (-> results ffirst val)))
  (find-user [{db :spec} username password]
    (when-let [user (first (jdbc/find-by-keys db "\"user\"" {:username username}))]
      (when (hashers/check password (:password user))
        (dissoc user :password)))))
