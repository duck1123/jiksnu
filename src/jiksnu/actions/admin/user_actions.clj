(ns jiksnu.actions.admin.user-actions
  "This is the namespace for the admin pages for users"
  (:use [ciste.core :only [defaction]])
  (:require [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model :as model]
            [jiksnu.model.user :as model.user]
            [jiksnu.templates.actions :as templates.actions]))

(defaction create
  [options]
  (actions.user/create options))

(def index*
  (templates.actions/make-indexer 'jiksnu.model.user))

(defaction index
  [& [params & [options & _]]]
  (index* params options))

(defaction show
  [user]
  (actions.user/show user))
