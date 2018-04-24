CREATE TABLE "favorite" (
 user_id INTEGER REFERENCES "user"("id") ON DELETE CASCADE,
 article_id INTEGER REFERENCES "article"("id") ON DELETE CASCADE
)
