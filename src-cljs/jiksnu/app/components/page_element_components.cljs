(ns jiksnu.app.components.page-element-components
  (:require [jiksnu.app :refer [jiksnu]]
            [jiksnu.app.helpers :as helpers]
            [jiksnu.app.protocols :as p]
            jiksnu.app.services
            [jiksnu.registry :as registry]
            [taoensso.timbre :as timbre]))

(defn AsModelController
  [$scope DS]
  (let [collection-name $scope.model]
    (set! $scope.init
          (fn [id]
            (timbre/debugf "init as model %s(%s)" collection-name id)
            (.bindOne DS collection-name id $scope "item")
            (.find DS collection-name id)))
    (.init $scope $scope.id)))

(.component
 jiksnu "asModel"
 #js {:bindings #js {:id "<" :model "@"}
      :controller #js ["$scope" "DS" AsModelController]
      :template "<span ng-transclude></span>"
      :transclude true})

(defn DebugController
  [$ctrl $scope $filter app]
  (set! $scope.visible #(.. app -data -debug))
  (set! $ctrl.$onChanges #(do
                            (js/console.debug "change expr: " %)
                            (when (.-id %) (.init $scope))))
  (set! $scope.init
        (fn []
          (timbre/debugf "Debugging expr: %s" (js/JSON.stringify $ctrl.expr))
          (set! $scope.formattedCode #(($filter "json") $ctrl.expr))))
  (.init $scope))

(.component
 jiksnu "debug"
 #js {:bindings #js {:expr "=expr" :exprText "@expr"}
      :templateUrl "/templates/debug"
      :controller
      #js ["$scope" "$filter" "app"
           (fn [$scope $filter app]
             (this-as $ctrl (DebugController $ctrl $scope $filter app)))]})

(defn DisplayAvatarController
  [$ctrl $scope Users]
  (set! $ctrl.$onChanges #(when (.-id %) (.init $scope)))
  (set! $scope.init
        (fn []
          (set! $scope.size (or $ctrl.size 32))
          (when-let [id $ctrl.id]
            (when (seq id)
              (timbre/debugf "Displaying avatar for %s" id)
              (.bindOne Users id $scope "user")
              (.find Users id)))))
  (.init $scope))

(.component
 jiksnu "displayAvatar"
 #js {:bindings #js {:id "@" :size "@"}
      :controller #js ["$scope" "Users"
                       (fn [$scope Users]
                         (this-as $ctrl (DisplayAvatarController $ctrl $scope Users)))]
      :templateUrl "/templates/display-avatar"})

