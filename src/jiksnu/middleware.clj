(ns jiksnu.middleware
  (:use [ciste.config :only [config]]
        [clojure.stacktrace :only [print-stack-trace]]
        [jiksnu.session :only [with-user-id]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clj-statsd :as s]
            [jiksnu.ko :as ko]
            [lamina.trace :as trace])
  (:import javax.security.auth.login.LoginException))

(defn wrap-user-binding
  [handler]
  (fn [request]
    (with-user-id (-> request :session :id)
      (handler request))))

(defn auth-exception
  [ex]
  {:status 303
   :template false
   :flash "You must be logged in to do that."
   :headers {"location" "/main/login"}})

(defn wrap-authentication-handler
  [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :authentication] ex
       (auth-exception ex))
     (catch [:type :permission] ex
       (auth-exception ex))
     (catch LoginException ex
       (auth-exception ex)))))

(defn wrap-stacktrace
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (trace/trace "errors:handled" ex)
        (try
          (let [st (with-out-str (print-stack-trace ex))]
            (println st)
            {:status 500
             :headers {"content-type" "text/plain"}
             :body st})
          (catch Exception ex
            #_(trace/trace "errors:handled" ex)
            (log/fatalf "Error parsing exception: %s" (str ex))))))))

(defn default-html-mode
  []
  (config :htmlOnly))

(defn wrap-dynamic-mode
  [handler]
  (fn [request]
    (let [params (-> request :params)]
      (let [dynamic? (not (Boolean/valueOf (get params :htmlOnly (default-html-mode))))]
        (binding [ko/*dynamic* dynamic?]
          (handler request))))))

(defn wrap-stat-logging
  [handler]
  (fn [request]
    (s/increment "requests handled")
    (handler request)))
