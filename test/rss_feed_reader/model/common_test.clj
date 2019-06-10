(ns rss-feed-reader.model.common-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.model.common :as model-common])
  (:import (java.util UUID)
           (java.sql Timestamp)
           (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn- get-complete-entity
  []
  {:id           (UUID/randomUUID)
   :insert-date  (Timestamp/from (.minus (Instant/now) 10 ChronoUnit/MINUTES))
   :update-date  (Timestamp/from (.minus (Instant/now) 10 ChronoUnit/MINUTES))
   :order-unique (.toEpochMilli (Instant/now))
   :version      0})

(deftest should-autocomplete-with-id
  (testing "should autocomplete with id entity with missing id"
    (let [entity (dissoc (get-complete-entity) :id)
          autocompleted-with-id (model-common/autocomplete-id entity)]

      (is (contains? autocompleted-with-id :id))
      (is (= (.getClass (:id autocompleted-with-id)) UUID))
      (for [[key value] entity]
        (is (= (get autocompleted-with-id key) value))))))

(deftest should-not-autocomplete-with-id
  (testing "should not autocomplete with id entity with already an id"
    (let [entity (get-complete-entity)
          autocompleted-with-id (model-common/autocomplete-id entity)]

      (is (contains? autocompleted-with-id :id))
      (is (= (.getClass (:id autocompleted-with-id)) UUID))
      (for [[key value] entity]
        (is (= (get autocompleted-with-id key) value))))))

; ===

(deftest should-autocomplete-with-insert-date
  (testing "should autocomplete with insert date"
    (let [entity (dissoc (get-complete-entity) :insert-date)
          entity-with-insert-date (model-common/autocomplete-insert-date entity)]
      (is (contains? entity-with-insert-date :id))
      (is (contains? entity-with-insert-date :insert-date))
      (is (= (.getClass (:insert-date entity-with-insert-date)) Timestamp))
      (for [[key value] entity]
        (is (= (get entity-with-insert-date key) value))))))

(deftest should-not-autocomplete-with-insert-date
  (testing "should not autocomplete with insert date entity with already insert date"
    (let [entity (get-complete-entity)
          entity-with-insert-date (model-common/autocomplete-insert-date entity)]
      (is (contains? entity-with-insert-date :id))
      (is (contains? entity-with-insert-date :insert-date))
      (is (= (.getClass (:insert-date entity-with-insert-date)) Timestamp))
      (for [[key value] entity]
        (is (= (get entity-with-insert-date key) value))))))

; ===

(deftest should-autocomplete-with-update
  (testing "should autocomplete with update date"
    (let [entity (dissoc (get-complete-entity) :update-date)
          entity-with-update-date (model-common/autocomplete-update-date entity)]
      (is (contains? entity-with-update-date :update-date))
      (is (= (.getClass (:update-date entity-with-update-date)) Timestamp))
      (for [[key value] entity]
        (is (= (get entity-with-update-date key) value))))))

(deftest should-not-autocomplete-with-update
  (testing "should not autocomplete with update date entity with already update date"
    (let [entity (get-complete-entity)
          entity-with-update-date (model-common/autocomplete-update-date entity)]
      (is (contains? entity-with-update-date :update-date))
      (is (= (.getClass (:update-date entity-with-update-date)) Timestamp))
      (for [[key value] entity]
        (is (= (get entity-with-update-date key) value))))))

; todo test auto-complete version
; todo test auto-complete order unique

; ===

(deftest should-get-sql-insert
  "should get sql insert"
  (let [entity (get-complete-entity)
        table "the_table"
        custom-types-by-key {:id "uuid"}
        sql-insert (model-common/to-sql-insert entity table custom-types-by-key)
        expected (str "insert into the_table (id,insert_date,update_date,order_unique,version) values (('" (:id entity) "'::uuid),'" (:insert-date entity) "','" (:update-date entity) "','" (:order-unique entity) "','" (:version entity) "')")]
    (is (= sql-insert expected))))

(deftest should-get-sql-update-skip-null
  "should get sql update skip null"
  (let [entity (assoc (get-complete-entity) :version 1)
        table "the_table"
        custom-types-by-key {:id "uuid"}
        key :id
        sql-update-skip-null (model-common/to-sql-update-skip-null entity table custom-types-by-key key)
        expected (str "update the_table set insert_date='" (:insert-date entity) "',update_date='" (:update-date entity) "',order_unique='" (:order-unique entity) "',version='" (:version entity) "' where id=('" (get entity key) "'::uuid) and version=" (- (:version entity) 1))]
    (is (= sql-update-skip-null expected))))
