(ns jiksnu.transforms.activity-transforms
  (:use [ciste.config :only [config]]
        [jiksnu.session :only [current-user current-user-id is-admin?]]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojurewerkz.route-one.core :as r]
            [jiksnu.abdera :as abdera]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.feed-source :as model.feed-source]
            [jiksnu.model.user :as model.user]
            [lamina.core :as l]))

(defn set-local
  [activity]
  (assoc activity :local true))

(defn set-object-updated
  [activity]
  (if (:updated (:object activity))
    activity
    (assoc-in activity [:object :updated] (time/now))))

(defn set-object-created
  [activity]
  (if (:created (:object activity))
    activity
    (assoc-in activity [:object :created] (time/now))))

(defn set-url
  [activity]
  (if (and (:local activity)
           (empty? (:url activity)))
    (assoc activity :url (str "http://" (config :domain) "/notice/" (:_id activity)))
    activity))

(defn set-object-type
  [activity]
  (assoc-in
   activity [:object :object-type]
   (if-let [object-type (:object-type (:object activity))]
     (-> object-type
         ;; strip namespaces
         (string/replace #"http://onesocialweb.org/spec/1.0/object/" "")
         (string/replace #"http://activitystrea.ms/schema/1.0/" ""))
     "note")))

(defn set-parent
  [activity]
  (if (= (:parent activity) "")
    (dissoc activity :parent)
    activity))

(defn set-source
  [activity]
  (if (:update-source activity)
    activity
    (let [author (model.activity/get-author activity)
          source (model.feed-source/find-by-user author)]
      (assoc activity :update-source (:_id source)))))

(defn set-tags
  [activity]
  (let [tags (:tags activity )]
    (if (string? tags)
      (if (and tags (not= tags ""))
        (if-let [tag-seq (filter #(not= % "") (string/split tags #",\s*"))]
          (assoc activity :tags tag-seq)
          (dissoc activity :tags))
        (dissoc activity :tags))
      (if (coll? tags)
        activity
        (dissoc activity :tags)))))

(defn set-public
  [activity]
  (if (false? (:public activity))
    activity
    (assoc activity :public true)))

(defn set-geo
  [activity]
  (let [latitude (get activity "geo.latitude")
        longitude (get activity "geo.longitude")]
    (-> activity
        (dissoc "geo.latitude")
        (dissoc "geo.longitude")
        (assoc :geo {:latitude latitude
                     :longitude longitude}))))

(defn set-remote
  [activity]
  (if (:local activity)
    activity
    (assoc activity :local false)))

(defn set-object-id
  [activity]
  (if (:id (:object activity))
    activity
    (assoc-in activity [:object :id] (abdera/new-id))))

(defn set-title
  [activity]
  (if (empty? (:title activity))
    ;; TODO: strip down to plain text and limit to 140 characters
    (assoc activity :title (:content activity))
    activity))

(defn set-actor
  [activity]
  ;; TODO: Should we be allowing an author to be passed in?
  (if-let [author (or (:author activity)
                      (current-user-id))]
    (assoc activity :author author)))

;; TODO: This operation should be performed on local posts. Remote
;; posts without an id should be rejected
(defn set-id
  [activity]
  (if (empty? (:id activity))
    (if-let [user (model.activity/get-author activity)]
      (when (:local activity)
        (let [id (r/named-url "show activity" {:id (:_id activity)})]
          (assoc activity :id id)))
      (throw+ "Could not determine author"))
    activity))

(defn set-verb
  [activity]
  (if (:verb activity)
    activity
    (assoc activity :verb "post")))

;; TODO: this type of job should be done via triggers
(defn set-recipients
  "attempt to resolve the recipients"
  [activity]
  (let [uris (filter identity (:recipient-uris activity))]
    (if (empty? uris)
      (dissoc activity :recipient-uris)
      (let [users (->> uris
                       (keep #(:_id (actions.user/find-or-create-by-remote-id {:id %})))) ]
        (assoc activity :recipients users)))))

(defn set-conversation
  [item]
  (if (:conversation item)
    item
    (let [user (model.user/fetch-by-id (:author item))]
      (if (:local user)
        (assoc item :conversation (:_id (model/create-new-conversation)))
        (if-let [uri (first (:conversation-uris item))]
          (let [conversation (model/get-conversation uri)]
            (-> item
                (assoc :conversation (:_id conversation))
                (dissoc :conversation-uris)))
          item)))))

(defn set-mentioned
  [activity]
  (if-let [ids (->> activity
                    :mentioned-uris
                    (map
                     (fn [uri]
                       (let [resource (model/get-resource uri)]
                         (try
                           (:_id (actions.user/find-or-create-by-remote-id {:id uri}))
                           (catch RuntimeException ex
                             nil)))))
                    (filter identity)
                    seq)]
    (-> activity
        (assoc :mentioned ids)
        (dissoc :mentioned-uris))
    activity))

(defn set-resources
  [activity]
  (if-let [ids (->> activity
                    :enclosures
                    (map :href)
                    (map (fn [url]
                           (:_id (model/get-resource url))))
                    seq
                    doall)]
    (-> activity
        (assoc :resources ids)
        (dissoc :enclosures))
    activity))
