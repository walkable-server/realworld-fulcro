CREATE TABLE "comment" (
 id SERIAL PRIMARY KEY,
 body TEXT,
 created_at TIMESTAMP DEFAULT NOW(),
 updated_at TIMESTAMP DEFAULT NOW(),
 article_id INTEGER REFERENCES "article"("id"),
 author_id INTEGER REFERENCES "user"("id")
)
