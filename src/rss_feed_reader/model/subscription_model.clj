(ns rss-feed-reader.model.subscription-model
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [clojure.walk]
            [rss-feed-reader.config.db :as db]
            [rss-feed-reader.model.auto-complete :as auto-complete]))


(s/def ::insert-subscription (s/keys :req-un [::url]
                                     :opt-un [::id ::insert-date ::version ::update-date]))

; ==== load

(defn all
  "get list of all subscriptions"
  []
  (log/info "getting all subscriptions")
  (into [] (sql/query (db/db-connection) ["select * from subscription order by insert_date desc"])))

(defn by-id
  "get subscription by id"
  ([id]
   (by-id id (db/db-connection)))
  ([id conn]
   (log/info "loading subscription by id=" id)
   (first (sql/query conn ["select * from subscription where id = (?::uuid)" id]))))

(defn by-url
  "get subscriptions by url"
  ([url]
   (by-url url (db/db-connection)))
  ([url conn]
   (log/info "loading subscriptions by url=" url)
   (sql/query conn ["select * from subscription where url = ?" url])))

; ==== insert

(defn autocomplete-subscription-insert
  "auto complete missing field of subscription"
  [subscription]
  (-> subscription
      (auto-complete/autocomplete-id)
      (auto-complete/autocomplete-insert-date)
      (auto-complete/autocomplete-version)))

(defn insert
  "insert given subscription if valid"
  [subscription]
  {:pre [(s/valid? ::insert-subscription subscription)]}

  (let [autocompleted-subscription (autocomplete-subscription-insert subscription)]
    (log/info "creating subscription=" autocompleted-subscription)
    (sql/with-db-transaction [conn (db/db-connection)]
                             (sql/execute! conn ["insert into subscription values((?::uuid),?,?,?,?)"
                                                 (:id autocompleted-subscription)
                                                 (:url autocompleted-subscription)
                                                 (:insert_date autocompleted-subscription)
                                                 (:update_date autocompleted-subscription)
                                                 (:version autocompleted-subscription)])
                             (by-id (:id autocompleted-subscription) conn))))


