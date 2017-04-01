(ns jiksnu.provider-methods
  (:require [clojure.tools.reader.edn :as edn]
            [jiksnu.protocols :as p]
            [taoensso.timbre :as timbre]))

(defn add-stream
  "Create a stream with the given name"
  [$http stream-name]
  (timbre/info "Creating Stream")
  (let [body #js {:name stream-name}]
    (-> (.post $http "/model/streams" body)
        (.then (fn [response]
                 (timbre/debugf "Response: %s" (js/JSON.stringify response))
                 (.-data response))))))

(defn connect
  "Establish a websocket connection"
  [app]
  (.send app "connect"))

(defn delete-stream
  "Delete the stream matching the id"
  [app id]
  (timbre/info "Deleting stream" id)
  (let [activity #js {:action "delete"
                      :object #js {:id id}}]
    (.post app activity)))

(defn fetch-status
  "Fetch the status from the server"
  [app]
  (timbre/debug "fetching app status")
  (.. app
      (inject "$http")
      (get "/status")
      (then (fn [response]
              (timbre/debug "setting app status")
              (set! (.-data app) (.-data response))))))

(defn post
  "Create a new activity"
  [$http activity & [pictures]]
  (let [path "/model/activities"
        form-data (js/FormData.)]

    (js/console.info "pictures" pictures)

    (.forEach
     js/angular activity
     (fn [v k]
       (timbre/debugf "Adding parameter: %s => %s" k v)
       (.append form-data k v)))

    (doseq [picture pictures]
      (js/console.info "Picture" picture)
      (.append form-data "pictures[]" picture))

    (timbre/infof "Posting Activity - %s" (js/JSON.stringify activity))
    (.post $http path form-data
           #js {:transformRequest (.-identity js/angular)
                :headers #js {"Content-Type" js/undefined}})))

(defn follow
  "Follow the target user"
  [app target]
  (timbre/debug "follow" target)
  (if target
    (let [object  #js {:id (.-_id target)}
          activity #js {:verb "follow" :object object}]
      (.post app activity))
    (let [$q (.inject app "$q")]
      (timbre/warn "No target")
      ($q (fn [_ reject] (reject))))))

(defn following?
  "Is the currently authenticated user following the target user"
  [app target]
  (.. app getUser
      (then (fn [user]
              (let [response (= (.-_id user) (.-_id target))]
                (timbre/debugf "following?: %s" response)
                response)))))

(defn get-user
  "Return the authenticated user"
  [app]
  (let [$q (.inject app "$q")
        Users (.inject app "Users")]
    ($q (fn [resolve reject]
          (let [id (.getUserId app)]
            (timbre/debugf "getting user: %s" id)
            (if id
              (resolve (.find Users id))
              (resolve ($q #(% nil)))))))))

(defn get-user-id
  "Returns the authenticated user id from app data"
  [app]
  (if-let [data (.-data app)]
    (if-let [username (.-user data)]
      (if-let [domain (.-domain data)]
        (str "acct:" username "@" domain)
        (do
          (timbre/warn "No domain")
          nil))
      (do
        (timbre/warn "could not get authenticated user id")
        nil))
    (do
      (timbre/warn "Attempted to get user id, but data not loaded")
      nil)))

(defn get-websocket-url
  "Determine the websocket connection url for this app"
  [$location]
  (let [host (.host $location)
        secure?  (= (.protocol $location) "https")
        scheme (str "ws" (when secure? "s"))
        port (.port $location)
        port-suffix (if (or (and secure? (= port 443))
                            (and (not secure?) (= port 80)))
                      "" (str ":" port))]
    (str scheme "://" host port-suffix "/")))

(defn go
  "Navigate to the named state"
  [app state]
  (.. app
      (inject "$state")
      (go state)))

(defmulti handle-action
  "Handler action response notifications"
  (fn [app data] (.-action data))
  :default :default)

(defmethod handle-action "like"
  [app data]
  (let [message (.-content (.-body data))]
    (.. app
        (inject "$mdToast")
        (showSimple message))))

(defmethod handle-action "error"
  [app data]
  (let [message #js {:message (or (some-> data .-message edn/read-string :msg) "Error")}]
    (.. app
        (inject "$mdToast")
        (showSimple message))))

(defmethod handle-action "delete"
  [app data]
  (let [message (str "Deleted item: " (js/JSON.stringify (.-action data)))]
    (.. app
        (inject "$mdToast")
        (showSimple message))))

(defmethod handle-action :default
  [app data]
  (let [message (str "Unknown message: " (.stringify js/JSON data))]
    (.. app
        (inject "$mdToast")
        (showSimple message))))

(defn on-connection-established
  [app data])

(defn handle-message
  "Handler for incoming messages from websocket connection"
  [app message]
  (let [$mdToast (.inject app "$mdToast")
        data-str (.-data message)
        data (js/JSON.parse data-str)]
    (timbre/debugf "Received Message - %s" data-str)
    (cond
      ;; (.-connection data) (.success Notification "connected")
      (.-action data)     (handle-action app data)
      :default            nil #_(.warning Notification (str "Unknown message: " data-str)))))

(defn invoke-action
  [app model-name action-name id]
  (timbre/debugf "Invoking Action. %s(%s)=>%s" model-name id action-name)
  (let [msg (str "invoke-action \""
                 model-name
                 "\", \""
                 action-name
                 "\", \""
                 id
                 "\"")]
    (.send app msg)))

;; TODO: Find a cljs version of this check
(defn response-ok?
  [response]
  (let [status (.-status response)]
    (and (<= 200 status) (> 299 status))))

(defn login
  "Authenticate session"
  [$http $httpParamSerializerJQLike username password]
  (let [data ($httpParamSerializerJQLike #js {:username username :password password})
        opts #js {:headers #js {"Content-Type" "application/x-www-form-urlencoded"}}]
    ;; (timbre/infof "Logging in user. %s:%s" username password)
    (-> $http
        (.post "/main/login" data opts)
        (.then response-ok?))))

(defn logout
  "Log out the authenticated user"
  [app]
  (let [$http (.inject app "$http")]
    (-> (.post $http "/main/logout")
        (.then (fn [data]
                 (set! (.-user app) nil)
                 (.fetchStatus app))))))

(defn ping
  "Send a ping command"
  [app]
  (.send app "ping"))

(defn refresh
  "Send a signal for collections to refresh themselves"
  [app]
  (let [$rootScope (.inject app "$rootScope")]
    (.$broadcast $rootScope "updateCollection")))

(defn register
  "Register a new user"
  [$http params]
  (timbre/debugf "Registering - %s" (.-reg params))
  (let [params #js {:method "post"
                    :url    "/main/register"
                    :data   (.-reg params)}]
    (-> ($http params)
        (.then (fn [data]
                 (timbre/debug "Response" data)
                 data)))))

(defn send
  "Send a command over the websocket connection"
  [connection command]
  (timbre/debugf "Sending command: %s" command)
  (.send connection command))

(defn unfollow
  "Remove a subscription to target"
  [app target]
  (timbre/debug "unfollow - %s" target)
  (let [object #js {:id (.-_id target)}
        activity #js {:verb "unfollow" :object object}]
    (.post app activity)))

(defn update-page
  "Notify a page update"
  [$mdToast Pages message]
  (let [conversation-page (.get Pages "conversations")]
    (.unshift (.-items conversation-page) (.-body message))
    (.showSimple $mdToast "Adding to page")))
