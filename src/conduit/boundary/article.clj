(ns conduit.boundary.article
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [conduit.util :as util]
            [duct.database.sql]))

(def remove-article-namespace
  (util/remove-namespace "article" [:title :body :slug :description :tags]))

(defprotocol Article
  (article-by-slug [db article])
  (create-article [db author-id article])
  (destroy-article [db author-id article-id])
  (update-article [db author-id id article])
  (like [db user-id article-id])
  (unlike [db user-id article-id]))

(defprotocol Comment
  (create-comment [db author-id article-id comment])
  (update-comment [db author-id comment-id comment])
  (destroy-comment [db author-id comment-id]))

(defprotocol Tag
  (add-tag [db author-id article-id tag])
  (remove-tag [db author-id article-id tag]))

(defn delete-non-existing-where-clause [article-id existing]
  (concat
    [(str "article_id = ? and tag not in ("
        (str/join ", " (repeat (count existing) \?))
        ")")
     article-id]
    (vec existing)))

(comment
  (= (delete-non-existing-where-clause 1 #{"foo" "bar"})
    ["article_id = ? and tag not in (?, ?)" 1 "foo" "bar"]))

(extend-protocol Article
  duct.database.sql.Boundary
  (article-by-slug [{db :spec} article-slug]
    (:id (first (jdbc/find-by-keys db "\"article\"" {:slug article-slug}))))

  (create-article [{db :spec} author-id article]
    (let [tags           (:article/tags article)
          article        (-> (rename-keys article remove-article-namespace)
                           (select-keys [:title :slug :description :body])
                           (assoc :author_id author-id))
          results        (jdbc/insert! db "\"article\"" article)
          new-article-id (-> results first :id)]
      (when new-article-id
        (when (seq tags)
          (jdbc/insert-multi! db "\"tag\""
            (mapv (fn [{:tag/keys [tag]}] {:article_id new-article-id :tag tag}) tags)))
        new-article-id)))

  (destroy-article [db author-id article-id]
    (jdbc/delete! (:spec db) "\"article\"" ["author_id = ? AND id = ?" author-id article-id]))

  (update-article [db author-id id article]
    (let [results (jdbc/query (:spec db)
                    ["select id, article_id, tag from \"article\" left join \"tag\" on tag.article_id = article.id where author_id = ? and id = ?"
                     author-id id])]
      (when (seq results)
        (let [new-article (-> (rename-keys article remove-article-namespace)
                            (select-keys [:slug :title :description :body]))]
          (when (seq new-article)
            (jdbc/update! (:spec db) "\"article\"" new-article ["id = ?" id])))
        (when (:article/tags article)
          (let [old-tags (->> (filter :article_id results)
                           (map :tag) set)
                new-tags (->> (:article/tags article)
                           (map :tag/tag) set)
                existing (clojure.set/intersection old-tags new-tags)]
            (jdbc/delete! (:spec db) "\"tag\"" (delete-non-existing-where-clause id existing))
            (jdbc/insert-multi! (:spec db) "\"tag\""
              (->> (clojure.set/difference new-tags existing)
                (mapv (fn [tag] {:article_id id :tag tag}))))
            {})))))

  (like [db user-id article-id]
    (jdbc/execute! (:spec db)
      [(str "INSERT INTO \"favorite\" (user_id, article_id)"
         " SELECT ?, ?"
         " WHERE NOT EXISTS (SELECT * FROM \"favorite\""
         " WHERE user_id = ? AND article_id = ?)")
       user-id article-id user-id article-id]))

  (unlike [db user-id article-id]
    (jdbc/delete! (:spec db) "\"favorite\"" ["user_id = ? AND article_id = ?" user-id article-id]))
  )

(extend-protocol Comment
  duct.database.sql.Boundary
  (create-comment [db author-id article-id comment-item]
    (let [comment-item (-> comment-item
                         (select-keys [:body])
                         (assoc :author_id author-id)
                         (assoc :article_id article-id))
          results (jdbc/insert! (:spec db) "\"comment\"" comment-item)]
      (-> results first :id)))
  (update-comment [db author-id comment-id comment-item]
    (jdbc/update! (:spec db) "\"comment\"" (select-keys comment-item [:body])
      ["author_id = ? AND id = ?" author-id comment-id]))
  (destroy-comment [{db :spec} author-id comment-id]
    (jdbc/delete! db "\"comment\"" ["author_id = ? AND id = ?" author-id comment-id])))

(extend-protocol Tag
  duct.database.sql.Boundary
  (add-tag [db author-id article-id tag]
    (let [results (jdbc/query (:spec db)
                    ["select id from \"article\" where author_id = ? and id = ?"
                     author-id article-id])]
      (when (seq results)
        (jdbc/execute! (:spec db)
          [(str "INSERT INTO \"tag\" (tag, article_id)"
             " SELECT ?, ?"
             " WHERE NOT EXISTS (SELECT * FROM \"tag\""
             " WHERE tag = ? AND article_id = ?)")
           tag article-id tag article-id]))))
  (remove-tag [db author-id article-id tag]
    (let [results (jdbc/query (:spec db)
                    ["select id from \"article\" where author_id = ? and id = ?"
                     author-id article-id])]
      (when (seq results)
        (jdbc/delete! (:spec db) "\"tag\"" ["tag = ? AND article_id = ?" tag article-id])))))
