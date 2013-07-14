(ns jiksnu.sections.activity-sections-test
  (:use [ciste.config :only [with-environment]]
        [ciste.core :only [with-context with-format with-serialization]]
        [ciste.sections.default :only [index-block index-section
                                       show-section uri]]
        [clj-factory.core :only [factory]]
        [jiksnu.test-helper :only [check context future-context test-environment-fixture]]
        jiksnu.session
        jiksnu.sections.activity-sections
        [midje.sweet :only [=>]])
  (:require [clj-tigase.element :as element]
            [clojure.tools.logging :as log]
            [hiccup.core :as h]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.ko :as ko]
            [jiksnu.mock :as mock]
            [jiksnu.features-helper :as feature]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.User
           org.apache.abdera.i18n.iri.IRI
           org.apache.abdera.model.Entry
           org.apache.abdera.model.Person
           org.joda.time.DateTime
           tigase.xml.Element))

(test-environment-fixture

 (future-context #'like-button
   (like-button (factory :activity)) =>
   (check [response]
     response => vector?

     ;; TODO: This checks that the first element is a form. This is no
     ;; longer a good test.
     (first response) => :form))

 (context #'posted-link-section
   (context "when the serialization is :http"
     (with-serialization :http

       (context "when the format is :html"
         (with-format :html

           (context "when dynamic is false"
             (binding [ko/*dynamic* false]

               (context "when given an empty activity"
                 (let [item (Activity.)]
                   (posted-link-section item) =>
                   (check [response]
                     (let [resp-str (h/html response)]
                       resp-str => string?))))

               (context "when given a real activity"
                 (let [activity (mock/there-is-an-activity)]
                   (posted-link-section activity) =>
                   (check [response]
                     (let [resp-str (h/html response)]
                       resp-str => string?))))
               ))
           ))
       ))
   )

 (context #'uri
   (context "Activity"
     ;; TODO: not a good test
     (with-context [:http :html]
       (uri .activity.))) => string?)

 (context #'index-block
   (context "when the context is [:http :rdf]"
     (with-context [:http :rdf]
       (let [activity (mock/there-is-an-activity)]
         (index-block [activity]) =>
         (check [response]
           response => (partial every? (fn [t]
                                         (and (vector? t)
                                              (= 3 (count t))))))))))

 (context #'index-section
   (context "Activity"
     (context "when the context is [:http :rdf]"
       (with-context [:http :rdf]
         (let [activity (mock/there-is-an-activity)]
           (index-section [activity]) =>
           (check [r]
             (doseq [t r]
               t => vector?
               (count t) => 3)))))))

 (context #'show-section
   (context "Activity"
     (context "when the serialization is :http"
       (with-serialization :http

         (context "when the format is :atom"
           (with-format :atom

             (context "when given a real activity"
               (let [activity (mock/there-is-an-activity)]
                 (show-section activity) =>
                 (check [response]
                   response => (partial instance? Entry)
                   (.getId response) => (partial instance? IRI)
                   ;; (.getUpdated response) => (partial instance? org.apache.abdera.model.DateTime)
                   (.getTitle response) => string?
                   (.getAuthor response) => (partial instance? Person))))

             (context "when given a partial activity"
               (let [activity (factory :activity)]
                 (show-section activity) =>
                 (check [response]
                   response => (partial instance? Entry))))
            ))

         (context "when the format is :html"
           (with-format :html

             (context "when dynamic is false"
               (binding [ko/*dynamic* false]

                 (context "when given an empty activity"
                   (let [item (Activity.)]
                     (show-section item) =>
                     (check [response]
                       (let [resp-str (h/html response)]
                         resp-str => string?))))

                 (context "when given a real activity"
                   (let [activity (mock/there-is-an-activity)]
                     (show-section activity) =>
                     (check [response]
                       (let [resp-str (h/html response)]
                         resp-str => string?))))
                 ))
             ))
         )))

   (context ":xmpp"
     (let [activity (mock/there-is-an-activity)]
       (with-context [:xmpp :xmpp]
         (show-section activity))) => element/element?)
   )
 )
