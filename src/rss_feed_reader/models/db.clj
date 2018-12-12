(ns rss-feed-reader.models.db)

(def db-name
  "rss-feed-reader")

(def db-user
  "rss-feed-reader-user")

(def db-url
  "postgresql://localhost:5432/" db-name)

(def pg-db {:dbtype   "postgresql"
            :dbname   db-name
            :host     "localhost"
            :user     db-user
            :password ""})

