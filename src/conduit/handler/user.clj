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

(defmethod ig/init-key ::create [_ {:keys [db jwt-secret]}]
  (fn [{[_ user] :ataraxy/result}]
    (if-let [new-user (user/create-user db user)]
      [::response/ok {:user (with-token new-user jwt-secret)}]
      [::response/bad-request {:errors {:body "Failed create user!"}}])))

(defmethod ig/init-key ::login [_ {:keys [db jwt-secret]}]
  (fn [{[_kw {:keys [email password]}] :ataraxy/result}]
    (if-let [user (user/find-login db email password)]
      [::response/ok {:user (with-token user jwt-secret)}]
      [::response/bad-request "Failed!"])))

(defmethod ig/init-key ::whoami
  [_ opts]
  (fn [{id :identity}]
    [::response/ok {:message (str "your user id: " (:user-id id))}]))

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
       `[{(:user/by-username ~username)
          ~profile-query}])]))