(defn FollowButtonController
  [$scope app $q $rootScope Subscriptions]
  (set! $scope.app app)
  (set! $scope.loaded false)

  (set! $scope.isActor
        (fn []
          (set! (.-authenticated $scope)
                (some-> app p/get-user-id (= (some-> $scope .-item .-_id))))))

  (set! $scope.init
        (fn []
          (let [actor (.isActor $scope)]
            (set! $scope.actor actor)
            (when-not actor
              (-> (.isFollowing $scope)
                  (.then (fn [following]
                           (when following (.log following "info"))
                           (set! $scope.following following)
                           (set! $scope.followLabel
                                 (if $scope.following "Unfollow" "Follow"))
                           (set! $scope.loaded true))))))))

  (set! $scope.isFollowing
        (fn []
          ($q
           (fn [resolve reject]
             (if-let [user $scope.item]
               (let [user-id user._id]
                 (-> (p/get-user app)
                     (.then #(some-> % .getFollowing))
                     (.then (fn [page]
                              (when page
                                (->> page.items
                                     (map Subscriptions.find)
                                     clj->js
                                     (.all $q)))))
                     (.then (fn [subscriptions]
                              (some #(#{user-id} (.-to %)) subscriptions)))
                     (.then resolve)))
               (reject))))))

  (set! (.-submit $scope)
        (fn []
          (let [item $scope.item]
            (-> (if $scope.following
                  (p/unfollow app item)
                  (p/follow app item))
                (.then (fn []
                         (.init $scope)
                         (.$broadcast $rootScope helpers/refresh-followers)))))))

  (.$watch $scope
           (fn [] app.data)
           (fn [data old-data] (.init $scope)))
  (.$on $scope helpers/refresh-followers $scope.init))

(.controller
 jiksnu "FollowButtonController"
 #js ["$scope" "app" "$q" "$rootScope" "Subscriptions" FollowButtonController])

(.component
 jiksnu "followButton"
 #js {:controller "FollowButtonController"
      :bindings #js {:item "<"}
      :templateUrl "/templates/follow-button"})

(defn swagger-url
  [protocol hostname port]
  (let [secure? (= protocol "https")
        default-port? (or (and secure?       (= port 443))
                          (and (not secure?) (= port 80)))]
    (str protocol "://" hostname
         (when-not default-port? (str ":" port))
         "/api-docs.json")))

(defn MainLayoutController
  [$location $mdSidenav $scope app]
  (let []
    (set! $scope.getSwaggerUrl
          (fn []
            (let [protocol (.protocol $location)
                  hostname (.host $location)
                  port (.port $location)]
              (swagger-url protocol hostname port))))
    (set! $scope.apiUrl (str "/vendor/swagger-ui/dist/index.html?url=" (.getSwaggerUrl $scope)))
    (set! $scope.$mdSidenav $mdSidenav)
    (set! $scope.logout #(p/logout app))))

(.component
 jiksnu "mainLayout"
 #js {:templateUrl "/templates/main-layout"
      :controller #js ["$location" "$mdSidenav" "$scope" "app" MainLayoutController]})

(defn NavBarController
  [$mdSidenav $rootScope $scope $state app hotkeys]
  (set! $scope.app2            app)
  (set! $scope.loaded          false)
  (set! $scope.logout          #(p/logout app))
  (set! $scope.logout          app.logout)
  (set! $scope.navbarCollapsed true)

  (helpers/setup-hotkeys hotkeys $state)

  (.$on $rootScope "$stateChangeSuccess"
        (fn [] (.close ($mdSidenav "left"))))

  (set! $scope.init
        (fn [auth-data]
          (when $scope.loaded
            (set! $scope.app auth-data)
            (-> (p/get-user app)
                (.then (fn [user] (set! app.user user)))))))

  (set! $scope.toggleSidenav (fn [] (.toggle ($mdSidenav "left"))))

  (.$watch $scope #(.-data app) $scope.init)

  (-> (p/fetch-status app)
      (.then (fn [] (set! $scope.loaded true)))))

(.controller
 jiksnu "NavBarController"
 #js ["$mdSidenav" "$rootScope" "$scope" "$state" "app" "hotkeys" NavBarController])

(.component
 jiksnu "navBar"
 #js {:controller "NavBarController"
      :templateUrl "/templates/navbar-section"})

(defn SidenavController
  [$scope app]
  (set! $scope.logout #(p/logout app))
  (set! $scope.app    app)

  (set! $scope.items
        (clj->js
         (map (fn [[label ref]] {:label label :ref ref}) registry/sidenav-data))))

(.component
 jiksnu "sidenav"
 #js {:templateUrl "/templates/sidenav"
      :controller #js ["$scope" "app" SidenavController]})

(.component
 jiksnu "spinner"
 #js {:templateUrl "/templates/spinner"})

(defn SubpageController
  [$ctrl $scope subpageService $rootScope]
  (set! $scope.loaded false)
  (let [subpage (or (some-> $ctrl .-subpage) (throw "Subpage not specified"))]
    (set! $scope.refresh (fn [] (.init $scope $scope.item)))

    (set! $ctrl.$onChanges
          (fn [changes]
            (when-let [item (some-> changes .-item .-currentValue)]
              (.init $scope item))))
    (.$on $scope "refresh-page" (fn [] (.refresh $scope)))

    (set! $scope.init
          (fn [item]
            (if item
              (let [model-name (.. item -constructor -name)
                    id (.-_id item)]
                (set! $scope.item    item)
                (set! $scope.loaded  false)
                (set! $scope.loading true)
                (timbre/debugf "Refreshing subpage: %s(%s)=>%s" model-name id subpage)
                (-> (.fetch subpageService item subpage)
                    (.then
                     (fn [page]
                       (set! $scope.errored false)
                       (set! $scope.loaded  true)
                       (set! $scope.loading false)
                       (set! $scope.page    page)
                       page)
                     (fn [page]
                       (timbre/errorf "Failed to load subpage. %s(%s)=>%s" model-name id subpage)
                       (set! $scope.errored true)
                       (set! $scope.loading false)
                       page)))))))
    (.refresh $scope)))

(.component
 jiksnu "subpage"
 #js {:bindings #js {:subpage "@name" :item "<"}
      :template "<div ng-transclude></div>"
      :transclude true
      :controller
      #js ["$scope" "subpageService" "$rootScope"
           (fn [$scope subpageService $rootScope]
             (this-as $ctrl (SubpageController $ctrl $scope subpageService $rootScope)))]})
