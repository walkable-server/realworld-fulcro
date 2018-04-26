(ns conduit.handler.user
  (:require [ataraxy.core :as ataraxy]
            [buddy.sign.jwt :as jwt]
            [conduit.boundary.user :as user]
            [buddy.hashers :as hashers]
            [ataraxy.response :as response]
            [integrant.core :as ig]))

(defn with-token [user jwt-secret]
  (->> (jwt/sign {:user-id (:id user)} jwt-secret)
    (assoc user :token)))

(defn error-message [body]
  [::response/bad-request {:errors {:body body}}])

(defmethod ig/init-key ::create [_ {:keys [db jwt-secret]}]
  (fn [{[_ user] :ataraxy/result}]
    (if-let [new-user (user/create-user db user)]
      [::response/ok {:user (with-token new-user jwt-secret)}]
      (error-message "Failed create user!"))))

(defmethod ig/init-key ::login [_ {:keys [db jwt-secret]}]
  (fn [{[_kw {:keys [email password]}] :ataraxy/result}]
    (if-let [user (user/find-login db email password)]
      [::response/ok {:user (with-token user jwt-secret)}]
      (error-message "Failed to login!"))))

(defmethod ig/init-key ::whoami [_ {:keys [db jwt-secret]}]
  (fn [{id :identity}]
    (if id
      (if-let [user (user/by-id db (:user-id id))]
        [::response/ok {:user (with-token user jwt-secret)}]
        (error-message "No such user!"))
      (error-message "You must login first!"))))

(def profile-query
  [:user/id :user/username :user/bio :user/image
   {:user/followed-by-me? [:agg/count]}
   {:user/followed-by [:user/id :user/username]}])

(defmethod ig/init-key ::by-username
  [_ {:keys [resolver]}]
  (fn [{[_kw username] :ataraxy/result
        id             :identity}]
    [::response/ok
     (resolver (:user-id id)
       `[{[:user/by-username ~username]
          ~profile-query}])]))
