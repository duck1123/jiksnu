(ns jiksnu.model.like-test
  (:use [clj-factory.core :only [factory fseq]]
        [jiksnu.model.like :only [create delete fetch-by-id fetch-all]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [midje.sweet :only [fact falsey =>]]))

(test-environment-fixture

 (fact "fetch-all"
   (fetch-all) => seq?)

 (fact "delete"
   (let [like (create (factory :like))]
     (delete like)
     (fetch-by-id (:_id like)) => falsey
     )
   )
 
 )
