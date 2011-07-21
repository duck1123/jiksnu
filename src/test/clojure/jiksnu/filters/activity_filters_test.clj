(ns jiksnu.filters.activity-filters-test
  (:use clj-factory.core
        ciste.debug
        ciste.filters
        ciste.sections
        clojure.test
        jiksnu.core-test
        jiksnu.filters.activity-filters
        jiksnu.model
        jiksnu.namespace
        jiksnu.session
        jiksnu.view
        jiksnu.xmpp.element)
  (:require [clj-tigase.core :as tigase]
            [clj-tigase.element :as element]
            [clj-tigase.packet :as packet]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.User))

(use-fixtures :each test-environment-fixture)

;; (deftest filter-action "#'actions.activity/create :xmpp"
;;   (testing "when the user is logged in"
;;     (testing "and it is a valid activity"
;;       (testing "should return that activity"
;;         (with-serialization :xmpp
;;           (with-format :xmpp
;;             (let [user (model.user/create (factory User))]
;;               (with-user user
;;                 (let [activity (factory Activity)
;;                       element (element/make-element
;;                                (index-section [activity]))
;;                       packet (tigase/make-packet
;;                               {:to (tigase/make-jid user)
;;                                :from (tigase/make-jid user)
;;                                :type :set
;;                                :body element})
;;                       request (assoc (packet/make-request packet)
;;                                 :serialization :xmpp)
;;                       response (filter-action #'create request)]
;;                   (is (activity? response)))))))))))

(deftest filter-action-test
  (testing "#'index :http :html"
    (testing "when there are no activities"
      (testing "should be empty"
        (model.activity/drop!)
        (let [request {:serialization :http}
              response (filter-action #'actions.activity/index request)]
          (is (empty? response)))))
    (testing "when there are activities"
      (testing "should return a seq of activities"
        (model.activity/drop!)
        (let [author (actions.user/create (factory User))]
          (with-user author
            (actions.activity/create (factory Activity))
            (let [request {:serialization :http
                           :action #'actions.activity/index
                           :format :html}
                  response (filter-action #'actions.activity/index request)]
              (is (seq response))
              (is (class (first response)))
              (is (every? activity? response)))))))))

(deftest filter-action-test
  (testing "#'index :xmpp"
    (testing "when there are no activities"
      (testing "should return an empty sequence"
        (model.activity/drop!)
        (let [user (model.user/create (factory User))
              element nil
              packet (tigase/make-packet
                      {:from (tigase/make-jid user)
                       :to (tigase/make-jid user)
                       :type :get
                       :body element})
              request (assoc (packet/make-request packet)
                        :serialization :xmpp)]
          (let [response (filter-action #'actions.activity/index request)]
            (is (not (nil? response)))
            (is (empty? response))))))
    (testing "when there are activities"
      (testing "should return a sequence of activities"
        (let [author (model.user/create (factory User))]
          (with-user author
            (let [element nil
                  packet (tigase/make-packet
                          {:from (tigase/make-jid author)
                           :to (tigase/make-jid author)
                           :type :get
                           :id (fseq :id)
                           :body element})
                  request (assoc (packet/make-request packet)
                            :serialization :xmpp)
                  activity (model.activity/create (factory Activity))
                  response (filter-action #'actions.activity/index request)]
              (is (seq response))
              (is (every? activity? response)))))))))

(deftest filter-action-test
  (testing "#'show :xmpp"
    (testing "when the activity exists"
      (testing "should return that activity"
        (let [author (model.user/create (factory User))]
          (with-user author
            (let [activity (model.activity/create (factory Activity))
                  packet-map {:from (tigase/make-jid author)
                              :to (tigase/make-jid author)
                              :type :get
                              :id "JIKSNU1"
                              :body (element/make-element
                                     ["pubsub" {"xmlns" pubsub-uri}
                                      ["items" {"node" microblog-uri}
                                       ["item" {"id" (:_id activity)}]]])}
                  packet (tigase/make-packet packet-map)
                  request (assoc (packet/make-request packet)
                            :serialization :xmpp)
                  response (filter-action #'actions.activity/show request)]
              (is (activity? response)))))))
    (testing "when the activity does not exist"
      (testing "should return nil" :pending))))
