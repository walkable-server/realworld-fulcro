CREATE TABLE "article" (
 id SERIAL PRIMARY KEY,
 slug TEXT UNIQUE,
 title TEXT,
 description TEXT,
 image TEXT,
 body TEXT,
 created_at TIMESTAMP DEFAULT NOW(),
 updated_at TIMESTAMP DEFAULT NOW(),
 author_id INTEGER REFERENCES "user"("id") ON DELETE CASCADE
)
