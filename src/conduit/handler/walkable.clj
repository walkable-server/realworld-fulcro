(ns conduit.handler.walkable
  (:require [walkable.sql-query-builder :as sqb]
            [integrant.core :as ig]
            [clojure.set :refer [rename-keys]]
            [conduit.boundary.user :as user]
            [buddy.sign.jwt :as jwt]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations :as mutations]
            [fulcro.server :as server :refer [parser server-mutate defmutation]]
            [com.wsscode.pathom.core :as p]))

(defn find-user-in-params [db params]
  (cond
    (:login params)
    (let [{:keys [email password]} (:login params)]
      (user/find-login db email password))

    (:sign-up params)
    (let [new-user (:sign-up params)]
      (user/create-user db (rename-keys new-user mutations/remove-user-namespace)))))

(def pre-processing-login
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (if (and (= (-> env :ast :dispatch-key)
                  :user/whoami)
             (map? (-> env :ast :params)))
         (let [params                                       (-> env :ast :params)
               {:app/keys [db jwt-secret]} env]
           (if-let [{user-id :id} (find-user-in-params db params)]
             (let [token (jwt/sign {:user/id user-id} jwt-secret)]
               (-> env
                 (assoc :app/current-user user-id)
                 reader
                 (assoc :token token)))
             {}))
         (reader env))))})

(def query-top-tags
  "SELECT \"tag\" AS \"tag/tag\",
   COUNT (*) AS \"tag/count\"
   FROM \"tag\"
   GROUP BY \"tag\"
   ORDER BY \"tag/count\" DESC
   LIMIT 20")

(def pathom-parser
  (p/parser
    {:mutate server-mutate
     ::p/plugins
     [(p/env-plugin
        {::p/reader
         [sqb/pull-entities p/map-reader p/env-placeholder-reader
          {:tags/all (fn [{::sqb/keys [run-query sql-db]}]
                       ;; todo: cache this!
                       (into [] (run-query sql-db [query-top-tags])))}]})
      pre-processing-login]}))

(def extra-conditions
  {:articles/feed
   (fn [{:app/keys [current-user]}]
     {:article/author {:user/followed-by [:= current-user :user/id]}})

   :article/liked-by-me?
   (fn [{:app/keys [current-user]}] [:= current-user :user/id])

   :user/whoami
   (fn [{:app/keys [current-user]}] [:= current-user :user/id])

   :user/followed-by-me?
   (fn [{:app/keys [current-user]}] [:= current-user :user/id])})

(defmethod ig/init-key ::compile-schema [_ schema]
  (-> schema
    (assoc :quote-marks sqb/quotation-marks
      :extra-conditions extra-conditions)
    sqb/compile-schema))

(defmethod ig/init-key ::resolver [_ {:app/keys [db] :as env}]
  (fn [{current-user :identity
        query        :body-params}]
    (jdbc/with-db-connection [conn (:spec db)]
      {:body (pathom-parser (merge env
                              #::sqb{:sql-db           conn
                                     :run-query        jdbc/query
                                     :app/current-user (:user/id current-user)})
               query)})))
