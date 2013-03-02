(ns jiksnu.actions.user-actions
  (:use [ciste.config :only [config]]
        [ciste.core :only [defaction]]
        [ciste.initializer :only [definitializer]]
        [ciste.loader :only [require-namespaces]]
        [clojure.core.incubator :only [-?> -?>>]]
        [jiksnu.actions :only [invoke-action]]
        [lamina.executor :only [task]]
        [slingshot.slingshot :only [throw+]])
  (:require [aleph.http :as http]
            [ciste.model :as cm]
            [clj-statsd :as s]
            [clj-tigase.core :as tigase]
            [clj-tigase.element :as element]
            [clj-tigase.packet :as packet]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lamina.core :as l]
            [jiksnu.abdera :as abdera]
            [jiksnu.actions.auth-actions :as actions.auth]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.key-actions :as actions.key]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.actions.webfinger-actions :as actions.webfinger]
            [jiksnu.channels :as ch]
            [jiksnu.helpers.user-helpers :as helpers.user]
            [jiksnu.model :as model]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.key :as model.key]
            [jiksnu.model.user :as model.user]
            [jiksnu.model.webfinger :as model.webfinger]
            [jiksnu.namespace :as ns]
            [jiksnu.ops :as ops]
            [jiksnu.session :as session]
            [jiksnu.templates :as templates]
            [jiksnu.transforms :as transforms]
            [jiksnu.transforms.user-transforms :as transforms.user]
            [jiksnu.util :as util]
            [monger.collection :as mc]
            [plaza.rdf.core :as rdf]
            [plaza.rdf.sparql :as sp])
  (:import java.net.URI
           jiksnu.model.User
           org.apache.abdera2.model.Person
           tigase.xmpp.JID))

(defonce delete-hooks (ref []))

(defn prepare-delete
  ([item]
     (prepare-delete item @delete-hooks))
  ([item hooks]
     (if (seq hooks)
       (recur ((first hooks) item) (rest hooks))
       item)))

(defn prepare-create
  [user]
  (-> user
      transforms/set-_id
      transforms/set-updated-time
      transforms/set-created-time
      transforms.user/set-domain
      transforms.user/set-id
      transforms.user/set-url
      transforms.user/set-local
      transforms.user/assert-unique
      transforms.user/set-update-source
      transforms.user/set-discovered
      transforms.user/set-avatar-url))

(defn get-domain
  "Return the domain of the user"
  [^User user]
  (if-let [domain-id (or (:domain user)
                         (when-let [id (:id user)]
                           (util/get-domain-name id)))]
    (actions.domain/get-discovered {:_id domain-id})))


(defaction add-link*
  [user link]
  (mc/update "users" {:_id (:_id user)}
    {:$addToSet {:links link}})
  user)

(defn add-link
  [user link]
  (if-let [existing-link (model.user/get-link user
                                              (:rel link)
                                              (:type link))]
    user
    (add-link* user link)))






(defaction create
  [params]
  (let [item (prepare-create params)]
    (model.user/create item)))

(defaction find-or-create
  [username domain]
  (or (model.user/get-user username domain)
      (create {:username username :domain domain})))

(defn get-user-meta
  "Returns an enlive document for the user's xrd file"
  [user]
  ;; {:pre [(instance? User user)]}
  (if-let [url (:user-meta-link user)]
    (let [resource (actions.resource/find-or-create {:url url})
          response (actions.resource/update* resource)]
      (if-let [body (:body response)]
        (cm/string->document body)))
    (throw+ "User does not have a meta link")))

