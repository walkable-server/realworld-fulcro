(ns conduit.boundary.article
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol Article
  (create-article [db article])
  (destroy-article [db author-id article-slug])
  (update-article [db article])
  (find-article [db slug])
  (like [db user-id article-id])
  (unlike [db user-id article-id]))

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
  (destroy-article [db author-id article-slug]
    (jdbc/delete! db "\"article\"" ["author_id = ? AND article.slug = ?" author-id article-slug]))
  )
