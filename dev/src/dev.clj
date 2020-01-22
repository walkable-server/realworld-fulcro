(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [duct.repl.figwheel :refer [cljs-repl]]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]))

(duct/load-hierarchy)

(defmethod ig/init-key :fulcro.module/cljs-build-options
  [_ {build-id :build-id :or {build-id 0}}]
  (fn [config]
    (update-in config
      [:duct.server/figwheel :builds build-id :build-options]
      duct/merge-configs
      (:fulcro/cljs-build-options config))))

(defmethod ig/init-key :fulcro/cljs-build-options [_ _]
  identity)

(defn read-config []
  (duct/read-config (io/resource "conduit/config.edn")))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))

(defn db []
  (-> system (ig/find-derived-1 :duct.database/sql) val :spec))

(defn q [sql]
  (jdbc/query (db) sql))

(defn e [sql]
  (jdbc/execute! (db) sql))

(comment
  (q "select * from \"user\"")
  (jdbc/insert! (db) "\"favorite\"" {"user_id" 1 "article_id" 5})
  (try
    (jdbc/insert! (db) "\"user\"" {:email "j@j"})

    (catch org.postgresql.util.PSQLException e
      (.getMessage e)))

  "org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"user_email_key\"\n  Detail: Key (email)=(j@j) already exists."
  )

(defn w
  ([query] (w query nil))
  ([query user-id]
   (let [f (-> system :conduit.handler.walkable/resolver)]
     (:body (f {:identity {:user/id user-id} :body-params query})))))
#_
(w `[{[:article/by-id 3]
      [:article/id (:article/liked-by-me {:filters false})]}]
  1)

#_
(w `[{(:app/users {:filters [:in :user/id 2 3 13 17 20 21]})
      [:user/id :user/followed-by-me]}]
  1)
#_
(w `[{[:app.articles/list
       ~(merge #:app.articles.list{:list-id   :global
                                   :list-type :app.articles/on-feed
                                   :size      3
                                   :direction :forward}
          #:app.articles.list.page {:operation :next ;;:current
                                    :start     7
                                    :end       5})]
      [:app.articles.list.page/size
       :app.articles.list.page/direction
       :app.articles.list.page/start
       :app.articles.list.page/total-items
       :app.articles.list.page/end
       {:app.articles.list/current-page
        [:app.articles.list.page/first-item-id
         :app.articles.list.page/last-item-id
         {:app.articles.list.page/items
          [:article/id]}]}]}]
  1)
