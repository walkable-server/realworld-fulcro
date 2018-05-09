(ns conduit.handler.walkable
  (:require [walkable.sql-query-builder :as sqb]
            [integrant.core :as ig]
            [conduit.handler.mutations :as mutations]
            [fulcro.server :as server :refer [parser server-mutate defmutation]]
            [com.wsscode.pathom.core :as p]))

(def post-processing
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (let [k (-> env :ast :dispatch-key)]
         (case k
           :article/tags
           (let [tags (reader env)]
             (mapv :tag/tag tags))

           (:article/liked-by-me? :user/followed-by-me?)
           (let [{n :agg/count} (reader env)]
             (not= 0 n))

           (:article/liked-by-count :user/articles-count)
           (let [{n :agg/count} (reader env)]
             n)
           ;; default
           (reader env)))))})

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
      ;;post-processing
      ]}))

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

(defmethod ig/init-key ::resolver [_ env]
  (fn [{current-user :identity
        query        :body-params}]
    {:body (pathom-parser (assoc env :app/current-user (:user/id current-user))
             query)}))
