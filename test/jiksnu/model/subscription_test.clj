(ns jiksnu.model.subscription-test
  (:use [ciste.config :only [with-environment]]
        clj-factory.core
        jiksnu.test-helper
        jiksnu.model
        jiksnu.session
        jiksnu.model.subscription
        jiksnu.xmpp.plugin
        karras.core
        midje.sweet)
  (:require [jiksnu.model.user :as model.user])
  (:import jiksnu.model.User))

(test-environment-fixture

 (fact "#'drop!"
   (fact "when there are subscriptions"
     (fact "should delete them all"
       (let [actor (model.user/create (factory User))
             user (model.user/create (factory User))]
         (with-user (:_id actor)
           (subscribe (current-user-id) (:_id user))))
       (drop!)
       (index) => empty?)))

 (fact "#'index"
   (fact "when there are no subscriptions"
     (fact "should be empty"
       (index) => empty?)
     (fact "should return a seq"
       (index) => seq?)))

 (fact "#'subscribe"

   (fact "when the user is logged in"
     (fact "and the subscription doesn't exist"
       (fact "should return a Subscription"
         (drop!)
         (let [actor (model.user/create (factory User))
               user (model.user/create (factory User))]
           (let [response (subscribe (:_id actor) (:_id user))]
             response => subscription?))))))

 (fact "#'subscribing?"

   (fact "when the user is subscribing"
     (fact "should return true"
       (let [actor (model.user/create (factory User))
             user (model.user/create (factory User))]
         (subscribe actor user)
         (let [response (subscribing? actor user)]
           response => truthy))))

   (fact "when the user is not subscribed"
     (fact "should return a false value"
       (let [actor (model.user/create (factory User))
             user (model.user/create (factory User))]
         (let [response (subscribing? actor user)]
           response =not=> truthy)))))

 (fact "#'subscribed?"

   (fact "when the user is subscribed"
     (fact "should return true"
       (let [actor (model.user/create (factory User))
             user (model.user/create (factory User))]
         (subscribe user actor)
         (let [response (subscribed? actor user)]
           response => truthy))))

   (fact "when the user is not subscribed"
     (fact "should return a false value"
       (let [actor (model.user/create (factory User))
             user (model.user/create (factory User))]
         (let [response (subscribed? actor user)]
           response =not=> truthy))))))
