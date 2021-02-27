(ns conduit.handler.walkable
  (:require [walkable.core :as walkable]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [conduit.handler.mutations :refer [mutations]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

;; TODO: keep this for dev env only
(pc/defresolver index-explorer [env _]
  {::pc/input  #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index
   (get env ::pc/indexes)})

(pc/defresolver user-valid?
  [env params]
  {::pc/input  #{:user/id}
   ::pc/output [:user/valid?]}
  {:user/valid? (boolean (:user/id params))})

(def resolvers
  [index-explorer user-valid?])

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

(defmethod ig/init-key ::connect [_ registry]
  (walkable/connect-plugin {:db-type :postgres
                            :registry (concat registry
                                        [{:key 'app.auth/current-user
                                          :type :variable
                                          :compute (fn [env] (:app.auth/current-user env))}])
                            :query-env #(jdbc/query (:db-spec %1) %2)}))

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
  [_ [{:keys [:connect]} {:keys [:app/db] :as env}]]
  (let [parser (pathom-parser connect)]
    (fn [{:keys [:session]
          current-user :identity
          edn-query :transit-params}]
      (jdbc/with-db-connection [conn (:spec db)]
        (let [env (merge env {:db-spec conn
                              :app.auth/current-user (:user/id current-user)})
              result (parser env edn-query)]
          (add-session result session))))))
