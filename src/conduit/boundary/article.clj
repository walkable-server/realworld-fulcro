(ns conduit.boundary.article
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol Article
  (article-by-slug [db article])
  (create-article [db author-id article])
  (destroy-article [db author-id article-id])
  (update-article [db author-id id article])
  (like [db user-id article-id])
  (unlike [db user-id article-id]))

(defprotocol Comment
  (create-comment [db article-slug comment])
  (destroy-comment [db author-id comment-id]))

(extend-protocol Article
  duct.database.sql.Boundary
  (article-by-slug [{db :spec} article-slug]
    (:id (first (jdbc/find-by-keys db "\"article\"" {:slug article-slug}))))

  (create-article [{db :spec} author-id article]
    (let [tags           (:tags article)
          article        (-> article (select-keys [:title :slug :description :body])
                           (assoc :author_id author-id))
          results        (jdbc/insert! db "\"article\"" article)
          new-article-id (-> results first :id)]
      (when new-article-id
        (when (seq tags)
          (jdbc/insert-multi! db "\"tag\""
            (mapv (fn [tag] {:article_id new-article-id :tag tag}) tags)))
        new-article-id)))

  (destroy-article [db author-id article-id]
    (jdbc/delete! (:spec db) "\"article\"" ["author_id = ? AND id = ?" author-id article-id]))

  (update-article [db author-id id article]
    (jdbc/update! (:spec db) "\"article\"" (select-keys article [:slug :title :description :body])
      ["author_id = ? AND id = ?" author-id id]))

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
  (create-comment [db article-slug comment-item]
    (when-let [article-id  (article-by-slug db article-slug)]
      (let [comment-item (-> comment-item
                      (select-keys [:author_id :body])
                      (assoc :article_id article-id))]
        (jdbc/insert! (:spec db) "\"comment\"" comment-item))))
  (destroy-comment [{db :spec} author-id comment-id]
    (jdbc/delete! db "\"comment\"" ["author_id = ? AND id = ?" author-id comment-id])))
