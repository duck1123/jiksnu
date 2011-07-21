(ns jiksnu.triggers.subscription-triggers
  (:use ciste.config
        ciste.core
        ciste.debug
        ciste.triggers
        ciste.sections
        ciste.sections.default
        jiksnu.actions.subscription-actions
        jiksnu.helpers.subscription-helpers
        jiksnu.namespace
        jiksnu.view)
  (:require [clj-tigase.core :as tigase]
            [clj-tigase.element :as element]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.user :as model.user]
            [jiksnu.helpers.user-helpers :as helpers.user]))

(defn notify-subscribe-xmpp
  [request subscription]
  (with-serialization :xmpp
    (with-format :xmpp
      (let [user (model.user/fetch-by-id (:from subscription))
            subscribee (model.user/fetch-by-id (:to subscription))
            ele (subscribe-request subscription)
            packet (tigase/make-packet {:body (element/make-element ele)
                                 :type :set
                                 :id (:id request)
                                 :from (tigase/make-jid user)
                                 :to (tigase/make-jid subscribee)})]
        (tigase/deliver-packet! packet)))))

(defn notify-unsubscribe-xmpp
  [request subscription]
  (with-serialization :xmpp
    (with-format :xmpp
      (let [user (model.user/fetch-by-id (:from subscription))
            subscribee (model.user/fetch-by-id (:to subscription))
            ele (unsubscription-request subscription)
            packet (tigase/make-packet {:body (element/make-element ele)
                                 :type :set
                                 :id (:id request)
                                 :from (tigase/make-jid user)
                                 :to (tigase/make-jid subscribee)})]
        (tigase/deliver-packet! packet)))))

(defn notify-subscribe
  [action [user] subscription]
  (let [domain (model.domain/show (:domain user))]
    (if (:xmpp domain)
      (notify-subscribe-xmpp {} subscription)
      ;; TODO: OStatus case
      )))

(defn notify-unsubscribe
  [action [user] subscription]
  (let [domain (model.domain/show (:domain user))]
    (if (:xmpp domain)
      (notify-unsubscribe-xmpp {} subscription)
      ;; TODO: OStatus case
      )))

(defn notify-subscribed
  [action params subscription]
  (let [[actor user] params]
   (if (model.user/local? user)
     (let [packet (tigase/make-packet
                   {:type :headline
                    :to (tigase/make-jid user)
                    :from (tigase/make-jid "" (config :domain))
                    :body
                    (element/make-element
                     ["body" {}
                      (str (title actor)
                           " has subscribed to you")])})]
       (tigase/deliver-packet! packet)))))

(add-trigger! #'subscribe #'notify-subscribe)
(add-trigger! #'unsubscribe #'notify-unsubscribe)
(add-trigger! #'subscribed #'notify-subscribed)
