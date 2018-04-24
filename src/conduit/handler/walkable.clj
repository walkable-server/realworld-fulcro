(ns conduit.handler.walkable
  (:require [walkable.sql-query-builder :as sqb]
            [integrant.core :as ig]
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

           :article/liked-by-count
           (let [{n :agg/count} (reader env)]
             n)
           ;; default
           (reader env)))))})

(def pathom-parser
  (p/parser
    {::p/plugins
     [(p/env-plugin
        {::p/reader
         [sqb/pull-entities p/map-reader]})
      post-processing]}))

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

(defmethod ig/init-key ::resolver [_ env]
  (fn resolver
    ([current-user query]
     (pathom-parser (assoc env :current-user current-user) query))))
