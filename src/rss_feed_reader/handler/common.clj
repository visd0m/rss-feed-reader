(ns rss-feed-reader.handler.common
  (:require [clojure.spec.alpha :as s]))

(defn get-starting-after
  "extract field used as starting after to paginate entity, using starting after query params"
  [{starting-after "starting_after"} query-param-extractor]
  (if starting-after
    (-> starting-after
        (query-param-extractor))))

(defn get-limit
  "given a map of query param, extract limit if param is present
  it is capped on 100, if it is not present 20 is returned as default limit"
  [{limit "limit"}]
  (if-let [arg-limit limit]
    (-> arg-limit
        (java.lang.Integer/parseInt)
        (min 100)
        (max 1))
    20))

(defn get-paginated-list
  "get paginated list given a collection and a limit"
  [coll limit]
  {:has_more (> (count coll) limit)
   :data     (take limit coll)})