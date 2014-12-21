(ns jiksnu.modules.admin.views.group-views-test
  (:use [ciste.core :only [with-serialization with-format]]
        [ciste.filters :only [filter-action]]
        [ciste.views :only [apply-view]]
        [clj-factory.core :only [factory]]
        [jiksnu.ko :only [*dynamic*]]
        [jiksnu.test-helper :only [check context future-context test-environment-fixture]]
        [midje.sweet :only [=>]])
  (:require [clojure.tools.logging :as log]
            [hiccup.core :as h]
            [jiksnu.actions.group-actions :as actions.group]
            [jiksnu.db :as db]
            [jiksnu.modules.admin.actions.group-actions :as actions.admin.group]))

(test-environment-fixture

 (context "apply-view #'actions.admin.group/index"
   (let [action #'actions.admin.group/index]

     (context "when the serialization is :http"
       (with-serialization :http

         (context "when the format is :html"
           (with-format :html

             (future-context "In static mode"
               (binding [*dynamic* false]
                 (context "when there are groups"
                   (db/drop-all!)
                   (let [groups (doall
                                 (map (fn [n]
                                        (actions.group/create (factory :group)))
                                      (range 15)))
                         request {:action action}
                         response (filter-action action request)]
                     (apply-view request response) =>
                     (check [response]
                            response => map?
                            (let [body (h/html (:body response))]
                              body => #"groups"
                              (doseq [group groups]
                                (let [pattern (re-pattern (str (:_id group)))]
                                  body => pattern))))))))
             ))
         ))
     ))
 )
