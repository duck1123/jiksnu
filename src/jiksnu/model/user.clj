(ns jiksnu.model.user
  (:use (ciste config
               [debug :only [spy]])
        [clj-gravatar.core :only [gravatar-image]]
        jiksnu.model)
  (:require (jiksnu [abdera :as abdera]
                    [namespace :as namespace])
            [clojure.string :as string]
            (clojure.tools [logging :as log])
            (clj-tigase [core :as tigase]
                        [element :as element])
            [karras.entity :as entity]
            [jiksnu.model.domain :as model.domain])
  (:import jiksnu.model.Domain
           jiksnu.model.User
           tigase.xmpp.BareJID
           tigase.xmpp.JID))

(defn salmon-link
  [user]
  (str "http://" (:domain user) "/main/salmon/user/" (:_id user)))

;; TODO: Move this to actions and make it find-or-create
(defn get-domain
  [^User user]
  (model.domain/fetch-by-id (:domain user)))

(defn local?
  [^User user]
  (or (:local user)
      (:local (get-domain user))
      ;; TODO: remove this clause
      (= (:domain user) (config :domain))))

(defn get-uri
  ([^User user] (get-uri user true))
  ([^User user use-scheme?]
     (str (when use-scheme? "acct:") (:username user) "@" (:domain user))))

(defn uri
  "returns the relative path to the user's profile page"
  [user]
  (if (local? user)
    (str "/" (:username user))
    (str "/remote-user/" (get-uri user false))))

(defn full-uri
  "The fully qualified path to the user's profile page on this site"
  [user]
  (str "http://" (config :domain) (uri user)))

(defn rel-filter
  "returns all the links in the collection where the rel value matches the
   supplied value"
  [rel links content-type]
  (filter (fn [link]
            (and (= (:rel link) rel)
                 (= (:type link) content-type)))
          links))

(defn split-uri
  "accepts a uri in the form of username@domain or scheme:username@domain and
   returns a vector containing both the username and the domain"
  [uri]
  (let [[_ _ username domain] (re-find #"(.*:)?([^@]+)@([^\?]+)" uri)]
    (when (and username domain) [username domain])))

(defn display-name
  [^User user]
  (or (:display-name user)
      (when (and (:first-name user) (:last-name user))
        (str (:first-name user) " " (:last-name user)))
      (get-uri user)))

(defn get-link
  [user rel content-type]
  (first (rel-filter rel (:links user) content-type)))

(defn drop!
  []
  (entity/delete-all User))

(defn create
  [user]
  (if user
    (let [id (make-id)
          {:keys [username domain]} user]
      (if (and (and username (not= username ""))
               (and domain (not= domain "")))
        (do
          (log/debugf "Creating user: %s@%s" username domain)
          (entity/create User (assoc user :_id id)))
        (throw (IllegalArgumentException.
                "Users must contain both a username and a domain"))))
    (throw (IllegalArgumentException. "Can not create nil users"))))

(defn fetch-all
  "Fetch all users"
  ([] (fetch-all {}))
  ([params & options]
     (apply entity/fetch User params options))) 

(defn get-user
  "Find a user by username and domain"
  ([username] (get-user username (config :domain)))
  ([username domain]
     (entity/fetch-one
      User
      {:username username
       :domain domain})))

(defn fetch-by-id
  "Fetch a user by it's object id"
  [id]
  (when id
    (try
      (entity/fetch-by-id User id)
      (catch IllegalArgumentException ex
        ;; Invalid ObjectID simply returning nil
        ))))

;; deprecated
;; TODO: Split the jid into it's parts and fetch.
(defn fetch-by-jid
  [jid]
  (get-user (.getLocalpart jid)
            (.getDomain jid)))

(defn set-field
  "Updates user's field to value"
  [user field value]
  (entity/find-and-modify
   User
   {:_id (:_id user)}
   {:$set {field value}}))

(defn fetch-by-uri
  "Fetch user by their acct uri"
  [uri]
  (apply get-user (split-uri uri)))

(defn fetch-by-remote-id
  "Fetch user by their id value"
  [uri]
  (entity/fetch-one User {:id uri}))

;; TODO: Is this needed?
(defn subnodes
  [^BareJID user subnode]
  (let [id (tigase/get-id user)
        domain (tigase/get-domain user)]
    (:nodes (get-user id))))

;; TODO: Should accept a user
(defn delete
  [id]
  (entity/delete (fetch-by-id id)))

;; TODO: Is this needed?
(defn add-node
  [^User user name]
  (entity/update User
                 {:_id (tigase/get-id user)}))

(defn update
  [^User new-user]
  (let [old-user (get-user (:username new-user) (:domain new-user))
        merged-user (merge {:admin false}
                           old-user new-user)
        user (entity/make User merged-user)]
    (entity/update User {:_id (:_id old-user)} (dissoc user :_id))
    user))

;; TODO: move part of this to domains
(defn user-meta-uri
  [^User user]
  (if-let [domain (get-domain user)]
    (if-let [lrdd-link (get-link domain "lrdd" nil)]
      (let [template (:template lrdd-link)]
        (string/replace template "{uri}" (get-uri user)))
      (throw (RuntimeException. "could not find lrdd link")))
    (throw (RuntimeException. "could not determine domain"))))

(defn image-link
  [user]
  (or (:avatar-url user)
      (when (:email user) (gravatar-image (:email user)))
      (gravatar-image (get-uri user false))))

(defn vcard-request
  [user]
  (let [body (element/make-element
              "query" {"xmlns" namespace/vcard-query})
        packet-map {:from (tigase/make-jid "" (config :domain))
                    :to (tigase/make-jid user)
                    :id "JIKSNU1"
                    :type :get
                    :body body}]
    (tigase/make-packet packet-map)))
