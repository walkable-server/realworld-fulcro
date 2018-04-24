CREATE TABLE "tag" (
 tag TEXT,
 article_id INTEGER REFERENCES "article"("id")
)
