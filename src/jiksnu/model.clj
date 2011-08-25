(ns jiksnu.model
  (:use ciste.core
        [ciste.config :only (config environment)]
        clj-factory.core
        clojure.data.json
        jiksnu.namespace
        karras.entity
        plaza.rdf.core
        plaza.rdf.implementations.jena)
  (:require (jiksnu [redis :as redis])
            (karras [core :as karras]
                    [sugar :as sugar]))
  (:import java.io.PrintWriter
           java.text.SimpleDateFormat
           java.util.Date
           org.apache.axiom.util.UIDGenerator
           org.bson.types.ObjectId))

(def ^:dynamic *date-format* "yyyy-MM-dd'T'hh:mm:ssZ")

(def ^:dynamic *mongo-database* (ref nil))

(defonce ^:dynamic *formatter*
  (SimpleDateFormat. *date-format*))

(defn format-date
  [^Date date]
  (.format *formatter* date))

(init-jena-framework)
(register-rdf-ns :dc dc-ns)
(register-rdf-ns :foaf foaf-ns)

(defn mongo-database*
  []
  (karras/mongo-db (config :database :name)))

(defn mongo-database
  []
  (or @*mongo-database*
      (mongo-database*)))

(defembedded Person
  [:name])

(defentity Activity
  [:id
   :actor
   :title])

(defentity Like
  [:id :user :activity :created])

(defentity Subscription
  [:to :from :created])

(defentity User
  [:_id
   :username
   :created
   :updated
   :domain
   :subnodes])

(defentity Item
  [:user :activity :created])

(defentity Domain
  [:osw :ostatus])

(defentity PushSubscription
  [:hub :verify-token :secret :created :updated :topic])

(defentity MagicKeyPair
  [:crt-coefficient :prime-exponent-p :prime-exponent-q
   :prime-p :prime-q :public-exponent :private-exponent :userid])

(defmacro with-database
  [& body]
  `(karras/with-mongo-request (mongo-database)
    ~@body))

(defmacro with-environment
  [environment & body]
  `(binding [ciste.config/*environment* ~environment]
     (redis/init-client)
     (with-database
       ~@body)))

(defn activity?
  [activity]
  (instance? Activity activity))

(defn subscription?
  [s]
  (instance? Subscription s))

(defn make-id
  [^String id]
  (ObjectId. id))

(defn new-id
  []
  (UIDGenerator/generateURNString))

(defseq :id
  [n]
  n)

(defseq :domain
  [n]
  (str "example" n ".com"))

(defseq :word
  [n]
  (str "word" n))

(deffactory Activity
  {:id #'new-id
   :title (fseq :word)
   :summary (fseq :word)
   :published #'sugar/date
   :updated #'sugar/date
   :public true})

(deffactory Domain
  {:_id (fseq :domain)
   :local false})

(deffactory User
  (let [password (fseq :word)]
    {:username (fseq :word)
     :domain (config :domain)
     :name (fseq :word)
     :first-name (fseq :word)
     :last-name (fseq :word)
     :password password
     :confirm-password password}))

(deffactory Subscription
  {:to (fseq :word)
   :from (fseq :word)
   :created #'sugar/date})

(defn write-json-date
  ([date out]
     (write-json-date date out false))
  ([^Date date ^PrintWriter out escape-unicode?]
     (let [formatted-date (.format (SimpleDateFormat. *date-format*) date)]
       (.print out (str "\"" formatted-date "\"")))))

(defn write-json-object-id
  ([id out]
     (write-json-object-id id out false))
  ([id ^PrintWriter out escape-unicode]
     (.print out (str "\"" id "\""))))

(extend Date Write-JSON
  {:write-json write-json-date})
(extend ObjectId Write-JSON
  {:write-json write-json-object-id})

(defmacro with-context
  [[serialization format] & body]
  `(with-serialization ~serialization
    (with-format ~format
      ~@body)))
