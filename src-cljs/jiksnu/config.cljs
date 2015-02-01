(ns jiksnu.config
  (:require jiksnu.app
            [jiksnu.helpers :as helpers]
            jiksnu.providers)
  (:use-macros [gyr.core :only [def.config]]
               [purnam.core :only [? ?> ! !> f.n def.n do.n
                                   obj arr def* do*n def*n f*n]]))

(def.config jiksnu [$stateProvider $urlRouterProvider $locationProvider
                    appProvider wsProvider DSProvider]

  (.setUrl wsProvider "wss://renfer.name/")

  (! DSProvider.defaults.idAttribute "_id")
  (! DSProvider.defaults.baseUrl "/model")

  (.otherwise $urlRouterProvider "/")
  (-> $locationProvider
      (.hashPrefix "!")
      (.html5Mode true))
  (helpers/add-states $stateProvider helpers/states))
