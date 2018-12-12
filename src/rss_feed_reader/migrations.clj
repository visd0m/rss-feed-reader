(ns rss-feed-reader.migrations
  (:require [rss-feed-reader.models.db :refer :all]))

(def config {:store                :database,
             :migration-dir        "migrations/",
             :init-script          "init.sql",
             :init-in-transaction? false,
             :db                   {:classname   "org.postgresql.Driver",
                                    :subprotocol "postgresql",
                                    :subname     db-url,
                                    :user        db-user,
                                    :password    ""}})