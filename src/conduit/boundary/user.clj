(ns conduit.boundary.user
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol User
  (create-user [db user])
  (find-login [db email password])
  (follow [db follower-id followee-id])
  (unfollow [db follower-id followee-id]))

(extend-protocol User
  duct.database.sql.Boundary
  (create-user [{db :spec} {:keys [password] :as user}]
    (let [pw-hash (hashers/derive password)
          results (jdbc/insert! db "\"user\""
                    (-> user (select-keys [:username :email :bio :image])
                      (assoc :password pw-hash)))]
      (-> results first (dissoc :password))))
  (find-login [{db :spec} email password]
    (when-let [user (first (jdbc/find-by-keys db "\"user\"" {:email email}))]
      (when (hashers/check password (:password user))
        (dissoc user :password)))))