(defn get-username
  "Given a url, try to determine the username of the owning user"
  [params]
  ;; {:pre [(instance? User params)]}
  (let [id (or (:id params)
               (:url params))
        uri (URI. id)]
    (if (= "acct" (.getScheme uri))
      (assoc params :username (first (util/split-uri id)))
      (or (if-let [username (.getUserInfo uri)]
            (assoc params :username username))
          (if-let [domain-name (or (:domain params)
                                   (util/get-domain-name id))]
            (let [domain (actions.domain/find-or-create {:_id domain-name})
                  params (assoc params :domain domain-name)
                  user-meta-link (actions.domain/get-user-meta-url domain id)
                  params (assoc params :user-meta-link user-meta-link)]
              (if-let [xrd (get-user-meta params)]
                (let [source (model.webfinger/get-feed-source-from-xrd xrd)]
                  (merge params
                         {:username (model.webfinger/get-username-from-xrd xrd)
                          :update-source (:_id source)}))
                (throw+ "could not get user meta")))
            (throw+ "Could not determine domain name"))))))

(defn get-user-meta-uri
  [user]
  {:pre [(instance? User user)]}
  (let [domain (get-domain user)]
    (or (:user-meta-uri user)
        ;; TODO: should update uri in this case
        (actions.domain/get-user-meta-url domain (:url user)))))

(defn find-or-create-by-remote-id
  ([user] (find-or-create-by-remote-id user {}))
  ([params options]
     (if-let [id (:id params)]
       (if-let [domain (get-domain params)]
         (if-let [domain (if (:discovered domain)
                           domain (actions.domain/discover domain id))]
           (let [params (assoc params :domain (:_id domain))]
             (or (model.user/fetch-by-remote-id id)
                 (let [params (if (:username params)
                                params
                                (get-username params))]
                   (create params))))
           ;; this should never happen
           (throw+ "domain has not been disovered"))
         (throw+ "could not determine domain"))
       (throw+ "User does not have an id"))))

(defn find-or-create-by-uri
  [uri]
  {:pre [(string? uri)]}
  (let [[username domain] (util/split-uri uri)]
    (find-or-create username domain)))

(defn split-jid
  [^JID jid]
  [(tigase/get-id jid) (tigase/get-domain jid)])

;; TODO: This is the job of the filter
(defn find-or-create-by-jid
  [^JID jid]
  {:pre [(instance? JID jid)]}
  (apply find-or-create (split-jid jid)))

(defaction delete
  "Delete the user"
  [^User user]
  ;; {:pre [(instance? User user)]}
  (if-let [user (prepare-delete user)]
    (do (model.user/delete user)
        user)
    (throw+ "prepare delete failed")))

(defaction exists?
  [user]
  ;; {:pre [(instance? User user)]}
  ;; TODO: No need to actually fetch the record
  (model.user/fetch-by-id (:_id user)))

(def index*
  (templates/make-indexer 'jiksnu.model.user
                          :sort-clause {:username 1}))

(defaction index
  [& options]
  (apply index* options))

(defaction profile
  [& _]
  (cm/implement))

(defaction user-meta
  "returns a user matching the uri"
  [user]
  (if (model.user/local? user)
    (let [full-uri (model.user/full-uri user)]
      {:subject (model.user/get-uri user)
       :alias full-uri
       :links
       [
        {:rel ns/wf-profile
         :type "text/html"
         :href full-uri}

        {:rel ns/hcard
         :type "text/html"
         :href full-uri}

        {:rel ns/xfn
         :type "text/html"
         :href full-uri}

        {:rel ns/updates-from
         :type "application/atom+xml"
         ;; TODO: use formatted-uri
         :href (str "http://" (config :domain) "/api/statuses/user_timeline/" (:_id user) ".atom")}

        {:rel ns/updates-from
         :type "application/json"
         :href (str "http://" (config :domain) "/api/statuses/user_timeline/" (:_id user) ".json")}

        {:rel "describedby"
         :type "application/rdf+xml"
         :href (str full-uri ".rdf")}

        {:rel "salmon"          :href (model.user/salmon-link user)}
        {:rel ns/salmon-replies :href (model.user/salmon-link user)}
        {:rel ns/salmon-mention :href (model.user/salmon-link user)}
        {:rel ns/oid-provider   :href full-uri}
        {:rel ns/osw-service    :href (str "xmpp:" (:username user) "@" (:domain user))}


        {:rel "magic-public-key"
         :href (-> user
                   model.key/get-key-for-user
                   model.key/magic-key-string)}

        {:rel ns/ostatus-subscribe
         :template (str "http://" (config :domain) "/main/ostatussub?profile={uri}")}


        {:rel ns/twitter-username
         :href (str "http://" (config :domain) "/api/")
         :property [{:type "http://apinamespace.org/twitter/username"
                     :value (:username user)}]}]})
    (throw+ "Not authorative for this resource")))

