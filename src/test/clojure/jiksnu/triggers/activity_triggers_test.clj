(ns jiksnu.triggers.activity-triggers-test
  (:use ciste.core
        ciste.debug
        ciste.views
        clj-factory.core
        clojure.test
        jiksnu.core-test
        jiksnu.model
        jiksnu.session
        jiksnu.triggers.activity-triggers
        jiksnu.views.activity-views)
  (require [clj-tigase.packet :as packet]
           [jiksnu.model.activity :as model.activity]
           [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.User))

(use-fixtures :each test-environment-fixture)

(deftest notify-activity-test
  (testing "should return a packet"
    (let [user (model.user/create (factory User))]
      (with-user user
        (let [activity (model.activity/create
                        (factory Activity
                                 {:authors [(:_id user)]}))
              response (notify-activity user activity)]
          (is (packet/packet? response)))))))
