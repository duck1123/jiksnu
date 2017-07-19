(ns jiksnu.modules.web.middleware
  (:require [ciste.config :refer [config]]
            [taoensso.timbre :as timbre]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.string :as string]
            [jiksnu.modules.core.metrics :as metrics]
            [jiksnu.modules.core.model.access-token :as model.access-token]
            [jiksnu.modules.core.model.request-token :as model.request-token]
            [jiksnu.modules.core.model.client :as model.client]
            [jiksnu.session :refer [with-user-id]]
            [slingshot.slingshot :refer [try+]])
  (:import javax.security.auth.login.LoginException))

(defn wrap-user-binding
  [handler]
  (fn [request]
    (let [username (get-in request [:session :cemerick.friend/identity :current])
          id (when username (str "acct:" username "@" (config :domain)))]
      (with-user-id id
        (handler request)))))

(defn auth-exception
  [_]
  {:status 401
   :template false
   :flash "You must be logged in to do that."
   :headers {"location" "/main/login"}})

(defn wrap-authentication-handler
  [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :authentication] ex
       (timbre/warn ex "Auth error")
       (auth-exception ex))
     (catch [:type :authorization] ex
       (timbre/warn ex "Auth error")
       (auth-exception ex))
     (catch [:type :permission] ex
       (auth-exception ex))
     (catch LoginException ex
       (auth-exception ex)))))

(defn authorization-header
  "Given an auth map, returns an authorization header"
  [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=\"" v "\"")))
       (string/join ",")
       (str "OAuth ")))

(def unparsed-types #{"BASIC" "Basic"})

(defn parse-authorization-header
  [header]
  (let [[type & parts] (string/split header #" |, ?")]
    (if (unparsed-types type)
      [type {}]
      (let [parts (->> parts
                       (map (fn [part]
                              (let [[k v] (string/split part #"=")
                                    v (string/replace v #"\"([^\"]+)\",?" "$1")]
                                [k v])))
                       (into {}))]
        [type parts]))))

(defn wrap-authorization-header
  [handler]
  (fn [request]
    (let [request
          (or
           (if-let [authorization (get-in request [:headers "authorization"])]
             (let [[type parts] (parse-authorization-header authorization)]
               (if (unparsed-types type)
                 request
                 (let [client (some-> parts (get "oauth_consumer_key") model.client/fetch-by-id)
                       token  (some-> parts (get "oauth_token") model.access-token/fetch-by-id)]
                   (merge request
                          {:authorization-type type
                           :authorization-parts parts}
                          (when client
                            {:authorization-client client})
                          (when token
                            {:access-token token}))))))
           request)]
      (handler request))))

(defn wrap-oauth-user-binding
  [handler]
  (fn [request]
    (handler
     (merge
      request
      (some-> request
              :access-token
              :request-token
              model.request-token/fetch-by-id)))))

(defn wrap-stacktrace
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (timbre/error ex "Unhandled error")
        (try
          (let [st (with-out-str (print-stack-trace ex))]
            {:status 500
             :headers {"content-type" "text/plain"}
             :body st})
          (catch Throwable ex
            (timbre/fatalf "Error parsing exception: %s" ex)))))))

(defn wrap-response-logging
  [handler]
  (fn [request]
    (metrics/with-trace :http-request
      (metrics/increment-counter! :request-handled)
      (timbre/with-context {:request (-> request
                                         (dissoc :async-channel)
                                         (dissoc :body)
                                         (dissoc :cemerick.friend/auth-config))}
        (timbre/debugf "%s %s" (:request-method request) (:uri request)))
      (handler request))))

(defn wrap-debug-param
  [handler]
  (fn [request]
    (let [debug (boolean (get-in request [:cookies "XDEBUG_SESSION"]))]
      (handler (assoc request :debug debug)))))
