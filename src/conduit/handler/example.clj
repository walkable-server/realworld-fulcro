(ns conduit.handler.example
  (:require [ataraxy.core :as ataraxy]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [ataraxy.response :as response]
            [integrant.core :as ig]))

(defn find-user [_db username password]
  (when (and (= username "foo")
          (= password "bar"))
    {:id 998}))

(defmethod ig/init-key :conduit.handler/login [_ {:keys [db secret]}]
  (fn [{[_kw username password] :ataraxy/result}]
    (if-let [user (find-user db username password)]
      [::response/ok {:token (jwt/sign {:user (:id user)} secret)}]
      [::response/bad-request "Failed!"])))

(defmethod ig/init-key :conduit.handler/whoami
  [_ opts]
  (fn [{id :identity}]
    [::response/ok {:message (str "your user id: " id)}]))
