(ns jiksnu.xmpp.controller.user-controller
  (:use jiksnu.namespace
        [jiksnu.session :only (current-user)]
        jiksnu.xmpp.view)
  (:require [jiksnu.model.user :as model.user])
  (:import tigase.xml.Element))

(defn rule-element?
  [^Element element]
  (= (.getName element) "acl-rule"))

(defn rule-map
  [rule]
  (let [^Element action-element (.getChild rule "acl-action")
        ^Element subject-element (.getChild rule "acl-subject")]
    {:subject (.getAttribute subject-element "type")
     :permission (.getAttribute action-element "permission")
     :action (.getCData action-element)}))

(defn property-map
  [user property]
  (let [child-elements (children property)
        rule-elements (filter rule-element? child-elements)
        type-element (first (filter (comp not rule-element?) child-elements))]
    {:key (.getName property)
     :type (.getName type-element)
     :value (.getCData type-element)
     :rules (map rule-map rule-elements)
     :user user}))

(defn process-vcard-element
  [element]
  (fn [vcard-element]
    (map (partial property-map (current-user))
         (children vcard-element))))

(defn show
  [request]
  (let [to (:to request)]
    (model.user/fetch-by-jid to)))

(defn create
  [request]
  (let [vcard-elements (:items request)]
    (doseq [property
            (flatten
             (map process-vcard-element
                  vcard-elements))]
      (model.user/create property))))

(defn delete
  [request]
  ;; TODO: implement
  '())

;; (defn index
;;   [request]
;;   '()
;;   )

(defn inbox
  [request]
  ;; TODO: limit this to the inbox of the user
  (model.user/inbox))

(defn fetch-remote
  [request]
  (model.user/fetch-by-jid (:to request)))

(defn remote-create
  [request]
  (let [{:keys [to from payload]} request]
    (let [user (model.user/fetch-by-jid from)]
      ;; (println "user: " user)
      (let [vcard (first (children payload))]
        (println "vcard: " vcard)
        (let [gender (.getCData (find-children vcard "/vcard/gender"))
              name (.getCData (find-children vcard "/vcard/fn/text"))
              first-name (.getCData (find-children vcard "/vcard/n/given/text"))
              last-name (.getCData (find-children vcard "/vcard/n/surname/text"))
              url (.getCData (find-children vcard "/vcard/url/uri"))
              avatar-url (.getCData (find-children vcard "/vcard/photo/uri"))]
          (let [new-user {:gender gender
                          :name name
                          :first-name first-name
                          :last-name last-name
                          :url url
                          :avatar-url avatar-url
                          }]
            (let [updated-user (jiksnu.model.user/update (merge user new-user))]
              (println "updated-user: " updated-user))
            (println "new-user: " new-user)))
        
       user))))
