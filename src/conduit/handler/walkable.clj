(ns conduit.handler.walkable
  (:require [walkable.sql-query-builder :as sqb]
            [integrant.core :as ig]
            [com.wsscode.pathom.core :as p]))

(def pathom-parser
  (p/parser
    {::p/plugins
     [(p/env-plugin
        {::p/reader
         [sqb/pull-entities p/map-reader]})]}))

(def extra-conditions
  {:articles/feed
   (fn [{:keys [current-user]}]
     {:article/author {:user/followed-by [:= current-user :user/id]}})

   :article/liked-by-me?
   (fn [{:keys [current-user]}] [:= current-user :user/id])

   :user/followed-by-me?
   (fn [{:keys [current-user]}] [:= current-user :user/id])})

(defmethod ig/init-key ::compile-schema [_ schema]
  (sqb/compile-schema (assoc schema
                        :quote-marks      sqb/quotation-marks
                        :extra-conditions extra-conditions)))

(defmethod ig/init-key ::query [_ env]
  (fn walkable-query
    ([query]
     (pathom-parser env query))
    ([current-user query]
     (pathom-parser (assoc-in env :current-user current-user) query))))
