(ns jiksnu.filters.stream-filters-test
  (:use [clj-factory.core :only [factory fseq]]
        [ciste.config :only [config]]
        [ciste.core :only [with-format with-serialization]]
        [ciste.filters :only [filter-action]]
        [jiksnu.session :only [with-user]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [lamina.core :only [channel]]
        [midje.sweet :only [fact future-fact => every-checker truthy]])
  (:require [clj-tigase.core :as tigase]
            [clj-tigase.packet :as packet]
            [clojure.tools.logging :as log]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.stream-actions :as actions.stream]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.db :as db]
            [jiksnu.mock :as mock]
            jiksnu.filters.stream-filters
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Conversation
           jiksnu.model.User))

(test-environment-fixture

 (fact "filter-action #'actions.stream/public-timeline"
   (let [action #'actions.stream/public-timeline]

     (fact "when the serialization is :http"
       (with-serialization :http

         (fact "when the format is :html"
           (with-format :html

             (fact "when there are no activities"
               (db/drop-all!)
               (let [request {:action action}]
                 (filter-action action request) =>
                 (fn [response]
                   (fact
                     response => map?))))
             ))
         ))

     (fact "when the serialization is :xmpp"
       (with-serialization :xmpp

         (fact "when there are no activities"
           (db/drop-all!)
           (let [user (mock/a-user-exists)
                 element nil
                 packet (tigase/make-packet
                         {:from (tigase/make-jid user)
                          :to (tigase/make-jid user)
                          :type :get
                          :body element})
                 request (assoc (packet/make-request packet)
                           :serialization :xmpp)]
             (filter-action #'actions.stream/public-timeline request) =>
             (fn [response]
               (fact
                 response => map?
                 (:totalRecords response) => 0
                 (let [items (:items response)]
                   items => empty?)))))

         (fact "when there are activities"
           (let [author (mock/a-user-exists)]
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
                     activity (mock/there-is-an-activity)]
                 (filter-action #'actions.stream/public-timeline request) =>
                 (fn [response]
                   (fact
                     response => map?
                     (:totalRecords response) => 1
                     (let [items (:items response)]
                       (doseq [item items]
                         (class item) => Conversation))))))))
         ))))

 (fact "filter-action #'actions.stream/user-timeline"
   (let [action #'actions.stream/user-timeline]
     (fact "when the serialization is :http"
       (with-serialization :http
         (fact "when the user exists"
           (let [user (mock/a-user-exists)
                 request {:params {:id (str (:_id user))}}]
             (filter-action action request) => .response.
             (provided
              (actions.stream/user-timeline user) => .response.)))))))

 )
