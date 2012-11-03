(ns jiksnu.routes.user-routes
  (:use [clojurewerkz.route-one.core :only [add-route! named-path]]
        [jiksnu.routes.helpers :only [formatted-path]])
  (:require [jiksnu.actions.user-actions :as user]))

(add-route! "/main/register"      {:named "register page"})
(add-route! "/users"              {:named "index users"})
(add-route! "/users/:id/discover" {:named "discover user"})
(add-route! "/users/:id/update"   {:named "update user"})
(add-route! "/main/profile"       {:named "user profile"})
(add-route! "/main/xrd"           {:named "user meta"})
(add-route! "/model/users/:id"    {:named "user model"})

(defn routes
  []
  [
   [[:get    (named-path     "register page")]  #'user/register-page]
   [[:post   (named-path     "register page")]  #'user/register]
   [[:get    (named-path     "user profile")]   #'user/profile]
   [[:post   (named-path     "user profile")]   #'user/update-profile]
   [[:get    (formatted-path "index users")]    #'user/index]
   [[:get    (named-path     "index users")]    #'user/index]
   [[:post   (formatted-path "discover user")]  #'user/discover]
   [[:post   (named-path     "discover user")]  #'user/discover]
   [[:post   (formatted-path "update user")]    #'user/update]
   [[:post   (named-path     "update user")]    #'user/update]
   [[:get    (named-path     "user meta")]      #'user/user-meta]
   [[:get    (formatted-path "user model")]     #'user/show]

   [[:get    "/api/friendships/exists.:format"] #'user/exists?]
   [[:get    "/api/people/@me/@all"]            #'user/index]
   [[:get    "/api/people/@me/@all/:id"]        #'user/show]
   [[:delete "/users/:id"]                      #'user/delete]
   [[:post   "/users/:id/delete"]               #'user/delete]
   ;; [[:post   "/users/:id/update-hub"]           #'user/update-hub]
   [[:post   "/:username"]                      #'user/update]])
