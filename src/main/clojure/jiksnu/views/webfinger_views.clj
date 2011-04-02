(ns jiksnu.views.webfinger-views
  (:use ciste.core
        ciste.sections
        ciste.view
        hiccup.core
        ciste.config
        jiksnu.namespace
        jiksnu.controller.webfinger-controller
        jiksnu.sections.webfinger-sections))

(defview #'host-meta :html
  [request _]
  (let [domain (:domain (config))]
    {:template false
     :headers {"Content-Type" "application/xml"}
     :body
     (html ["XRD" {"xmlns" xrd-ns
              "xmlns:hm" host-meta-ns}
       ["hm:Host" domain]
       ["Link" {"rel" "lrdd"
                "template" (str "http://"
                                domain
                                "/main/xrd?uri={uri}")}
        ["Title" {} "Resource Descriptor"]]])}))

(defview #'user-meta :html
  [request user]
  {:template false
   :headers {"Content-Type" "application/xml"}
   :body
   (html ["XRD" {"xmlns" xrd-ns}
     ["Subject" {} (str "acct:" (:username user) "@" (:domain user))]
     ["Alias" {} (full-uri user)]

     ["Link" {"rel" "http://webfinger.net/rel/profile-page"
              "type" "text/html"
              "href" (full-uri user)}]

     ["Link" {"rel" "http://microformats.org/profile/hcard"
              "type" "text/html"
              "href" (full-uri user)}]

     ["Link" {"rel" "http://gmpg.org/xfn/11"
              "type" "text/html"
              "href" (full-uri user)}]

     ["Link" {"rel" "describedby"
              "type" "application/rdf+xml"
              "href" (str (full-uri user) ".rdf")}]

     ["Link" {"rel" "salmon"
              "href" (salmon-link user)}]

     ["Link" {"rel" "http://salmon-protocol.org/ns/salmon-replies"
              "href" (salmon-link user)}]

     ["Link" {"rel" "http://salmon-protocol.org/ns/salmon-mention"
              "href" (salmon-link user)}]

     ["Link" {"rel" "magic-public-key"
              "href" "data:application/magic-public-key,RSA"}]

     ["Link" {"rel" "http://ostatus.org/schema/1.0/subscribe"
              "template" (str "http://"
                              (:domain (config))
                              "/main/ostatussub?profile={uri}")}]

     ["Link" {"rel" "http://specs.openid.net/auth/2.0/provider"
              "href" (full-uri user)}]

     ["Link" {"rel" "http://onesocialweb.org/rel/service"
              "href" (str "xmpp:" (:username user) "@" (:domain user))}]])})
