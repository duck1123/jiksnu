(ns jiksnu.actions.activity-actions-test
  (:use (ciste core debug sections views)
        ciste.sections.default
        clj-factory.core
        clojure.test
        jiksnu.actions.activity-actions
        (jiksnu core-test model namespace session view)
        [karras.entity :only (make)]
        midje.sweet)
  (:require (clj-tigase [core :as tigase]
                        [element :as element]
                        [packet :as packet])
            (jiksnu.model [activity :as model.activity]
                          [user :as model.user]))
  (:import jiksnu.model.Activity
           jiksnu.model.User))

(use-fixtures :each test-environment-fixture)

(deftest prepare-activity-test
  (facts "should return an activity"
    (let [user (model.user/create (factory User))]
      (with-user user
        ;; TODO: fix clj-factory
        (let [args (make Activity (factory Activity))]
          (prepare-activity args) => activity?)))))

(deftest create-test
  (testing "when the user is logged in"
    (testing "and it is a valid activity"
     (facts "should return that activity"
       (with-serialization :xmpp
         (with-format :xmpp
           (let [user (model.user/create (factory User))]
             (with-user user
               (let [activity (factory Activity)
                     element (element/make-element
                              (index-section [activity]))
                     packet (tigase/make-packet
                             {:to (tigase/make-jid user)
                              :from (tigase/make-jid user)
                              :type :set
                              :body element})
                     request (packet/make-request packet)]
                 (create activity) => activity?)))))))))

(deftest create-test
  (testing "when the user is logged in"
    (facts "should return an activity"
      (with-user (model.user/create (factory User))
        (let [activity (factory Activity)]
          (create activity) => activity?))))
  ;; TODO: Move this to 'post'
  #_(testing "when the user is not logged in"
    (facts "should return nil"
      (let [activity (factory Activity)]
        (create activity) => nil))))

(deftest delete-test
  (testing "when the activity exists"
    (testing "and the user owns the activity"
      (testing "should delete that activity"
        (let [user (model.user/create (factory User))]
          (with-user user
            (let [activity (create (factory Activity {:author (:_id user)}))]
              (delete activity)
              (is (nil? (model.activity/fetch-by-id (:_id activity)))))))))
    (testing "and the user does not own the activity"
      (testing "should not delete that activity"
        (let [user1 (model.user/create (factory User))
              user2 (model.user/create (factory User))
              activity (with-user user1
                         (model.activity/create (factory Activity)))]
          (with-user user2
            (delete activity)
            (is (model.activity/fetch-by-id (:_id activity)))))))))

(deftest edit-test)

(deftest fetch-comments-test
  (testing "when the activity exists"
    (testing "and there are no comments"
      (testing "should return an empty sequence"
        (let [actor (model.user/create (factory User))]
          (with-user actor
            (let [activity (create (factory Activity))
                  [_ comments] (fetch-comments activity)]
              (is (empty? comments)))))))))

(deftest fetch-comments-remote-test)

(deftest friends-timeline-test)

(deftest inbox-test)

(deftest index-test
  (testing "when there are no activities"
    (testing "should be empty"
      (model.activity/drop!)
      (let [response (index)]
        (is (empty? response)))))
  (testing "when there are activities"
    (testing "should return a seq of activities"
      (let [author (model.user/create (factory User))]
        (with-user author
          (model.activity/create (factory Activity))))
      (let [response (index)]
        (is (seq response))
        (is (every? activity? response))))))

(deftest like-activity-test)

(deftest new-test)

(deftest new-comment-test)

(deftest show-test
  (testing "when the record exists"
    (testing "and the user is not logged in"
      (testing "and the record is public"
        (facts "should return the activity"
          (let [author (model.user/create (factory User))
                activity (with-user author
                           (create (factory Activity)))]
            (show (:_id activity)) => activity?)))
      (testing "and the record is not public"
        (facts "should return nil"
          (let [author (model.user/create (factory User))
                activity (with-user author
                           (create (factory Activity {:public false})))]
            (show (:_id activity)) => nil?))))
    (testing "and the user is logged in"
      (testing "and is the author"
        (facts "should return the activity"
          (let [user (model.user/create (factory User))]
            (with-user user
              (let [activity (create (factory Activity))]
                (show (:_id activity)) => activity?)))))
      (testing "and is not the author"
        (testing "and is not on the access list"
          (testing "and is an admin"
            (facts "should return the activity"
              (let [user (model.user/create (factory User {:admin true}))
                    author (model.user/create (factory User))]
                (let [activity (with-user author
                                 (create (factory Activity {:public false})))]
                  (with-user user
                    (show (:_id activity)) => activity?)))))
          (testing "and is not an admin"
            (facts "should return nil"
              (let [user (model.user/create (factory User))
                    author (model.user/create (factory User))
                    activity (with-user author
                               (create (factory Activity {:public false})))]
                (with-user user
                  (show (:_id activity)) => nil?)))))))
    (testing "and the record is not public"
      (testing "and the user is not logged in"
        (facts "should return nil"
          (let [activity (create (factory Activity {:public false}))
                response (show (:_id activity))]
            (is (nil? response))))))))

(deftest update-test)

(deftest user-timeline-test)
