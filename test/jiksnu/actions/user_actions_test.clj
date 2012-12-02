(ns jiksnu.actions.user-actions-test
  (:use [ciste.config :only [config]]
        [ciste.core :only [with-context]]
        [ciste.sections.default :only [show-section]]
        [clj-factory.core :only [factory fseq]]
        [midje.sweet :only [fact future-fact => anything throws contains every-checker]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        jiksnu.actions.user-actions)
  (:require [ciste.model :as cm]
            [clojure.tools.logging :as log]
            [jiksnu.abdera :as abdera]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.existance-helpers :as existance]
            [jiksnu.factory :as factory]
            [jiksnu.features-helper :as feature]
            [jiksnu.model :as model]
            [jiksnu.model.authentication-mechanism :as model.auth-mechanism]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.user :as model.user]
            [jiksnu.model.webfinger :as model.webfinger]
            [ring.util.codec :as codec])
  (:import jiksnu.model.Domain
           jiksnu.model.User
           org.apache.abdera2.model.Person))

(defn mock-user-meta
  [username domain-name uri source-link]
  (cm/string->document
   (format "
<XRD xmlns=\"http://docs.oasis-open.org/ns/xri/xrd-1.0\">
  <Subject>%s</Subject>
  <Link rel=\"http://webfinger.net/rel/profile-page\" type=\"text/html\" href=\"%s\"></Link>
  <Link rel=\"http://apinamespace.org/atom\" type=\"application/atomsvc+xml\"
        href=\"http://%s/api/statusnet/app/service/%s.xml\">
    <Property type=\"http://apinamespace.org/atom/username\">%s</Property>
  </Link>
  <Link rel=\"http://apinamespace.org/twitter\" href=\"https://%s/api/\">
    <Property type=\"http://apinamespace.org/twitter/username\">%s</Property>
  </Link>
  <Link rel=\"http://schemas.google.com/g/2010#updates-from\"
        href=\"%s\" type=\"application/atom+xml\"></Link>
</XRD>"
           uri uri domain-name username username domain-name username source-link)))

