(ns jiksnu.modules.core.model.webfinger-test
  (:require [clj-factory.core :refer [factory fseq]]
            [ciste.model :as cm]
            [hiccup.core :as hiccup]
            [jiksnu.modules.core.model.webfinger :refer :all]
            [jiksnu.namespace :as ns]
            [jiksnu.test-helper :as th]
            [midje.sweet :refer :all]))

(defn mock-xrd-with-username
  [username]
  (cm/string->document
   (hiccup/html
    [:XRD {:xmlns ns/xrd}
     [:Link
      [:Property {:type "http://apinamespace.org/atom/username"}
       username]]])))

(defn mock-xrd-with-subject
  [subject]
  (cm/string->document
   (hiccup/html
    [:XRD {:xmlns ns/xrd}
     [:Subject subject]])))

(th/module-test ["jiksnu.modules.core"])

(facts "#'jiksnu.modules.core.model.webfinger/get-username-from-atom-property"
  (fact "when the property has an identifier"
    (let [username (fseq :username)
          user-meta (mock-xrd-with-username username)]
      (get-username-from-atom-property user-meta) => username)))

(facts "#'jiksnu.modules.core.model.webfinger/get-links"
  (fact "When it has links"
    (let [xrd (cm/string->document
               (hiccup/html
                [:XRD
                 [:Link {:ref ns/updates-from
                         :xmlns ns/xrd
                         :type "application/atom+xml"
                         :href ""}]]))]
      (get-links xrd) => (contains [(contains {:type "application/atom+xml"})]))))

(facts "#'jiksnu.modules.core.model.webfinger/get-identifiers"
  (let [subject "acct:foo@bar.baz"
        xrd (mock-xrd-with-subject subject)]
    (get-identifiers xrd) => (contains subject)))

(facts "#'jiksnu.modules.core.model.webfinger/get-username-from-identifiers"
  (let [subject "acct:foo@bar.baz"
        xrd (mock-xrd-with-subject subject)]
    (get-username-from-identifiers xrd) => "foo"))

(facts "#'jiksnu.modules.core.model.webfinger/get-username-from-xrd"
  (let [username (fseq :username)
        domain (fseq :domain)
        subject (format "acct:%s@%s" username domain)
        user-meta (mock-xrd-with-subject subject)]
    (fact "when the usermeta has an identifier"
      (get-username-from-xrd user-meta) => username
      (provided
       (get-username-from-identifiers user-meta) => username
       (get-username-from-atom-property user-meta) => nil :times 0))
    (fact "when the usermeta does not have an identifier"
      (fact "and the atom link has an identifier"
        (let [user-meta (cm/string->document
                         (hiccup/html
                          [:XRD
                           [:Link {:ref ns/updates-from
                                   :type "application/atom+xml"
                                   :href ""}]]))]
          (get-username-from-xrd user-meta) => .username.
          (provided
           (get-username-from-identifiers user-meta) => nil
           (get-username-from-atom-property user-meta) => .username.)))
      (fact "and the atom link does not have an identifier"
        (let [user-meta (cm/string->document
                         (hiccup/html
                          [:XRD
                           [:Link {:ref ns/updates-from
                                   :type "application/atom+xml"
                                   :href ""}]]))]
          (get-username-from-xrd user-meta) => nil
          (provided
           (get-username-from-identifiers user-meta) => nil
           (get-username-from-atom-property user-meta) => nil))))))
