(ns jiksnu.model.domain
  (:use [ciste.config :only [config]]
        [clojure.core.incubator :only [-?>>]]
        [jiksnu.transforms :only [set-updated-time set-created-time]]
        [jiksnu.validators :only [type-of]]
        [slingshot.slingshot :only [throw+]]
        [validateur.validation :only [acceptance-of presence-of valid? validation-set]])
  (:require [clj-statsd :as s]
            [clj-tigase.core :as tigase]
            [clj-tigase.element :as element]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [jiksnu.templates :as templates]
            [jiksnu.util :as util]
            [monger.collection :as mc]
            [monger.core :as mg]
            [ring.util.codec :as codec])
  (:import jiksnu.model.Domain
           org.bson.types.ObjectId
           org.joda.time.DateTime))

(def collection-name "domains")
(def maker           #'model/map->Domain)
(def page-size       20)

(defn host-meta-link
  [domain]
  (list
   (str "https://" (:_id domain) "/.well-known/host-meta")
   (str "http://" (:_id domain) "/.well-known/host-meta")))

(defn statusnet-url
  [domain]
  (str "http://" (:_id domain) (:context domain) "/api/statusnet/config.json"))

(def create-validators
  (validation-set
   (type-of :_id        String)
   (type-of :created    DateTime)
   (type-of :updated    DateTime)
   (type-of :local      Boolean)
   (type-of :discovered Boolean)))

(def count-records (templates/make-counter       collection-name))
(def delete        (templates/make-deleter       collection-name))
(def drop!         (templates/make-dropper       collection-name))
(def remove-field! (templates/make-remove-field! collection-name))
(def set-field!    (templates/make-set-field!    collection-name))
(def fetch-by-id   (templates/make-fetch-by-id   collection-name maker false))
(def create        (templates/make-create        collection-name #'fetch-by-id #'create-validators))
(def fetch-all     (templates/make-fetch-fn      collection-name maker))

(defn get-link
  [item rel content-type]
  (first (util/rel-filter rel (:links item) content-type)))

;; TODO: add the links to the list
(defn add-links
  [domain links]
  ;; TODO: This should push only if the link is not yet there
  (mc/update collection-name
    (select-keys domain #{:_id})
    {:$pushAll {:links links}}))

(defn ping-request
  [domain]
  {:type :get
   :to (tigase/make-jid "" (:_id domain))
   :from (tigase/make-jid "" (config :domain))
   :body (element/make-element ["ping" {"xmlns" "urn:xmpp:ping"}])})

(defn get-xrd-url
  [domain user-uri]
  (when user-uri
    (when-let [template (or (:xrdTemplate domain)
                            (-?>> domain
                                  :links
                                  (filter #(= (:rel %) "lrdd"))
                                  (filter #(or (nil? (:type %))
                                               (= (:type %) "application/xrd+xml")))
                                  first
                                  :template))]
      (util/replace-template template user-uri))))

(defn get-jrd-url
  [domain user-uri]
  (when user-uri
    (when-let [template (or (:jrdTemplate domain)
                            (-?>> domain
                                  :links
                                  (filter #(= (:rel %) "lrdd"))
                                  (filter #(= (:type %) "application/json"))
                                  first
                                  :template))]
      (util/replace-template template user-uri))))
