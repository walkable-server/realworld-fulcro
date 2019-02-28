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

(defn find-user-in-params [db params]
  (cond
    (:login params)
    (let [{:user/keys [email password]} (:login params)]
      (user/find-login db email password))

    (:sign-up params)
    (let [new-user (:sign-up params)]
      (user/create-user db (rename-keys new-user mutations/remove-user-namespace)))))

(def guest-user
  #:user{:id        :guest
         :name      "Guest"
         :email     "non@exist"
         :app/token "No token"})

(def pre-processing-login
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (if (= (-> env :ast :dispatch-key) :user/whoami)
         (if (map? (-> env :ast :params))
           (let [params                      (-> env :ast :params)
                 {:app/keys [db jwt-secret]} env]
             (when-not (:logout params)
               guest-user
               (if-let [{user-id :id} (find-user-in-params db params)]
                 (let [token (jwt/sign {:user/id user-id} jwt-secret)]
                   (-> env
                     (assoc :app/current-user user-id)
                     reader
                     (assoc :app/token token)))
                 guest-user)))
           (let [result (reader env)]
             (if (seq result)
               result
               guest-user)))
         (reader env))))})

(def query-top-tags
  "SELECT \"tag\" AS \"tag/tag\",
   COUNT (*) AS \"tag/count\"
   FROM \"tag\"
   GROUP BY \"tag\"
   ORDER BY \"tag/count\" DESC
   LIMIT 20")

(defn get-items-subquery [query]
  (->> query
    (some #(and (map? %) (get % :pagination/items)))))

(defn query-params [env]
  (let [{:pagination/keys [size start end] :or {size 10}} (env/params env)]
    {:order-by [:article/id (if-not (number? end) :desc :asc)]
     :filters  (cond
                 (number? end)
                 [:<= :article/id end]
                 (number? start)
                 [:<= start :article/id])}))

(defn extra-filter [env]
  (let [{:pagination/keys [list-type list-id]} (env/params env)]
    (case list-type
      :liked-articles/by-user-id
      {:article/liked-by [:= :user/id list-id]}

      :owned-articles/by-user-id
      [:= :article/author-id list-id]

      :articles/by-tag
      {:article/tags [:= :tag/tag list-id]}

      ;; default
      nil)))

(defn merge-filters [xs]
  (let [xs (remove nil? xs)]
    (when (seq xs)
      (if (= 1 (count xs))
        (first xs)
        (into [:and] xs)))))

(defn next-id [env]
  (let [{:pagination/keys [list-type list-id size start end] :or {size 10}} (env/params env)

        {:keys [parser query]} env

        query-root
        (cond
          (= [list-type list-id] [:articles/by-feed :personal])
          :feed.personal/next-id

          :default
          :feed.global/next-id)

        params {:order-by [:article/id :desc]
                :limit    1
                :offset   (when-not (number? end) size)
                :filters  (merge-filters
                            [(extra-filter env)
                             (cond
                               (number? end)
                               [:< :article/id end]
                               (number? start)
                               [:<= :article/id start])])}]
    (-> (parser env [(list query-root params)])
      (get query-root))))

(defn previous-id [env]
  (let [{:pagination/keys [list-type list-id size start end] :or {size 10}} (env/params env)

        {:keys [parser query]} env
        query-root
        (cond
          (= [list-type list-id] [:articles/by-feed :personal])
          :feed.personal/next-id

          :default
          :feed.global/next-id)

        params {:order-by [:article/id :asc]
                :limit    1
                :offset   (when (number? end) size)
                :filters  (merge-filters
                            [(extra-filter env)
                             (cond
                               (number? end)
                               [:<= end :article/id]
                               (number? start)
                               [:< start :article/id])])}]
    (when (or end start)
      (-> (parser env [(list query-root params)])
        (get query-root)))))

(defn fetch-items [env]
  (let [{:pagination/keys [list-type list-id size start end] :or {size 10}} (env/params env)

        {:keys [parser query]} env

        query-root
        (cond
          (= [list-type list-id] [:articles/by-feed :personal])
          :feed.personal/articles

          :default
          :feed.global/articles)

        params      {:order-by [:article/id (if (number? end) :asc :desc)]
                     :limit    size
                     :filters  (merge-filters
                                 [(extra-filter env)
                                  (cond
                                    (number? end)
                                    [:<= end :article/id]
                                    (number? start)
                                    [:<= :article/id start])])}
        items-query [{(list query-root params) (get-items-subquery query)}]]
    (-> (parser env items-query)
      (get query-root))))

(defn paginated-list-resolver [env]
  (if (not= :paginated-list/articles (env/dispatch-key env))
    ::p/continue
    (let [{:pagination/keys [list-type list-id size start end] :or {size 10}} (env/params env)

          items (fetch-items env)]
      (merge
        {(if (number? end) :pagination/end :pagination/start)
         (or (:article/id (first items)) :no-start)}
        #:pagination{:size        size
                     :list-type   list-type
                     :list-id     list-id
                     :next-id     (when-let [n (next-id env)]
                                    #:pagination{:list-type list-type
                                                 :list-id   list-id
                                                 :size      size
                                                 :start     n})
                     :previous-id (when-let [p (previous-id env)]
                                    #:pagination{:list-type list-type
                                                 :list-id   list-id
                                                 :size      size
                                                 :end       p})
                     :items       items}))))

(def tag-resolver
  {:tags/all (fn [{::sqb/keys [run-query sql-db]}]
               ;; todo: cache this!
               (into [] (run-query sql-db [query-top-tags])))})

(def pathom-parser
  (p/parser
    {:mutate server-mutate
     ::p/plugins
     [pre-processing-login
      (p/env-plugin
        {::p/reader
         [paginated-list-resolver
          sqb/pull-entities
          p/map-reader
          p/env-placeholder-reader
          tag-resolver]})]}))

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
