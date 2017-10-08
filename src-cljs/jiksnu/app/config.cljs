(ns jiksnu.app.config
  (:require [jiksnu.app :refer [jiksnu]]
            [jiksnu.app.helpers :as helpers]
            jiksnu.app.providers
            [jiksnu.registry :as registry]
            [taoensso.timbre :as timbre]))

(defn jiksnu-config
  [$stateProvider $urlRouterProvider $locationProvider DSProvider
   DSHttpAdapterProvider hljsServiceProvider $mdThemingProvider]

  (.setOptions hljsServiceProvider #js {:tabReplace "  "})

  (-> $mdThemingProvider
      (.theme "default")
      (.primaryPalette registry/pallete-color))

  (js/angular.extend (.-defaults DSProvider)
                     #js {:idAttribute "_id"
                          :basePath    "/model"
                          :afterFind (fn [Resource data cb]
                                       (timbre/debugf "data: %s" (js/JSON.stringify data))
                                       (cb nil data))
                          :afterFindAll (fn [Resource data cb]
                                          (timbre/debugf "data: %s" (js/JSON.stringify data))
                                          (cb nil data))})

  ;; (js/angular.extend (.-defaults DSHttpAdapterProvider)
  ;;                    #js {:log false})



  (.otherwise $urlRouterProvider "/")
  (-> $locationProvider
      (.hashPrefix "!")
      (.html5Mode true))
  (helpers/add-states $stateProvider registry/route-data))

(.config
 jiksnu
 #js ["$stateProvider" "$urlRouterProvider" "$locationProvider"
      "appProvider" "DSProvider" "DSHttpAdapterProvider"
      "hljsServiceProvider" "$mdThemingProvider" jiksnu-config])
