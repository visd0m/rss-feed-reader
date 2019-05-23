(ns rss-feed-reader.config.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def db-name "rss-feed-reader")

(def db-user "rss-feed-reader-user")

(def db-config {:dbtype      "postgresql"
                :classname   "org.postgresql.Driver"
                :subprotocol "postgresql",
                :subname     db-name
                :user        db-user
                :password    ""})

(defn pool
  [db-config]
  (let [connection-pool (doto (ComboPooledDataSource.)
                          (.setDriverClass (:classname db-config))
                          (.setJdbcUrl (str "jdbc:" (:subprotocol db-config) ":" (:subname db-config)))
                          (.setUser (:user db-config))
                          (.setMaxPoolSize 10)
                          (.setMinPoolSize 10)
                          (.setInitialPoolSize 10))]
    {:datasource connection-pool}))

(def pooled-db (delay (pool db-config)))

(defn db-connection
  []
  @pooled-db)