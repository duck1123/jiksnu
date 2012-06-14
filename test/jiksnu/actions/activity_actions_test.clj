(ns jiksnu.actions.activity-actions-test
  (:use [ciste.core :only [with-context]]
        [ciste.sections.default :only [show-section]]
        [clj-factory.core :only [factory fseq]]
        jiksnu.actions.activity-actions
        [jiksnu.test-helper :only [test-environment-fixture]]
        [jiksnu.session :only [with-user]]
        [midje.sweet :only [fact future-fact => every-checker throws truthy falsey]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.abdera :as abdera]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.Domain
           jiksnu.model.User))


(test-environment-fixture
 
 (fact "#'set-recipients"
   (fact "when there are no recipient uris"
     (fact "should return that activity"
       (let [activity (factory :activity)]
         (set-recipients activity) => activity)))
   (fact "When the activity contains a recipient uri"
     (let [recipient (model.user/create (factory :local-user))
           activity (factory :activity {:recipient-uris [(:id recipient)]})]
       (set-recipients activity) =>
       (every-checker
        #(= (:_id recipient) (first (:recipients %)))))))

 (fact "#'oembed->activity"
   (let [oembed-str (slurp "test-resources/oembed.json")]
     ;; TODO: complete
     oembed-str => string?))
 
 (fact "entry->activity"
   (let [domain-name (fseq :domain)
         domain (actions.domain/find-or-create (factory :domain
                                              {:discovered true
                                               :links [{:rel "lrdd"
                                                        :template (str "http://" domain-name "/lrdd?uri={uri}")}]
                                               :_id domain-name}))
         user (model.user/create (factory :user {:domain domain-name}))]

     ;; TODO: Load elements from resources
     (fact "should return an Activity"
       (with-context [:http :atom]
         (let [entry (show-section (model/map->Activity
                                    (factory :activity {:author (:_id user)})))]
           (entry->activity entry) => model/activity?)))
     
     (future-fact "when coming from an identi.ca feed"
       (fact "should parse the published field"
         (let [feed nil #_(abdera/load-file "identica-update.xml")
               entry (first (abdera/get-entries feed))]
           (entry->activity entry) => model/activity?
           #_(provided
               (.getId entry) => "1"))))))

 (fact "#'create"
   (fact "when the user is logged in"
     (fact "and it is a valid activity"
       (fact "should return that activity"
         (let [user (model.user/create (factory :local-user))]
           (with-user user
             (let [activity (factory Activity)]
               (create activity) => model/activity?)))))))

 (fact "#'post"
   (fact "when the user is not logged in"
     (fact "should return nil"
       (let [activity (factory Activity)]
         (post activity) => nil))))

 (fact "#'delete"
   (fact "when the activity exists"
     (fact "and the user owns the activity"
       (fact "should delete that activity"
         (let [user (model.user/create (factory :local-user))]
           (with-user user
             (let [activity (create (factory :activity {:author (:_id user)}))]
               (delete activity) => activity
               (model.activity/fetch-by-id (:_id activity)) => nil)))))
     (fact "and the user does not own the activity"
       (fact "should not delete that activity"
         (let [user (model.user/create (factory :local-user))
               activity (model.activity/create (factory Activity))]
           (with-user user
             (delete activity) => (throws RuntimeException)
             (model.activity/fetch-by-id (:_id activity)) => activity))))))

 (fact "#'viewable?"
   (fact "When it is public"
     (let [activity (factory :activity {:public true})]
       (viewable? activity .user.)) => truthy)
   (fact "when it is not public"
     (fact "when the user is the author"
       (let [user (model.user/create (factory :user))
             activity (model.activity/create
                       (factory :activity {:public false
                                           :author (:_id user)}))]
         (viewable? activity user)) => truthy)
     (fact "when the user is not the author"
       (fact "when the user is an admin"
         (let [user (model.user/create (factory :user {:admin true}))
               activity (model.activity/create (factory :activity {:public false}))]
           (viewable? activity user)) => truthy)
       (fact "when the user is not an admin"
         (let [user (model.user/create (factory :user))
               activity (model.activity/create (factory :activity {:public false}))]
           (viewable? activity user)) => falsey))))
 
 (fact "#'show"
   (fact "when the record exists"
     (fact "and the record is viewable"
       (let [activity (create (factory :activity))]
         (show activity) => activity
         (provided
           (viewable? activity) => true)))
     (fact "and the record is not viewable"
       (let [activity (create (factory Activity))]
         (show activity) => (throws RuntimeException)
         (provided
           (viewable? activity) => false)))))
 )