(test-environment-fixture

 (fact "#'get-username"
   (fact "when given a http uri"
     (let [username (fseq :username)
           domain-name (fseq :domain)
           template (str "http://" domain-name "/xrd?uri={uri}")
           domain (actions.domain/find-or-create (factory :domain
                                                          {:_id domain-name
                                                           :discovered true
                                                           :links [{:rel "lrdd"
                                                                    :template template}]}))
           uri (factory/make-uri domain-name "/users/1")
           source-link (fseq :uri)]
       (get-username {:id uri}) => (contains {:username username})
       (provided
         (get-user-meta anything) => .xrd.
         (model.webfinger/get-feed-source-from-xrd .xrd.) => .topic.
         (model.webfinger/get-username-from-xrd .xrd.) => username)))

   (fact "when given an acct uri"
     (let [domain-name (fseq :domain)
           template (str "http://" domain-name "/xrd?uri={uri}")
           domain (actions.domain/find-or-create
                   (factory :domain
                            {:_id domain-name
                             :links [{:rel "lrdd" :template template}]}))
           uri (str "acct:bob@" domain-name)]
       (get-username {:id uri}) => (contains {:username "bob"}))))

 (fact "#'get-domain"
   (fact "when the domain already exists"

     (let [domain (existance/a-domain-exists {:discovered true})]

       (fact "when the domain is specified"
         (let [response (get-domain {:domain (:_id domain)})]
           response => (partial instance? Domain)
           (:_id response) => (:_id domain)))

       (fact "when the domain is not specified"
         (fact "when there is an id"
           (fact "when it is a http url"
             (let [response (get-domain {:id (str "http://" (:_id domain)
                                                  "/users/1")})]
               response => (partial instance? Domain)
               (:_id response) => (:_id domain)))

           (fact "when it is an acct uri"
             (let [response (get-domain {:id (str "acct:" (fseq :username)
                                                  "@" (:_id domain))})]
               response => (partial instance? Domain)
               (:_id response) => (:_id domain))))))))

 (fact "#'create"
   (fact "when the params ar nil"
     (create nil) => (throws RuntimeException))
   (fact "empty map"
     (create {}) => (throws RuntimeException))
   (fact "local user"
     (create {:username (fseq :username)
              :domain (config :domain)}) => model/user?))

 (fact "#'index"
   (index) => map?)

 (fact "#'person->user"
   (fact "when the user has an acct uri"

     (fact "when the domain is discovered"
       (fact "when given a Person generated by show-section"
         (model/drop-all!)
         (let [user (existance/a-user-exists)
               person (with-context [:http :atom] (show-section user))]
           (person->user person) =>
           (every-checker
            (partial instance? User)
            #(= (:username %) (:username user))
            #(= (:id %) (:id user))
            #(= (:domain %) (:domain user))
            #(= (:url %) (:url user))
            #(= (:display-name %) (:display-name user))))))

     (fact "when the domain is not discovered"
       (fact "when given a Person generated by show-section"
         (model/drop-all!)
         (let [user (existance/a-user-exists)
               person (with-context [:http :atom] (doall (show-section user)))]
           (person->user person) =>
           (every-checker
            (partial instance? User)
            #(= (:username %) (:username user))
            #(= (:id %)       (:id user))
            #(= (:domain %)   (:domain user))
            #(= (:url %) (:url user))
            #(= (:display-name %) (:display-name user)))))))

   (fact "when the user has an http uri"
     (fact "when the domain is not discovered"
       (fact "when given a Person generated by show-section"
         ;; (model/drop-all!)
         (let [domain-name (fseq :domain)
               uri (str "http://" domain-name "/users/1")
               person (.newAuthor abdera/abdera-factory)]
           (doto person
             (.setUri uri))
           ;; (person->user person) => (partial instance? User)
           (person->user person) => (contains {:id uri
                                               :domain domain-name
                                               :username "bob"})
           (provided
             (actions.domain/get-discovered anything) => .domain.
             (get-username anything) => "bob"))))))

 (fact "#'find-or-create-by-remote-id"
   (let [username (fseq :username)
         domain-name (fseq :domain)
         template (str "http://" domain-name "/xrd?uri={uri}")]

     (fact "when given a http uri"
       (fact "when the domain is discovered"
         (model/drop-all!)
         (let [username (fseq :username)
               domain (actions.domain/find-or-create
                       (factory :domain
                                {:_id domain-name
                                 :links [{:rel "lrdd" :template template}]
                                 :discovered true}))
               uri (str "http://" domain-name "/user/1")
               um-url (format "http://%s/xrd?uri=%s"
                              domain-name
                              (codec/url-encode uri))
               source-link (format "http://%s/api/statuses/user_timeline/1.atom" domain-name)
               mock-um (mock-user-meta username domain-name uri source-link)]
           (find-or-create-by-remote-id {:id uri}) => (partial instance? User)
           (provided
             (actions.user/get-user-meta anything) => mock-um))))
     (future-fact "when given an acct uri uri"
       (model/drop-all!)
       (let [domain (actions.domain/find-or-create
                     (factory :domain
                              {:links [{:rel "lrdd" :template template}]
                               :discovered true}))
             uri (str "acct:" username "@" (:_id domain))
             response (find-or-create-by-remote-id {:id uri})]
         response => (partial instance? User)))))

 (fact "#'register-page"
   (register-page) =>
   (every-checker
    (partial instance? User)))

 (fact "#register"
   (let [params {:username (fseq :username)
                 :email (fseq :email)
                 :display-name (fseq :display-name)
                 :bio (fseq :bio)
                 :location (fseq :location)
                 :password (fseq :password)}]
     (register params) =>
     (every-checker
      map?
      (partial instance? User)
      (fn [response]
        (fact
          (model.auth-mechanism/fetch-by-user response) =not=> empty?)))))
 )
