(ns conduit.handler.walkable
  (:require [walkable.core :as walkable]
            [walkable.sql-query-builder.emitter :as emitter]
            [walkable.sql-query-builder.floor-plan :as floor-plan]
            [walkable.sql-query-builder.pathom-env :as env]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [conduit.util :as util :refer [list-ident-value]]))

#_(defn get-items-subquery [query]
  (->> query
    (some #(and (map? %) (get % :app.articles.list/current-page)))
    (some #(and (map? %) (get % :app.articles.list.page/items)))))

#_(defn page-filters [{:app.articles.list/keys [direction]
                     :app.articles.list.page/keys [operation start end]}]
  (if (= :forward direction)
    (case operation
      :current
      [:>= start :article/id end]

      :next
      [:> end :article/id]

      :previous
      [:> :article/id start]

      ;; else
      nil)
    (case operation
      :current
      [:<= start :article/id end]

      :next
      [:< end :article/id]

      :previous
      [:< :article/id start]

      ;; else
      nil)))

#_(defn order-by [{:app.articles.list/keys [direction]
                 :app.articles.list.page/keys [operation]}]
  [:article/id (if (and (= direction :forward) (not= operation :previous))
                 :desc
                 :asc)])

#_(defn list-filters [{:app.articles.list/keys [list-type list-id]}]
  (case list-type
    :app.articles/liked-by-user
    {:article/liked-by [:= :user/id list-id]}

    :app.articles/owned-by-user
    [:= :article/author-id list-id]

    :app.articles/with-tag
    {:article/tags [:= :tag/tag list-id]}

    ;; default
    nil))

#_(defn merge-filters [xs]
  (let [xs (remove nil? xs)]
    (when (seq xs)
      (if (= 1 (count xs))
        (first xs)
        (into [:and] xs)))))

#_(defn fetch-items
  [query-root params {:keys [parser query] :as env}]
  (let [items-query
        [{(list query-root params) (get-items-subquery query)}]]
    (-> (parser env items-query)
      (get query-root))))

#_(defn query-root
  [{:app.articles.list/keys [list-type list-id]}]
  (cond
    (= [list-type list-id] [:app.articles/on-feed :personal])
    :feed.personal/articles

    :else
    :feed.global/articles))

#_(defn fetch-list-stats
  [query-root list-filters {:keys [parser] :as env}]
  (let [query [{(list query-root {:filters list-filters})
                [:app.articles.list/first-item-id
                 :app.articles.list/last-item-id
                 :app.articles.list/total-items]}]]
    (-> (parser env query)
      (get query-root)
      first)))

#_(defn article-list-resolver [env]
  (if (not= :app.articles/list (env/dispatch-key env))
    ::p/continue
    (let [page        (env/ident-value env)
          ident-value (list-ident-value page)
          direction   (:app.articles.list/direction ident-value)
          qr          (query-root ident-value)
          lf          (list-filters ident-value)
          stats       (fetch-list-stats qr lf env)
          items       (fetch-items qr
                        {:order-by (order-by ident-value)
                         :limit    (:app.articles.list/size ident-value)
                         :filters  (merge-filters [lf (page-filters page)])}
                        env)]
      (merge ident-value
        (clojure.set/rename-keys stats (when (= :forward direction)
                                         {:app.articles.list/first-item-id
                                          :app.articles.list/last-item-id
                                          :app.articles.list/last-item-id
                                          :app.articles.list/first-item-id}))
        {:app.articles.list/current-page
         (merge ident-value
           #:app.articles.list.page {:items items
                                     :start (-> items first :article/id)
                                     :end   (-> items last :article/id)})}))))

#_(defn whoami-resolver [env]
  (if (not= :user/whoami (env/dispatch-key env))
    ::p/continue
    (if-let [user-id (:app/current-user env)]
      (let [{:keys [parser query]} env
            ident [:user/by-id user-id]]
        (-> (parser env [{ident query}])
          (get ident)))
      #:user {:id :guest :email "non@exist"})))

(defn pathom-parser [walkable-connect]
  (p/parser
   {::p/env {::p/reader [p/map-reader
                         pc/reader3
                         pc/open-ident-reader
                         p/env-placeholder-reader]
             ::p/placeholder-prefixes #{">"}}
    ::p/mutate pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register []})
                 walkable-connect
                 p/elide-special-outputs-plugin
                 p/error-handler-plugin
                 p/trace-plugin]}))

(defmethod ig/init-key ::connect [_ config]
  (let [config (update-in config [:floor-plan]
                          assoc
                          :emitter emitter/postgres-emitter
                          :variable-getters [{:key 'app/current-user
                                              :fn (fn [env] (:app/current-user env))
                                              :cached? true}])]
    (walkable/connect-plugin config)))

(defmethod ig/init-key ::inputs-outputs [_ io]
  (into [] (for [{:keys [input output]} io]
             {::pc/output output
              ::pc/input input})))

(defmethod ig/init-key ::resolver
  [_ {:app/keys [db] :keys [connect] :as env}]
  (let [parser (pathom-parser connect)]
    (fn [{current-user :identity
          query :transit-params}]
      (jdbc/with-db-connection [conn (:spec db)]
        (let [env (->> #::walkable {:db conn
                                    :run jdbc/query
                                    :app/current-user (:user/id current-user)}
                       (merge env))]
          {:body (parser env query)})))))

;; p/env-plugin
(comment
  (let [my-plugin  {::p/wrap-parser   (fn [parser] (fn [env tx] (parser env tx)))
                    ::p/wrap-read     (fn [reader] (fn [env] (reader env)))
                    ::pc/wrap-resolve (fn [resolve] (fn [env input] (resolve env input)))}
        the-parser (p/parser {::p/plugins [my-plugin
                                           (p/env-plugin {::p/reader p/map-reader})]})]
    (the-parser {:db {1 {:a 2}
                      2 {:a 3}}}
      [{[:id 1] [:a]}])))
