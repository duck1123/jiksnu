(ns jiksnu.models
  (:require jiksnu.app)
  (:use-macros [gyr.core :only [def.factory]]))

(defn deserializer
  [resource-name data]
  (if-let [items (.-items (.-data data))]
    items
    (.-data data)))

;; (def.factory jiksnu.$exceptionHandler
;;   []
;;   (fn [exception cause]
;;     (throw exception)))

(def.factory jiksnu.Activities
  [DS]
  (.defineResource
   DS
   #js
   {:name "activity"
    :endpoint "activities"
    :methods #js {:getType (constantly "Activity")}}))

(def.factory jiksnu.Clients
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "client"
    :endpoint "clients"
    :deserialize deserializer
    :methods
    #js
    {:getType (constantly "Client")}}))

(def.factory jiksnu.Conversations
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "conversation"
    :endpoint "conversations"
    :deserialize deserializer
    :methods
    #js
    {:getActivities (fn [] (this-as item (.fetch subpageService item "activities")))
     :getType (constantly "Conversation")}}))

(def.factory jiksnu.Domains
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "domain"
    :endpoint "domains"
    :deserialize deserializer
    :methods #js {:getType (constantly "Domain")}}))

(def.factory jiksnu.Followings
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "following"
    :endpoint "followings"
    :deserialize deserializer
    :methods #js {:getType (constantly "Following")}}))

(def.factory jiksnu.Groups
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "group"
    :endpoint "groups"
    :deserialize deserializer
    :methods #js {:getType (constantly "Group")}}))

(def.factory jiksnu.GroupMemberships
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "group-membership"
    :endpoint "group-memberships"
    :deserialize deserializer
    :methods #js {:getType (constantly "GroupMembership")}}))

(def.factory jiksnu.Likes
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name "like"
    :endpoint "likes"
    :deserialize deserializer
    :methods #js {:getType (constantly "Like")}}))

(def.factory jiksnu.Streams
  [DS]
  (.defineResource
   DS
   #js
   {:name "stream"
    :endpoint "streams"
    :methods #js {:getType (constantly "Stream")}}))

(def.factory jiksnu.Subscriptions
  [DS]
  (.defineResource
   DS
   #js
   {:name "subscription"
    :endpoint "subscriptions"
    :methods #js {:getType (constantly "Subscription")}}))

(def.factory jiksnu.Users
  [DS subpageService]
  (.defineResource
   DS
   #js
   {:name        "user"
    :endpoint    "users"
    :deserialize deserializer
    :methods
    #js
    {:getType      (constantly "User")
     :getSubpage   (fn [page-name] (this-as item (.fetch subpageService item page-name)))
     :getFollowers (fn [] (this-as item (.fetch subpageService item "followers")))
     :getFollowing (fn [] (this-as item (.fetch subpageService item "following")))
     :getGroups    (fn [] (this-as item (.fetch subpageService item "groups")))
     :getStreams   (fn [] (this-as item (.fetch subpageService item "streams")))}}))
