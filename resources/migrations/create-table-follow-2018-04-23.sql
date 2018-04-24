CREATE TABLE "follow" (
 follower_id INTEGER REFERENCES "user"("id"),
 followee_id INTEGER REFERENCES "user"("id")
)
