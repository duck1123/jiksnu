(ns jiksnu.triggers.activity-triggers-test
  (:use (ciste core debug views)
        clj-factory.core
        clojure.test
        jiksnu.test-helper
        jiksnu.model
        jiksnu.session
        jiksnu.triggers.activity-triggers
        jiksnu.views.activity-views
        midje.sweet)
  (require [clj-tigase.packet :as packet]
           [jiksnu.model.activity :as model.activity]
           [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.User))

(test-environment-fixture)

;; (deftest notify-activity-test)

(fact "should return a packet"
  (let [user (model.user/create (factory User))]
    (with-user user
      (let [activity (model.activity/create
                      (factory Activity
                               {:author (:_id user)}))
            response (notify-activity user activity)]
        (is (packet/packet? response))))))
