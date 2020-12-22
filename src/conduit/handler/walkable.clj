(ns conduit.handler.walkable
  (:require [walkable.core :as walkable]
            [walkable.sql-query-builder.emitter :as emitter]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations :refer [mutations]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

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

(pc/defresolver user-valid?
  [env params]
  {::pc/input  #{:user/id}
   ::pc/output [:user/valid?]}
  {:user/valid? (boolean (:user/id params))})

(def resolvers
  [user-valid?])

(defn pathom-parser [walkable-connect]
  (p/parser
   {::p/env {::p/reader [p/map-reader
                         pc/reader3
                         pc/open-ident-reader
                         p/env-placeholder-reader]
             ::pc/mutation-join-globals [:tempids]
             ::p/placeholder-prefixes #{">"}}
    ::p/mutate pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register (into resolvers mutations)})
                 walkable-connect
                 p/elide-special-outputs-plugin
                 p/error-handler-plugin
                 p/trace-plugin]}))

(defmethod ig/init-key ::connect [_ config]
  (let [config (update-in config [:floor-plan]
                          assoc
                          :emitter emitter/postgres-emitter
                          :variable-getters [{:key 'app.auth/current-user
                                              :fn (fn [env] (:app.auth/current-user env))
                                              :cached? true}])]
    (walkable/connect-plugin config)))

(defmethod ig/init-key ::inputs-outputs [_ _]
  (let [User [:user/id :user/email :user/name :user/username :user/bio :user/image
              :user/followed-by-me :user/followed-by-count]
        Article [:article/id :article/slug :article/title
                 :article/description :article/body :article/image
                 :article/created-at :article/updated-at
                 :article/liked-by-count :article/liked-by-me]
        Tag [:tag/tag]
        Comment [:comment/id :comment/created-at :comment/updated-at
                 :comment/body]]
    ;; idents
    [{::pc/input #{:user/id}
      ::pc/output User}
     {::pc/input #{:article/id}
      ::pc/output Article}
     ;; roots
     {::pc/input #{}
      ::pc/output [{:session/current-user User}]}
     {::pc/input #{}
      ::pc/output [{:app/users User}]}
     {::pc/input #{}
      ::pc/output [{:app.articles/list Article}]}
     ;; roots with group-by
     {::pc/input #{}
      ::pc/output [{:app.tags/top-list [:tag/tag :tag/count]}]}
     ;; joins
     {::pc/input #{:user/id}
      ::pc/output [{:user/followed-by User}]}
     {::pc/input #{:user/id}
      ::pc/output [{:user/follows User}]}
     {::pc/input #{:article/id}
      ::pc/output [{:article/tags Tag}]}
     {::pc/input #{:article/id}
      ::pc/output [{:article/comments Comment}]}
     {::pc/input #{:article/id}
      ::pc/output [{:article/liked-by User}]}
     {::pc/input #{:user/id}
      ::pc/output [{:user/likes Article}]}
     {::pc/input #{:user/id}
      ::pc/output [{:user/articles Article}]}
     {::pc/input #{:article/id}
      ::pc/output [{:article/author User}]}
     {::pc/input #{:comment/id}
      ::pc/output [{:comment/author User}]}]))

(defn add-session [result session]
  (let [response {:body result}]
    (if-let [new-user
             (or (get-in result '[conduit.session/login :user/id])
               (get-in result '[conduit.session/sign-up :user/id]))]
      (assoc response :session (assoc-in session [:identity :user/id] new-user))
      (if (contains? result 'conduit.session/logout)
        (assoc response :session {})
        (assoc response :session session)))))

(defmethod ig/init-key ::resolver
  [_ {:app/keys [db] :keys [connect] :as env}]
  (let [parser (pathom-parser connect)]
    (fn [{current-user :identity edn-query :transit-params
          :keys [session]}]
      (jdbc/with-db-connection [conn (:spec db)]
        (let [env (->> #::walkable {:db conn
                                    :run jdbc/query
                                    :app.auth/current-user (:user/id current-user)}
                    (merge env))
              result (parser env edn-query)]
          (add-session result session))))))
