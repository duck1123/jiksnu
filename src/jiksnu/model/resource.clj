(ns jiksnu.model.resource
  (:use [ciste.config :only [config]]
        [clojure.core.incubator :only [-?>>]]
        [slingshot.slingshot :only [throw+]]
        [validateur.validation :only [validation-set presence-of acceptance-of]])
  (:require [clj-statsd :as s]
            [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [lamina.trace :as trace]
            [monger.collection :as mc]
            [monger.query :as mq]))

(defonce page-size 20)
(def collection-name "resources")

(def create-validators
  (validation-set
   (presence-of   :_id)
   (presence-of   :url)
   #_(acceptance-of :local         :accept (partial instance? Boolean))

   ;; TODO: These should be joda times
   (presence-of   :created)
   (presence-of   :updated)))

(defn fetch-all
  ([] (fetch-all {}))
  ([params] (fetch-all params {}))
  ([params options]
     (let [sort-clause (mq/partial-query (mq/sort (:sort-clause options)))
           records (mq/with-collection collection-name
                     (mq/find params)
                     (merge sort-clause)
                     (mq/paginate :page (:page options 1)
                                  :per-page (:page-size options 20)))]
       (map model/map->Resource records))))

(defn fetch-by-url
  [url]
  (first (:items (fetch-all {:url url}))))

(defn set-field!
  "Updates resource's field to value"
  [item field value]
  (log/debugf "setting %s (%s = %s)" (:_id item) field value)
  (s/increment "resources field set")
  (mc/update collection-name
             {:_id (:_id item)}
             {:$set {field value}}))

(defn fetch-by-id
  [id]
  (let [id (if (string? id) (model/make-id id) id)]
    (s/increment "resources fetched")
    (if-let [item (mc/find-map-by-id collection-name id)]
      (model/map->Resource item))))

(defn create
  [params]
  (let [errors (create-validators params)]
    (if (empty? errors)
      (do
        (log/debugf "Creating resource: %s" (pr-str params))
        (mc/insert collection-name params)
        (let [item (fetch-by-id (:_id params))]
          (trace/trace :resource:created item)
          item))
      (throw+ {:type :validation :errors errors}))))

(defn drop!
  []
  (mc/remove collection-name))

(defn delete
  [item]
  (mc/remove-by-id collection-name (:_id item))
  item)

(defn count-records
  ([] (count-records {}))
  ([params]
     (mc/count collection-name params)))
