(ns jiksnu.channels
  (:require [lamina.core :as l]))

;; async fetchers

(defonce pending-get-conversation
  (l/channel*
   :permanent? true
   :description "pending-get-conversation"))

(defonce pending-get-discovered
  (l/channel*
   :permanent? true
   :description "pending-get-discovered"))

(defonce pending-get-domain
  (l/channel*
   :permanent? true
   :description "pending-get-domain"))

(defonce pending-get-resource
  (l/channel*
   :permanent? true
   :description "pending-get-resource"))

(defonce pending-get-source
  (l/channel*
   :permanent? true
   :description "pending-get-source"))

(defonce pending-create-conversations
  (l/channel*
   :permanent? true
   :description "pending-create-conversations"))

(defonce pending-update-resources
  (l/channel*
   :permanent? true
   :description "pending-update-resources"))

(defonce
  ^{:doc "Channel containing list of sources to be updated"}
  pending-updates (l/permanent-channel))

(defonce pending-entries
  (l/channel*
   :permanent? true
   :description "All atom entries that are seen come through here"))

(defonce posted-activities
  (l/channel*
   :permanent? true
   :description "Channel for newly posted activities"))

(defonce posted-conversations
  (l/channel*
   :permanent? true
   :description "Channel for newly posted conversations"))

(defonce pending-get-user-meta
  (l/channel*
   :permanent? true
   :description "get-user-meta"))