;; TODO: This is a special case of the discover action for users that
;; support xmpp discovery
(defn request-vcard!
  "Send a vcard request to the xmpp endpoint of the user"
  [user]
  (let [packet (model.user/vcard-request user)]
    (tigase/deliver-packet! packet)))

(defaction update
  "Update the user's activities and information."
  [user params]
  (invoke-action "feed-source" "update" (str (:update-source user)))
  user)

(defn parse-person
  [^Person person]
  {:id (abdera/get-simple-extension person ns/atom "id")
   :email (.getEmail person)
   :url (str (.getUri person))
   :name (abdera/get-name person)
   :note (abdera/get-note person)
   :username (abdera/get-username person)
   :local-id (-> person
                 (abdera/get-extension-elements ns/statusnet "profile_info")
                 (->> (map #(abdera/attr-val % "local_id")))
                 first)
   :links (abdera/get-links person)})

;; TODO: This function should be called at most once per user, per feed
(defn person->user
  "Extract user information from atom element"
  [^Person person]
  (log/info "converting person to user")
  (let [{:keys [id username url links note email local-id]
         :as params} (parse-person person)
         domain-name (util/get-domain-name (or id url))
         domain (actions.domain/get-discovered {:_id domain-name})
         username (or username (get-username {:id id}))]
    (if (and username domain)
      (let [user-meta (actions.domain/get-user-meta-url domain url)
            user (merge params
                        {:domain domain-name
                         :id (or id url)
                         :user-meta-link user-meta
                         :username username})]
        (model/map->User user))
      (throw+ "could not determine user"))))

(defn fetch-user-meta
  "returns a user meta document"
  [^User user]
  (if-let [uri (model.user/user-meta-uri user)]
    (let [resource (ops/get-resource uri)]
      (actions.webfinger/fetch-host-meta uri))
    (throw (RuntimeException. "Could not determine user-meta link"))))

(defn fetch-user-feed
  "returns a feed"
  [^User user]
  (if-let [url (model.user/feed-link-uri user)]
    (let [resource (actions.resource/find-or-create {:url url})
          response (actions.resource/update* resource)]
      (abdera/parse-xml-string (:body response)))
    (throw+ "Could not determine url")))

;; TODO: Collect all changes and update the user once.
(defaction update-usermeta
  "Retreive user information from webfinger"
  [user]
  ;; TODO: This is doing way more than it's supposed to
  (if-let [xrd (fetch-user-meta user)]
    (let [webfinger-links (model.webfinger/get-links xrd)
          feed (fetch-user-feed (assoc user :links (concat (:links user) webfinger-links)))
          first-entry (-?> feed .getEntries first)
          new-user (-?> (abdera/get-author first-entry feed)
                        person->user)
          links (concat webfinger-links (:links new-user))]
      ;; TODO: only new fields, and only safe ones (maybe)
      (doseq [[k v] new-user]
        (model.user/set-field! user k v))
      (if (seq links)
        (doseq [link links]
          (add-link user link))
        (log/warn "usermeta has no links"))
      (model.user/fetch-by-id (:_id user)))
    (throw+ "Could not fetch user-meta")))

(defn fetch-updates-xmpp
  [user]
  ;; TODO: send user timeline request
  (let [packet (tigase/make-packet
                {:to (tigase/make-jid user)
                 :from (tigase/make-jid "" (config :domain))
                 :type :get
                 :body (element/make-element
                        ["pubsub" {"xmlns" ns/pubsub}
                         ["items" {"node" ns/microblog}]])})]
    (tigase/deliver-packet! packet)))

(defn parse-magic-public-key
  [user link]
  (let [key-string (:href link)
        [_ n e] (re-matches
                 #"data:application/magic-public-key,RSA.(.+)\.(.+)"
                 key-string)]
    ;; TODO: this should be calling a key action
    (model.key/set-armored-key (:_id user) n e)))

(defaction discover-user-rdf
  "Discover user information from their rdf feeds"
  [user]
  ;; TODO: alternately, check user meta
  (let [uri (:foaf-uri user)
        model (rdf/document-to-model uri :xml)
        query (model.user/foaf-query)]
    (sp/model-query-triples model query)))

(defn discover*
  [^User user & [options & _]]
  (loop [try-count (get options :try-count 1)]
    (when (< try-count 5)
      (if (:local user)
        user
        ;; Get domain should, in theory, always return a domain, or else error
        (let [domain (actions.domain/get-discovered (get-domain user))]
          (if (:discovered domain)
            (do

              (when (:xmpp domain)
                (request-vcard! user))

              ;; There should be a similar check here so we're not
              ;; hitting xmpp-only services.
              ;; This is really OStatus specific
              (update-usermeta user)

              ;; TODO: there sould be a different discovered flag for
              ;; each aspect of a domain, and this flag shouldn't be set
              ;; till they've all responded
              ;; (model.user/set-field! user :discovered true)
              (model.user/fetch-by-id (:_id user)))
            (do
              ;; Domain not yet discovered
              (actions.domain/discover domain)
              (recur (inc try-count)))))))))

(defaction discover
  "perform a discovery on the user"
  [^User user & [options & _]]
  (task (discover* user options))
  user)

;; TODO: xmpp case of update
(defaction fetch-remote
  [user]
  (let [domain (get-domain user)]
    (if (:xmpp domain)
      (request-vcard! user))))

(defaction register
  "Register a new user"
  [{:keys [username password email display-name location bio] :as options}]
  ;; TODO: should we check reg-enabled here?
  ;; verify submission.
  (if (and username password)
    (if-let [user (model.user/get-user username)]
      (throw+ "user already exists")
      (let [params (merge {:username username
                           :domain (:_id (actions.domain/current-domain))
                           :discovered true
                           :id (str "acct:" username "@" (config :domain))
                           :local true}
                          (when email {:email email})
                          (when display-name {:display-name display-name})
                          (when bio {:bio bio})
                          (when location {:location location}))
            user (create params)]
        (actions.auth/add-password user password)
        (actions.key/generate-key-for-user user)
        user))
    (throw+ "Missing required params")))

(defaction register-page
  "Display the form to reqister a user"
  []
  (model/->User))

(defaction show
  "This action just returns the passed user.
   The user needs to be retreived in the filter."
  [user]
  user)

(defaction update-profile
  [options]
  (let [user (session/current-user)]
    ;; TODO: mass assign vulnerability here
    (update user options)))

(defaction xmpp-service-unavailable
  "Error callback when user doesn't support xmpp"
  [user]
  (let [domain-name (:domain user)
        domain (actions.domain/get-discovered domain-name)]
    (actions.domain/set-xmpp domain false)
    user))

(defn handle-pending-get-user-meta
  [user]
  (get-user-meta user))

(l/receive-all ch/pending-get-user-meta (ops/op-handler handle-pending-get-user-meta))

(definitializer
  (require-namespaces
   ["jiksnu.filters.user-filters"
    "jiksnu.helpers.user-helpers"
    "jiksnu.sections.user-sections"
    "jiksnu.triggers.user-triggers"
    "jiksnu.views.user-views"])

  (util/add-hook!
   actions.domain/delete-hooks
   (fn [domain]
     (doseq [user (:items (model.user/fetch-by-domain domain))]
       (delete user))
     domain))
  )
