# Clojure telegram bot rss feeds collector/dispenser

**!!disclaimer!! Clojure learning project**

Simple clojure Rss feeds collector.
Collected feeds a re stored on a postgres database and exposed through rest apis served by a web server.
New feeds are sent to subscribed user through telegram bot apis.

#### Project highlights:
- jdbc connection to potgres database
- quartzite jobs
- telegram bot apis
- rest apis exposed through ring
- migratus to handle database migrations
