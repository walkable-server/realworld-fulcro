(ns conduit.boundary.user
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol User
  (create-user [db user])
  (find-login [db email password])
  (update-user [db user-id user])
  (follow [db follower-id followee-id])
  (unfollow [db follower-id followee-id]))

(defn hash-password [user]
  (if (contains? user :user/password)
    (if (seq (:user/password user))
      (update user :user/password hashers/derive)
      (dissoc user :user/password))
    user))

(defn user-in [user]
  (select-keys user [:user/name :user/email :user/bio :user/image]))

(defn user-out [{:keys [id]}]
  {:user/id id})

(defn user-error-message [msg]
  (condp re-find msg
    #"ERROR: duplicate key value violates unique constraint \"user_email_key\""
    :error/email-taken
    :error/unknown))

(extend-protocol User
  duct.database.sql.Boundary
  (create-user [{db :spec} user]
    (try
      (->> (-> user user-in hash-password)
           (jdbc/insert! db "\"user\"")
           first
           user-out)
      (catch org.postgresql.util.PSQLException e
        (let [msg (.getMessage e)]
          {:error (user-error-message msg)}))))
  (update-user [db user-id user]
    (->> (jdbc/update! (:spec db) "\"user\""
                       (-> user user-in hash-password)
                       ["id = ?" user-id])
         first
         user-out))
  (find-login [{db :spec} email password]
    (let [user (first (jdbc/find-by-keys db "\"user\"" {:email email}))]
      (if (and user (hashers/check password (:password user)))
        {:user/id (:id user)}
        {:error :error/user-not-found})))
  (follow [db follower-id followee-id]
    (jdbc/execute! (:spec db)
      [(str "INSERT INTO \"follow\" (follower_id, followee_id)"
         " SELECT ?, ?"
         " WHERE NOT EXISTS (SELECT * FROM \"follow\""
         " WHERE follower_id = ? AND followee_id = ?)")
       follower-id followee-id follower-id followee-id]))
  (unfollow [db follower-id followee-id]
    (jdbc/delete! (:spec db) "\"follow\"" ["follower_id = ? AND followee_id = ?" follower-id followee-id])))
