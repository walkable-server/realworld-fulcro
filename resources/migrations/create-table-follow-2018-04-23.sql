CREATE TABLE "follow" (
 follower_id INTEGER REFERENCES "user"("id") ON DELETE CASCADE,
 followee_id INTEGER REFERENCES "user"("id") ON DELETE CASCADE
)
