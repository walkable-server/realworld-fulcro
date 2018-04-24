(ns conduit.boundary.article
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol Article
  (create-article [db article])
  (destroy-article [db author-id article-slug])
  (update-article [db article])
  (like [db user-id article-slug])
  (unlike [db user-id article-slug]))

(extend-protocol Article
  duct.database.sql.Boundary
  (create-article [{db :spec} article]
    (let [tags           (:tags article)
          article        (select-keys article [:author_id :title :slug :description :body])
          results        (jdbc/insert! db "\"article\"" article)
          new-article-id (-> results ffirst val)]
      (when (and new-article-id (seq tags))
        (jdbc/insert-multi! db "\"tag\""
          (mapv (fn [tag] {:article_id new-article-id :tag tag}) tags))
        new-article-id)))

  (destroy-article [{db :spec} author-id article-slug]
    (jdbc/delete! db "\"article\"" ["author_id = ? AND slug = ?" author-id article-slug]))

  (like [{db :spec} user-id article-slug]
    (when-let [article (first (jdbc/find-by-keys db "\"article\"" {:slug article-slug}))]
      (let [article-id (:id article)]
        (jdbc/execute! db [(str "INSERT INTO \"favorite\" (user_id, article_id)"
                             " SELECT ?, ?"
                             " WHERE NOT EXISTS (SELECT * FROM \"favorite\""
                             " WHERE user_id = ? AND article_id = ?)")
                           user-id article-id user-id article-id]))))

  (unlike [{db :spec} user-id article-slug]
    (when-let [article (first (jdbc/find-by-keys db "\"article\"" {:slug article-slug}))]
      (let [article-id (:id article)]
        (jdbc/delete! db "\"favorite\"" ["user_id = ? AND article_id = ?" user-id article-id]))))

  )
