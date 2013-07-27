(ns jiksnu.actions.setting-actions
  (:use [ciste.config :only [config describe-config set-config! write-config!]]
        [ciste.core :only [defaction]])
  (:require [jiksnu.session :as session]))

(defaction oauth-apps
  []

  )

(describe-config [:site :brought-by :name]
  String
  "The name of the organization running this service")

(describe-config [:site :brought-by :url]
  String
  "The url of the organization running this service")

(describe-config [:site :closed]
  Boolean
  "Can new users register?")

(describe-config [:site :default :language]
  String
  "The default language to use")

(describe-config [:site :default :timezone]
  String
  "The default timezone of this server")

(describe-config [:site :email]
  String
  "The email address of the administrator")

(describe-config [:site :invite-only]
  Boolean
  "Can users invite new users")

(describe-config [:site :limit :text]
  Number
  "The maximum length of post to accept. (-1 = unlimited)")

(describe-config [:site :name]
  String
  "The name of the service")

(describe-config [:site :private]
  Boolean
  "Can unauthenticated users view this site?")

(describe-config [:site :theme]
  String
  "The name of the theme to use")


(defaction config-output
  []
  {:site
   {
    :name (config :site :name)
    :server (config :domain)
    ;; TODO: theme name
    :theme (config :site :theme)
    ;; TODO: logo
    :logo ""
    :fancy "1"
    ;; TODO: default language
    :language (config :site :default :language)
    ;; TODO: email
    :email (config :site :email)
    :broughtby (config :site :brought-by :name)
    :broughtbyurl (config :site :brought-by :url)
    :timezone (config :site :default :timezone)
    :closed (config :site :closed)
    :inviteonly (config :site :invite-only)
    :private (config :site :private)
    :textlimit (config :site :limit :text)
    :ssl "sometimes"
    :sslserver (config :domain)
    :shorturllength 30
    }
   :license {
             :type "cc"
             :owner nil
             :url "http://creativecommons.org/licenses/by/3.0/"
             :title "Creative Commons Attribution 3.0",
             :image "http://i.creativecommons.org/l/by/3.0/80x15.png"
             }
   :nickname {
              :featured ["daniel"]

              }
   :profile {:biolimit nil}
   :group {:desclimit nil}
   :notice {:contentlimit nil}
   :throttle {
              :enabled true
              :count 20
              :timespan 600

              }
   :xmpp {
          :enabled true
          :server (config :domain)
          :port 5222
          :user "update"
          }
   :integration {:source "jiksnu"}
   :attachments {
                 :upload true
                 :file_quota 2097152
                 }
   })

(defn avatar-page
  [user]
  {:user user})
