(ns jiksnu.factory
  (:use [ciste.config :only [config]]
        [clj-factory.core :only [defseq deffactory fseq factory]])
  (:require [clj-time.core :as time]
            [jiksnu.abdera :as abdera]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.Conversation
           jiksnu.model.Domain
           jiksnu.model.Subscription
           jiksnu.model.User))

(defseq :id
  [n]
  n)

(defseq :username
  [n]
  (str "user" n))

(defseq :name
  [n]
  (rand-nth
   ["Alice" "Bob" "Carol"
    "Dave" "Eve" "Frank"]))

(defseq :surname
  [n]
  (rand-nth
   ["Smith" "Miller" "Johnson"
    "Doe" "McDuck" "Skywalker"
    "Attreides"
    ]))

(defseq :password
  [n]
  (str "hunter" n))

(defseq :domain
  [n]
  (str "subdomain" n ".example.com"))

(defseq :uri
  [n]
  (str "http://" (fseq :domain) "/" n))

(defseq :title
  [n]
  (str n "th post!"))

(defseq :summary
  [n]
  (str n "th post!"))

(defseq :word
  [n]
  (str "word" n))

(deffactory Domain
  {:_id (fseq :domain)
   :local false})

(deffactory :domain
  (factory Domain))

(deffactory Subscription
  {:to (fseq :word)
   :from (fseq :word)
   :created #'time/now})

(deffactory :subscription
  (factory Subscription))

(deffactory User
  (let [password (fseq :password)
        first-name (fseq :name)
        last-name (fseq :surname)
        display-name (str first-name " " last-name)]
    {:username (fseq :username)
     :domain (fseq :domain)
     :name display-name
     :display-name display-name
     :first-name first-name
     :last-name last-name}))

(deffactory :local-user
  (factory User {:domain (config :domain)}))

(deffactory :user
  (factory User))

(deffactory Conversation
  {:items []
   :local true}
  )

(deffactory :conversation
  (factory Conversation))

(deffactory Activity
  {:id #'abdera/new-id
   :title (fseq :title)
   :summary (fseq :summary)
   :author (:_id (model.user/create (factory :user)))
   :published #'time/now
   :verb "post"
   :object-type "note"
   :updated #'time/now
   :public true})

(deffactory :activity
  (factory Activity))
