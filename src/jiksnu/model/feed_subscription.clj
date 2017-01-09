(ns jiksnu.model.feed-subscription
  (:require [jiksnu.db :as db]
            [jiksnu.model :as model]
            [jiksnu.templates.model :as templates.model]
            [jiksnu.validators :refer [type-of]]
            [monger.collection :as mc]
            [validateur.validation :refer [acceptance-of presence-of valid? validation-set]])
  (:import (org.joda.time DateTime)
           (org.bson.types ObjectId)))

(def collection-name "feed_subscriptions")
(def maker           #'model/map->FeedSubscription)
(def page-size       20)

(def create-validators
  (validation-set
   (type-of :_id ObjectId)
   ;; (type-of :url      String)
   ;; (type-of :callback String)
   ;; (type-of :domain   String)
   ;; (type-of :local    Boolean)
   (type-of :created DateTime)
   (type-of :updated DateTime)))

(def count-records (templates.model/make-counter       collection-name))
(def delete        (templates.model/make-deleter       collection-name))
(def drop!         (templates.model/make-dropper       collection-name))
(def remove-field! (templates.model/make-remove-field! collection-name))
(def set-field!    (templates.model/make-set-field!    collection-name))
(def fetch-by-id   (templates.model/make-fetch-by-id   collection-name maker))
(def create        (templates.model/make-create        collection-name #'fetch-by-id #'create-validators))
(def fetch-all     (templates.model/make-fetch-fn      collection-name maker))

(defn fetch-by-topic
  "Fetch a single source by it's topic id"
  [topic]
  (fetch-all {:topic topic}))

(defn ensure-indexes
  []
  (doto collection-name
    (mc/ensure-index (db/get-connection) {:url 1 :callback 1} {:unique true})))
