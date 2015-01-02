(ns jiksnu.modules.admin.actions.subscription-actions-test
  (:require [jiksnu.mock :as mock]
            [jiksnu.model :as model]
            [jiksnu.model.subscription :as model.subscription]
            [jiksnu.modules.admin.actions.subscription-actions :refer [delete]]
            [jiksnu.test-helper :refer [test-environment-fixture]]
            [midje.sweet :refer [=> fact falsey truthy]]))

(test-environment-fixture

 (fact #'delete
   (let [subscription (mock/a-subscription-exists)]
     (model.subscription/fetch-by-id (:_id subscription)) => truthy
     (delete subscription)
     (model.subscription/fetch-by-id (:_id subscription)) => falsey))

 )
