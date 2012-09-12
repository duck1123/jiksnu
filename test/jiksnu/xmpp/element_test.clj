(ns jiksnu.xmpp.element-test
  (:use [ciste.config :only [with-environment]]
        [ciste.core :only [with-context]]
        [ciste.sections.default :only [show-section]]
        [clj-factory.core :only [factory]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [jiksnu.xmpp.element :only [abdera-to-tigase-element]]
        [midje.sweet :only [fact future-fact =>]])
  (:require [clj-tigase.element :as element]
            [jiksnu.model :as model]
            [jiksnu.namespace :as namespace]
            jiksnu.sections.activity-sections)
  (:import jiksnu.model.Activity))

(test-environment-fixture

 ;; just create a simple element here, don't involve activities
 (future-fact "abdera-to-tigase-element"
   (fact "should return a tigase element"
     (with-context [:xmpp :atom]
       (let [element (show-section (model/map->Activity (factory :activity)))]
         (abdera-to-tigase-element element) => element/element?)))))
