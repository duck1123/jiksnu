(ns jiksnu.views.feed-source-views
  (:use [ciste.config :only [config]]
        [ciste.views :only [defview]]
        ciste.sections.default
        [clojurewerkz.route-one.core :only [named-path]]
        jiksnu.actions.feed-source-actions
        [jiksnu.ko :only [*dynamic*]]
        [jiksnu.sections :only [bind-to format-page-info pagination-links with-page]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.model.feed-source :as model.feed-source]
            [jiksnu.model.user :as model.user]
            [ring.util.response :as response])
  (:import jiksnu.model.Activity
           jiksnu.model.FeedSource))

;; index

(defview #'index :html
  [request {:keys [items] :as page}]
  {:title "Feed Sources"
   :body
   (let [items (if *dynamic* [(FeedSource.)] items)]
     (with-page "feed-sources"
       (pagination-links page)
       (bind-to "items"
         (doall (index-section items page)))))})

(defview #'index :viewmodel
  [request {:keys [items] :as page}]
  {:body {:title "Feed Sources"
          :pages {:default (format-page-info page)}}})

;; process-updates

(defview #'process-updates :html
  [request params]
  {:body params
   :template false})

;; unsubscribe

(defview #'unsubscribe :html
  [request params]
  (-> (named-path "index feed-sources")
      response/redirect-after-post
      (assoc :template false)))

;; show

(defview #'show :html
  [request item]
  (let [page (actions.activity/fetch-by-feed-source item)
        items (if *dynamic* [(Activity.)] (:items page))]
    {:body
     (bind-to "targetFeedSource"
       (show-section item)
       (with-page "activities"
         (pagination-links (if *dynamic* {} page))
         (bind-to "items"
           (index-section items))))}))

(defview #'show :model
  [request activity]
  {:body (show-section activity)})

(defview #'show :viewmodel
  [request item]
  {:body {:targetFeedSource (:_id item)
          :title (:title item)
          :pages {:activities
                  (let [page (actions.activity/fetch-by-feed-source item
                                                                    {:sort-clause {:updated 1}})]
                    (format-page-info page))}}})

;; update

(defview #'update :html
  [request params]
  (-> (named-path "index feed-sources")
      response/redirect-after-post
      (assoc :template false)))
