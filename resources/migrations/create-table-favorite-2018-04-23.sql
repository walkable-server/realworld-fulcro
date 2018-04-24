CREATE TABLE "favorite" (
 user_id INTEGER REFERENCES "user"("id"),
 article_id INTEGER REFERENCES "article"("id")
)
