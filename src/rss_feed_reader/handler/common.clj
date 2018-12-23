(ns rss-feed-reader.handler.common)

(defn get-starting-after
  "extract field used as starting after to paginate entity, using starting after query params"
  [{starting-after "starting_after"} query-param-extractor]
  (when starting-after
    (query-param-extractor starting-after)))

(defn get-limit
  "given a map of query param, extract limit if param is present
  it is capped on 100, if it is not present 20 is returned as default limit"
  [{limit "limit"}]
  (if limit
    (-> limit
        (java.lang.Integer/parseInt)
        (min 100)
        (max 1))
    20))

(defn get-paginated-list
  "get paginated list given a collection and a limit"
  [coll limit]
  {:has_more (> (count coll) limit)
   :data     (take limit coll)})