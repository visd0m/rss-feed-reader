(ns rss-feed-reader.config.jobs
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :refer :all :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [rss-feed-reader.jobs.rss-fetching-job :as rss-fetcher]
            [rss-feed-reader.jobs.telegram-polling-job :as telegram-polling]
            [rss-feed-reader.jobs.old-feed-items-eraser :as old-feed-item-eraser]
            [rss-feed-reader.jobs.telegram-push-job :as telegram-push]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]))

(def jobs [{:job     (j/build
                       (j/of-type (defjob fetch-rss-feeds
                                    [_]
                                    (rss-fetcher/fetch-all-feeds)))
                       (j/with-identity (j/key "jobs.job.1")))
            :trigger (t/build
                       (t/with-identity (t/key "triggers.1"))
                       (t/start-now)
                       (t/with-schedule (schedule
                                          (cron-schedule "0 */1 * * * ?"))))}
           {:job     (j/build
                       (j/of-type (defjob push-news-telegram
                                    [_]
                                    (telegram-push/push-news)))
                       (j/with-identity (j/key "jobs.job.2")))
            :trigger (t/build
                       (t/with-identity (t/key "triggers.2"))
                       (t/start-now)
                       (t/with-schedule (schedule
                                          (cron-schedule "*/30 * * * * ?"))))}
           {:job     (j/build
                       (j/of-type (defjob fetch-telegram-commands
                                    [_]
                                    (telegram-polling/fetch-commands 0)))
                       (j/with-identity (j/key "jobs.job.3")))
            :trigger (t/build
                       (t/with-identity (t/key "triggers.3"))
                       (t/start-now)
                       (t/with-schedule (schedule
                                          (cron-schedule "*/4 * * * * ?"))))}])

(defn start-scheduler-with-jobs
  []
  "start quartz scheduler"
  (let [scheduler (qs/start (qs/initialize))]
    (doseq [job jobs]
      (qs/schedule scheduler (:job job) (:trigger job)))))