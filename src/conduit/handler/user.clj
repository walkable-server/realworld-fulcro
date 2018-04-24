(ns conduit.handler.user
  (:require [ataraxy.core :as ataraxy]
            [buddy.sign.jwt :as jwt]
            [conduit.boundary.user :as user]
            [buddy.hashers :as hashers]
            [ataraxy.response :as response]
            [integrant.core :as ig]))

(defmethod ig/init-key ::create [_ {:keys [db]}]
  (fn [{[_ email username password] :ataraxy/result}]
    (let [id (user/create-user db email username password)]
      [::response/created (str "/users/" id)])))

(defmethod ig/init-key ::login [_ {:keys [db secret]}]
  (fn [{[_kw username password] :ataraxy/result}]
    (if-let [user (user/find-user db username password)]
      [::response/ok {:token (jwt/sign {:user-id (:id user)} secret)}]
      [::response/bad-request "Failed!"])))

(defmethod ig/init-key ::whoami
  [_ opts]
  (fn [{id :identity}]
    [::response/ok {:message (str "your user id: " (:user-id id))}]))
