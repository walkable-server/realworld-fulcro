(ns conduit.boundary.user
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [conduit.ui.errors :as errors]
            [conduit.ui.return :as return]
            [duct.database.sql]))

(defprotocol User
  (create-user [db user])
  (find-login [db email password])
  (update-user [db user-id user])
  (follow [db follower-id followee-id])
  (unfollow [db follower-id followee-id]))

(defn hash-password [user]
  (if (contains? user :password)
    (if (seq (:password user))
      (update user :password hashers/derive)
      (dissoc user :password))
    user))

(defn user-error-message [msg]
  (condp re-find msg
    #"ERROR: duplicate key value violates unique constraint \"user_email_key\""
    {:user/email [errors/email-taken]}
    {errors/other-category [errors/unknown]}))

(extend-protocol User
  duct.database.sql.Boundary
  (create-user [{db :spec} {:keys [password] :as user}]
    (let [pw-hash (hashers/derive password)]
      (try
        (-> (jdbc/insert! db "\"user\""
              (-> user (select-keys [:name :email :bio :image])
                (assoc :password pw-hash)))
          first (dissoc :password)
          return/result)
        (catch org.postgresql.util.PSQLException e
          (let [msg (.getMessage e)]
            (return/errors (user-error-message msg)))))))
  (update-user [db user-id user]
    (jdbc/update! (:spec db) "\"user\""
      (-> user (select-keys [:name :email :bio :image :password]) hash-password)
      ["id = ?" user-id]))
  (find-login [{db :spec} email password]
    (let [user (first (jdbc/find-by-keys db "\"user\"" {:email email}))]
      (if (and user (hashers/check password (:password user)))
        (return/result (dissoc user :password))
        (return/errors {errors/other-category [errors/can-not-log-in]}))))
  (follow [db follower-id followee-id]
    (jdbc/execute! (:spec db)
      [(str "INSERT INTO \"follow\" (follower_id, followee_id)"
         " SELECT ?, ?"
         " WHERE NOT EXISTS (SELECT * FROM \"follow\""
         " WHERE follower_id = ? AND followee_id = ?)")
       follower-id followee-id follower-id followee-id]))
  (unfollow [db follower-id followee-id]
    (jdbc/delete! (:spec db) "\"follow\"" ["follower_id = ? AND followee_id = ?" follower-id followee-id])))
