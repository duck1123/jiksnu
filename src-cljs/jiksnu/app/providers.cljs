(ns jiksnu.app.providers
  (:require [inflections.core :as inf]
            [jiksnu.app :refer [jiksnu models]]
            [jiksnu.app.protocols :refer [AppProtocol] :as p]
            [jiksnu.app.provider-methods :as methods]
            [jiksnu.registry :as registry]
            [taoensso.timbre :as timbre]))

(def app-methods
  {:connect       methods/connect
   :fetchStatus   p/fetch-status
   :follow        methods/follow
   :getUserId     p/get-user-id
   :go            p/go
   :handleMessage methods/handle-message
   :invokeAction  p/invoke-action
   :isFollowing   methods/following?
   :login         p/login
   :logout        p/logout
   :ping          methods/ping
   :post          methods/post
   :refresh       methods/refresh
   :register      p/register
   :send          p/send
   :unfollow      p/unfollow})

(defn get-websocket-connection
  "Create a websocket connection to the server"
  [app]
  (let [$websocket (.inject app "$websocket")
        websocket-url (p/get-websocket-url app)
        connection ($websocket websocket-url)]
    (doto connection
      (.onMessage (partial p/handle-message app))
      (.onOpen (fn []
                 (timbre/debug "Websocket connection opened")))
      (.onClose (fn []
                  (timbre/debug "Websocket connection closed")
                  (.reconnect connection)))
      (.onError (fn []
                  (timbre/warn "Websocket connection errored"))))))

(deftype AppProvider
    [inject]

  AppProtocol

  (add-stream [app stream-name]
    (let [$http (.inject app "$http")]
      (methods/add-stream $http stream-name)))

  (connect [app])

  (delete-stream [app target-id]
    (let [$http (.inject app "$http")]
      (methods/delete-stream $http target-id)))

  (fetch-status [app]
    (let [$http (.inject app "$http")]
      (-> (methods/fetch-status $http)
          (.then (fn [data] (set! (.-data app) data))))))

  (follow [app target]
    (let [$q (.inject app "$q")
          $http (.inject app "$http")]
      (methods/follow $q $http target)))

  (following? [app target])

  (get-user [app]
    (let [$q (.inject app "$q")
          Users (.inject app "Users")
          data app.data]
      (timbre/debugf "app.data: %s" (js/JSON.stringify data))
      (methods/get-user $q Users data)))

  (get-user-id [app]
    (methods/get-user-id app.data))

  (get-websocket-url [app]
    (let [$location (.inject app "$location")]
      (methods/get-websocket-url $location)))

  (go [app state]
      (let [$state (.inject app "$state")]
        (methods/go $state state)))

  (handle-action [app data])

  (handle-message [app message])

  (inject [app atom])

  (invoke-action [app collection-name action-name id]
    (let [connection (.-connection app)]
      (methods/invoke-action connection collection-name action-name id)))

  (login [app username password]
    (let [$http (.inject app "$http")
          $httpParamSerializerJQLike (.inject app "$httpParamSerializerJQLike")]
      (-> (methods/login $http $httpParamSerializerJQLike username password)
          (.then (fn [] (.fetchStatus app))))))

  (logout [app]
    (let [$http (.inject app "$http")]
      (-> (methods/logout $http)
          (.then (fn [data]
                   (set! app.user nil)
                   (.fetchStatus app))))))

  (post [app activity pictures]
    (let [$http (.inject app "$http")]
      (methods/post $http activity pictures)))

  (register [app params]
    (let [$http (.inject app "$http")]
      (methods/register $http params)))

  (send [app command]
    (let [connection app.connection]
      (methods/send connection command)))

  (unfollow [app target]
    (methods/unfollow app target))

  (update-page [app message]
    (let [$mdToast (.inject app "$mdToast")
          Pages (.inject app "Pages")]
      (methods/update-page $mdToast Pages message))))

(defn app
  []
  (let [f (fn [$injector]
            (timbre/debug "creating app service")
            (let [$inject $injector.get
                  app (AppProvider. $inject)]
              (doseq [[n f] app-methods]
                (aset app (name n) (partial f app)))

              (set! app.connection (get-websocket-connection app))
              (set! app.data       #js {})

              ;; Bind to window for easy debugging
              (set! (.-app js/window) app)

              (doseq [[k _] registry/page-mappings]
                (let [model-name (inf/camel-case k)]
                  (swap! models assoc model-name ($inject model-name))))

              ;; return the app
              app))]
    (clj->js {:$get ["$injector" f]})))

(.provider jiksnu "app" app)
