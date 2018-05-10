CREATE TABLE "user" (
 id SERIAL PRIMARY KEY,
 email TEXT UNIQUE,
 username TEXT UNIQUE,
 name TEXT,
 password TEXT,
 bio TEXT,
 image TEXT
)
