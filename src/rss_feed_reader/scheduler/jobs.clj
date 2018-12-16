(ns rss-feed-reader.scheduler.jobs
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :refer :all :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [rss-feed-reader.rss-fetcher :refer :all :as rss-fetcher]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]))

(def jobs
  [{:job     (j/build
               (j/of-type (defjob rss-fetching-job
                            [_]
                            (rss-fetcher/fetch-all-subscriptions)))
               (j/with-identity (j/key "jobs.job.1")))
    :trigger (t/build
               (t/with-identity (t/key "triggers.1"))
               (t/start-now)
               (t/with-schedule (schedule
                                  (cron-schedule "0 */1 * * * ?"))))}])

(defn start-scheduler-with-jobs
  []
  "start quartz scheduler"
  (let [scheduler (qs/start (qs/initialize))]
    (doseq [job jobs]
      (qs/schedule scheduler (:job job) (:trigger job)))))

