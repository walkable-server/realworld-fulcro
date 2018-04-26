(ns conduit.boundary.user
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol User
  (by-id [db id])
  (by-username [db id])
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
  (by-id [{db :spec} id]
    (when-let [user (first (jdbc/find-by-keys db "\"user\"" {:id id}))]
      (dissoc user :password)))
  (by-username [{db :spec} username]
    (when-let [user (first (jdbc/find-by-keys db "\"user\"" {:username username}))]
      (dissoc user :password)))
  (find-login [{db :spec} email password]
    (when-let [user (first (jdbc/find-by-keys db "\"user\"" {:email email}))]
      (when (hashers/check password (:password user))
        (dissoc user :password))))
  (follow [db follower-id followee-id]
    (jdbc/execute! (:spec db)
      [(str "INSERT INTO \"follow\" (follower_id, followee_id)"
         " SELECT ?, ?"
         " WHERE NOT EXISTS (SELECT * FROM \"follow\""
         " WHERE follower_id = ? AND followee_id = ?)")
       follower-id followee-id follower-id followee-id]))
  (unfollow [db follower-id followee-id]
    (jdbc/delete! (:spec db) "\"follow\"" ["follower_id = ? AND followee_id = ?" follower-id followee-id])))
