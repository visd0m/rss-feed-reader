(ns rss-feed-reader.model.common-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.model.common :as model-common])
  (:import (java.util UUID)
           (java.sql Timestamp)))

(deftest should-autocomplete-with-id
  (testing "should autocomplete with id entity with missing id"
    (let [entity {:a_field       "a_field"
                  :another_field "another_field"}
          autocompleted-with-id (model-common/autocomplete-id entity)]

      (is (contains? autocompleted-with-id :id))
      (is (= (.getClass (:id autocompleted-with-id)) UUID)))))

(deftest should-not-autocomplete-with-id
  (testing "should not autocomplete with id entity with already an id"
    (let [id (UUID/randomUUID)
          entity {:id            id
                  :a_field       "a_field"
                  :another_field "another_field"}
          autocompleted-with-id (model-common/autocomplete-id entity)]

      (is (contains? autocompleted-with-id :id))
      (is (= (.getClass (:id autocompleted-with-id)) UUID))
      (is (= id (:id autocompleted-with-id))))))

; ===

(deftest should-autocomplete-with-update
  (testing "should autocomplete with update date"
    (let [entity {:id            (UUID/randomUUID)
                  :a_field       "a_field"
                  :another_field "another_field"}
          entity-with-update-date (model-common/autocomplete-update-date entity)]
      (is (contains? entity-with-update-date :update-date))
      (is (= (.getClass (:update-date entity-with-update-date)) Timestamp)))))