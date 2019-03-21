(ns conduit.handler.walkable
  (:require [walkable.sql-query-builder :as sqb]
            [walkable.sql-query-builder.emitter :as emitter]
            [walkable.sql-query-builder.floor-plan :as floor-plan]
            [walkable.sql-query-builder.pathom-env :as env]
            [integrant.core :as ig]
            [clojure.set :refer [rename-keys]]
            [conduit.boundary.user :as user]
            [buddy.sign.jwt :as jwt]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations :as mutations]
            [fulcro.server :as server :refer [server-mutate]]
            [com.wsscode.pathom.core :as p]
            [clojure.spec.alpha :as s]))

(defn get-items-subquery [query]
  (->> query
    (some #(and (map? %) (get % :app.articles.list/current-page)))
    (some #(and (map? %) (get % :app.articles.list.page/items)))))

(defn page-filters [{:app.articles.list/keys [direction]
                     :app.articles.list.page/keys [start end]}]
  (when (every? number? [start end])
    (if (= :forward direction)
      [:<= end :article/id start]
      [:<= start :article/id end])))

(defn order-by [direction]
  [:article/id (if (= direction :forward) :desc :asc)])

(defn list-filters [{:app.articles.list/keys [list-type list-id]}]
  (case list-type
    :app.articles/liked-by-user
    {:article/liked-by [:= :user/id list-id]}

    :app.articles/owned-by-user
    [:= :article/author-id list-id]

    :app.articles/with-tag
    {:article/tags [:= :tag/tag list-id]}

    ;; default
    nil))

(defn merge-filters [xs]
  (let [xs (remove nil? xs)]
    (when (seq xs)
      (if (= 1 (count xs))
        (first xs)
        (into [:and] xs)))))

(defn fetch-items
  [query-root params {:keys [parser query] :as env}]
  (let [items-query
        [{(list query-root params) (get-items-subquery query)}]]
    (-> (parser env items-query)
      (get query-root))))

(defn query-root
  [{:app.articles.list/keys [list-type list-id]}]
  (cond
    (= [list-type list-id] [:app.articles/on-feed :personal])
    :feed.personal/articles

    :default
    :feed.global/articles))

(defn fetch-list-stats
  [query-root list-filters {:keys [parser] :as env}]
  (let [query [{(list query-root {:filters list-filters})
                [:app.articles.list/first-item-id
                 :app.articles.list/last-item-id
                 :app.articles.list/total-items]}]]
    (-> (parser env query)
      (get query-root)
      first)))

(defn list-ident-value
  [{:app.articles.list/keys [list-type list-id direction size]
    :or                     {list-type :app.articles/on-feed
                             list-id   :global
                             direction :forward
                             size      5}}]
  #:app.articles.list{:list-type list-type
                      :list-id   list-id
                      :direction direction
                      :size      size})

(defn list-ident
  [props]
  [:app.articles/list (list-ident-value props)])

(defn article-list-resolver [env]
  (if (not= :app.articles/list (env/dispatch-key env))
    ::p/continue
    (let [page        (env/ident-value env)
          ident-value (list-ident-value page)
          qr          (query-root ident-value)
          lf          (list-filters ident-value)
          stats       (fetch-list-stats qr lf env)
          items       (fetch-items qr
                        {:order-by (order-by ident-value)
                         :limit    (:app.articles.list/size ident-value)
                         :filters  (merge-filters [lf (page-filters page)])}
                        env)]
      (merge ident-value stats
        {:app.articles.list/current-page
         (merge ident-value
           #:app.articles.list.page {:items items
                                     :start (-> items first :article/id)
                                     :end   (-> items last :article/id)})}))))

(def pathom-parser
  (p/parser
    {:mutate server-mutate
     ::p/plugins
     [(p/env-plugin
        {::p/reader
         [article-list-resolver
          sqb/pull-entities
          p/map-reader
          p/env-placeholder-reader]})]}))

(defmethod ig/init-key ::floor-plan [_ floor-plan]
  (-> floor-plan
    (assoc :emitter emitter/postgres-emitter
      :variable-getters [{:key     'app/current-user
                          :fn      (fn [env] (:app/current-user env))
                          :cached? true}])
    floor-plan/compile-floor-plan))

(defmethod ig/init-key ::resolver [_ {:app/keys [db] :as env}]
  (fn [{current-user :identity
        query        :body-params}]
    (jdbc/with-db-connection [conn (:spec db)]
      {:body (pathom-parser (merge env
                              #::sqb{:sql-db           conn
                                     :run-query        jdbc/query
                                     :app/current-user (:user/id current-user)})
               query)})))
