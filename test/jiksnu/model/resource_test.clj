(ns jiksnu.model.resource-test
  (:use [clj-factory.core :only [factory]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [jiksnu.session :only [with-user]]
        [jiksnu.model.resource :only [count-records create delete drop! fetch-all fetch-by-id]]
        [midje.sweet :only [every-checker fact future-fact => throws]]
        [validateur.validation :only [valid?]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.mock :as mock]
            [jiksnu.features-helper :as feature]
            [jiksnu.model :as model]
            [jiksnu.util :as util])
  (:import jiksnu.model.Resource))

(test-environment-fixture

 (fact "#'fetch-by-id"
   (fact "when the item doesn't exist"
     (let [id (util/make-id)]
       (fetch-by-id id) => nil?))

   (fact "when the item exists"
     (let [item (mock/a-resource-exists)]
       (fetch-by-id (:_id item)) => item)))

 (fact "#'create"
   (fact "when given valid params"
     (let [params (actions.resource/prepare-create
                   (factory :resource))]
       (create params) => (partial instance? Resource)))

   (fact "when given invalid params"
     (create {}) => (throws RuntimeException)))

 (fact "#'delete"
   (let [item (mock/a-resource-exists)]
     (delete item) => item
     (fetch-by-id (:_id item)) => nil))

 (fact "#'fetch-all"
   (fact "when there are no records"
     (drop!)
     (fetch-all) => (every-checker
                     seq?
                     empty?))

   (fact "when there is more than a page"
     (drop!)

     (dotimes [n 25]
       (mock/a-resource-exists))

     (fetch-all) =>
     (every-checker
      seq?
      #(fact (count %) => 20))

     (fetch-all {} {:page 2}) =>
     (every-checker
      seq?
      #(fact (count %) => 5))))

 (fact "#'count-records"
   (fact "when there aren't any items"
     (drop!)
     (count-records) => 0)
   (fact "when there are conversations"
     (drop!)
     (let [n 15]
       (dotimes [i n]
         (mock/a-resource-exists))
       (count-records) => n)))

 )
