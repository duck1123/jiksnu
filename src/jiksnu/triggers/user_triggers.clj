(ns jiksnu.triggers.user-triggers
  (:use (ciste config
               [debug :only (spy)]
               triggers)
        (jiksnu view)
        lamina.core)
  (:require (clj-tigase [core :as tigase]
                        [element :as element])
            (clojure.tools [logging :as log])
            (jiksnu [namespace :as namespace])
            (jiksnu.actions [activity-actions :as actions.activity]
                            [user-actions :as actions.user])
            (jiksnu.helpers [user-helpers :as helpers.user])
            (jiksnu.model [domain :as model.domain]
                          [signature :as model.signature]))
  (:import org.deri.any23.Any23))

(defonce a23 (Any23.))

(defn discover-user-xmpp
  [user]
  (log/info "discover xmpp")
  (actions.user/request-vcard! user))

(defn discover-user-http
  [user]
  (log/info "discovering http")
  (actions.user/update-usermeta user)
  #_(request-hcard user))

(defn discover-user
  [action _ user]
  (let [domain (model.domain/show (:domain user))]
    (if (:discovered domain)
      (do (async (discover-user-xmpp user))
          (async (discover-user-http user)))
      (actions.user/enqueue-discover user))))

(defn fetch-updates-http
  [user]
  (let [uri (helpers.user/feed-link-uri user)]
    (actions.activity/fetch-remote-feed uri)))

(defn fetch-updates-xmpp
  [user]
  ;; TODO: send user timeline request
  (let [packet (tigase/make-packet
                {:to (tigase/make-jid user)
                 :from (tigase/make-jid "" (config :domain))
                 :type :get
                 :body (element/make-element
                        ["pubsub" {"xmlns" namespace/pubsub}
                         ["items" {"node" namespace/microblog}]])})]
    (tigase/deliver-packet! packet)))

(defn fetch-updates-trigger
  [action _ user]
  (let [domain (model.domain/show (:domain user))]
    (if (:xmpp domain)
      (fetch-updates-xmpp user)
      (fetch-updates-http user))))

(defn create-trigger
  [action params user]
  (actions.user/discover user))

(defn add-link-trigger
  [action [user link] _]
  (if (= (:rel link) "magic-public-key")
    (let [key-string (:href link)
          [_ n e]
          (re-matches
           #"data:application/magic-public-key,RSA.(.+)\.(.+)"
           key-string)]
      (model.signature/set-armored-key (:_id user) n e))))

(add-trigger! #'actions.user/add-link      #'add-link-trigger)
(add-trigger! #'actions.user/create        #'create-trigger)
(add-trigger! #'actions.user/discover      #'discover-user)
(add-trigger! #'actions.user/fetch-updates #'fetch-updates-trigger)
