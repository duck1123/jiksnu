(ns jiksnu.routes-helper
  (:use [clj-factory.core :only [factory fseq]]
        [clojure.core.incubator :only [-?> -?>>]]
        [lamina.core :only [channel wait-for-message]])
  (:require [clj-http.cookies :as cookies]
            [clojure.tools.logging :as log]
            [jiksnu.actions.auth-actions :as actions.auth]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.mock :as mock]
            [jiksnu.routes :as r]
            [lamina.time :as time]
            [ring.mock.request :as req]
            [ring.util.codec :as codec])
  (:import java.io.StringReader))

(defn response-for
  "Run a request against the main handler and wait for the response"
  ([request] (response-for request (time/seconds 5)))
  ([request timeout]
     (let [ch (channel)]
       (try
         (r/app ch request)
         (catch Exception ex
           (.printStackTrace ex)))
       (wait-for-message ch timeout))))

(defn get-auth-cookie
  [username password]
  (-?> (req/request :post "/main/login")
       (assoc :params {:username username
                       :password password})
       response-for
       :headers
       (get "Set-Cookie")
       cookies/decode-cookies
       (->> (map (fn [[k v]] [k (:value v)]))
            (into {}))
       codec/form-encode))

(defn as-user
  ([m]
     (let [user (mock/a-user-exists)]
       (as-user m user)))
  ([m user]
     (let [password (fseq :password)]
       (actions.auth/add-password user password)
       (as-user m user password)))
  ([m user password]
     (let [cookie-str (get-auth-cookie (:username user) password)]
       (assoc-in m [:headers "cookie"] cookie-str))))

(defn as-admin
  [m]
  (let [user (actions.user/create (factory :local-user {:admin true}))]
    (as-user m user)))
