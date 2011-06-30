(ns jiksnu.helpers.activity-helpers-test
  (:use ciste.core
        ciste.sections
        ciste.sections.default
        clj-factory.core
        clj-tigase.core
        clojure.test
        jiksnu.helpers.activity-helpers
        jiksnu.model
        jiksnu.namespace
        jiksnu.session
        jiksnu.view)
  (:import jiksnu.model.Activity))

(deftest to-activity
  (testing "should return a map"
    (with-serialization :http
      (with-format :atom
        (let [activity (factory Activity)
              entry (show-section activity)
              response (to-activity entry)]
          (expect (map? response)))))))

(deftest to-json
  (testing "should not be nil"
    (with-serialization :http
      (with-format :atom
        (let [activity (factory Activity)
              entry (show-section activity)
              response (to-json entry)]
          (expect (not (nil? response))))))))

(deftest set-id
  (testing "when there is an id"
    (testing "should not change the value"
      (let [activity (factory Activity)
            response (set-id activity)]
        (expect (= (:_id activity)
                   (:_id response))))))
  (testing "when there is no id"
    (testing "should add an id key"
      (let [activity (factory Activity)
            response (set-id activity)]
        (:_id response)))))

(deftest set-updated-time
  (testing "when there is an updated property"
    (testing "should not change the value"
      (let [activity (factory Activity)
            response (set-updated-time activity)]
        (expect (= (:updated activity)
                   (:updated response))))))
  (testing "when there is no updated property"
    (testing "should add an updated property"
      (let [activity (dissoc (factory Activity) :updated)
            response (set-updated-time activity)]
        (expect (:updated response))))))
