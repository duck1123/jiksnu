(ns jiksnu.model.conversation-test
  (:use [clj-factory.core :only [factory]]
        [jiksnu.test-helper :only [check context test-environment-fixture]]
        [jiksnu.session :only [with-user]]
        [jiksnu.model.conversation :only [count-records create delete drop! fetch-all fetch-by-id]]
        [midje.sweet :only [=> throws]]
        [validateur.validation :only [valid?]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.actions.conversation-actions :as actions.conversation]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.mock :as mock]
            [jiksnu.features-helper :as feature]
            [jiksnu.model :as model]
            [jiksnu.util :as util])
  (:import jiksnu.model.Conversation))

(test-environment-fixture

 (context #'fetch-by-id
   (context "when the item doesn't exist"
     (let [id (util/make-id)]
       (fetch-by-id id) => nil?))

   (context "when the item exists"
     (let [item (mock/a-conversation-exists)]
       (fetch-by-id (:_id item)) => item)))

 (context #'create
   (context "when given valid params"
     (let [domain (mock/a-domain-exists)
           source (mock/a-feed-source-exists)
           params (actions.conversation/prepare-create
                   (factory :conversation {:update-source (:_id source)
                                           :local false
                                           :domain (:_id domain)}))]
       (create params) => (partial instance? Conversation)))

   (context "when given invalid params"
     (create {}) => (throws RuntimeException)))

 (context #'drop!
   (dotimes [i 1]
     (mock/a-conversation-exists))
   (drop!)
   (count-records) => 0)

 (context #'delete
   (let [item (mock/a-conversation-exists)]
     (delete item) => item
     (fetch-by-id (:_id item)) => nil))

 (context #'fetch-all
   (context "when there are no items"
     (drop!)
     (fetch-all) =>
     (check [response]
       response => seq?
       response => empty?))

   (context "when there is more than a page of items"
     (drop!)

     (let [n 25]
       (dotimes [i n]
         (mock/a-conversation-exists))

       (fetch-all) =>
       (check [response]
        response => seq?
        (count response) => 20)

       (fetch-all {} {:page 2}) =>
       (check [response]
         response => seq?
        (count response) => (- n 20)))))

 (context #'count-records
   (context "when there aren't any items"
     (drop!)
     (count-records) => 0)
   (context "when there are items"
     (drop!)
     (let [n 15]
       (dotimes [i n]
         (mock/a-conversation-exists))
       (count-records) => n)))

 )
