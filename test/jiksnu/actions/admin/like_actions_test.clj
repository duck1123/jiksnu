(ns jiksnu.actions.admin.like-actions-test
  (:use [clj-factory.core :only [factory fseq]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [jiksnu.actions.admin.like-actions :only [delete index]]
        [midje.sweet :only [fact future-fact every-checker falsey =>]])
  (:require [jiksnu.model.like :as model.like]))

(test-environment-fixture

 (fact "#'index"

   (index) =>
   (every-checker
    #(fact (:page %) => 1)
    #(fact (:totalRecords %) => 0))

   (index {} {:page 2}) =>
   (every-checker
    #(fact (:page %) => 2)
    #(fact (:totalRecords %) => 0)))


 (future-fact "#'delete"
   (let [like (model.like/create (factory :like))]
     (delete like)
     (model.like/fetch-by-id (:_id like)) => falsey

     )

   )
 )
