(ns conduit.handler.walkable
  (:require [walkable.core :as walkable]
            [walkable.sql-query-builder.emitter :as emitter]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations :refer [mutations]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

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
        TagList [:tag/tag :tag/count]
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
      ::pc/output [{:app.global-feed/articles Article}]}
     {::pc/input #{}
      ::pc/output [{:app.personal-feed/articles Article}]}
     {::pc/input #{}
      ::pc/output [{:app.tags/top-list TagList}]}
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
