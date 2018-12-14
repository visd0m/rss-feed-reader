(ns rss-feed-reader.models.subscription
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [clojure.walk]
            [rss-feed-reader.models.db :as db]
            [rss-feed-reader.models.auto-complete :as auto-complete]))


(s/def ::insert-subscription (s/keys :req-un [::url]
                                     :opt-un [::id ::insert_date ::version ::update_date]))

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
   (log/info "loading subscription for id=" id)
   (first (sql/query conn ["select * from subscription where id = (?::uuid)" id]))))

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


