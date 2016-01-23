(ns jiksnu.actions.group-actions-test
  (:require [clj-factory.core :refer [factory fseq]]
            [jiksnu.actions.group-actions :as actions.group]
            [jiksnu.model.group :as model.group]
            [jiksnu.test-helper :as th]
            [midje.sweet :refer :all])
  (:import jiksnu.model.Group))

(namespace-state-changes
 [(before :contents (th/setup-testing))
  (after :contents (th/stop-testing))])

(fact "#'actions.group/create"
  (fact "when given valid options"
    (fact "and the group does not already exist"
      (model.group/drop!)
      (let [params (factory :group)]
        (actions.group/create params) => #(instance? Group %)))
    ;; TODO: already exists
    )
  ;; TODO: invalid options
  )
