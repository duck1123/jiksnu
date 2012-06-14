(ns jiksnu.filters.auth-filters-test
  (:use [ciste.core :only [with-serialization]]
        [ciste.filters :only [filter-action]]
        [clj-factory.core :only [factory]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [midje.sweet :only [fact future-fact => every-checker]])
  (:require [jiksnu.actions.auth-actions :as actions.auth]))

(test-environment-fixture

 (fact "filter-action #'login :http"
   (with-serialization :http
     (let [request {:params {:username .username.
                             :password .password.}}]
       (filter-action #'actions.auth/login request))) => .response.
       (provided
         (actions.auth/login .username. .password.) => .response. :times 1))
 
 )
