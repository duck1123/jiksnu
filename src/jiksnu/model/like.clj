(ns jiksnu.model.like
  (:use [jiksnu.transforms :only [set-_id set-created-time set-updated-time]]
        [slingshot.slingshot :only [throw+]]
        [validateur.validation :only [validation-set presence-of]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.user :as model.user]
            [monger.collection :as mc]
            [monger.result :as result])
  (:import jiksnu.model.Like))

(def collection-name "likes")

(def create-validators
  (validation-set
   (presence-of :_id)
   (presence-of :created)
   (presence-of :updated)
   (presence-of :user)
   (presence-of :activity)))

(defn prepare
  [record]
  (-> record
      set-_id
      set-created-time
      set-updated-time))

(defn drop!
  []
  (mc/remove collection-name))

(defn fetch-by-id
  [id]
  (if-let [like (mc/find-map-by-id collection-name id)]
    (model/map->Like like)))

(defn delete
  [like]
  (let [like (fetch-by-id (:_id (log/spy like)))]
    (mc/remove-by-id collection-name (:_id like))
    like))

(defn create
  [params & [options & _]]
  (let [params (prepare params)
        errors (create-validators params)]
    (if (empty? errors)
      (do
        (log/debugf "Creating like: %s" params)
        (let [result (mc/insert collection-name params)]
          (if (result/ok? result)
            (fetch-by-id (:_id params))
            (throw+ {:type :write-error :result result}))))
      (throw+ {:type :validation :errors errors}))))

;; TODO: get-like

;; FIXME: This is not quite right
(defn find-or-create
  [activity user]
  (create activity))

(defn fetch-all
  ([] (fetch-all {} {}))
  ([params] (fetch-all params {}))
  ([params opts]
     (map model/map->Like (mc/find-maps collection-name params))))

;; TODO: use index to get pagination
(defn fetch-by-user
  [user]
  (fetch-all {:user (:_id user)}))

(defn get-activity
  [like]
  (-> like :activity model.activity/fetch-by-id))

(defn get-actor
  [like]
  (-> like :user model.user/fetch-by-id))

;; TODO: fetch-by-activity
(defn get-likes
  [activity]
  (seq (fetch-all {:activity (:_id activity)})))

(defn count-records
  ([] (count-records {}))
  ([params]
     (mc/count collection-name)))

;; TODO: deprecated
(defn format-data
  "format a like for display in templates"
  [like]
  (let [user (model.user/fetch-by-id (:user like))]
    (:username user)))
