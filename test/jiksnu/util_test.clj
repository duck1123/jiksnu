(ns jiksnu.util-test
  (:require [clj-factory.core :refer [fseq]]
            jiksnu.modules.core.factory
            [jiksnu.test-helper :as th :refer [context]]
            [jiksnu.util :as util]
            [midje.sweet :refer :all])
  (:import org.bson.types.ObjectId))

(th/module-test ["jiksnu.modules.core"])

(context #'util/new-id
  (util/new-id) => string?)

(context #'util/get-domain-name
  (let [domain-name (fseq :domain)]
    (context "when given a http uri"
      (let [uri (str "http://" domain-name "/users/1")]
        (util/get-domain-name uri) => domain-name))

    (context "when given an acct uri"
      (let [uri (str "acct:bob@" domain-name)]
        (util/get-domain-name uri) => domain-name))

    (context "when given a urn"
      (let [uri (str "urn:X-dfrn:"
                     domain-name
                     ":1:4735de37f18b820836fbe17890b33f90781d4fe275236094751be3fc163b40b4")]
        (util/get-domain-name uri) => domain-name))))

(context #'util/make-id
  (util/make-id) => (partial instance? ObjectId))

(context #'util/path-segments
  (context "When the path ends without a slash"
    (let [url "http://example.com/status/users/1"]
      (util/path-segments url) =>
      '("/" "/status/" "/status/users/"))))

(context #'util/rel-filter
  (let [links [{:rel "alternate"}
               {:rel "contains"}]]
    (context "when the link exists"
      (util/rel-filter "alternate" links nil) => [{:rel "alternate"}])
    (context "when the link does not exist"
      (util/rel-filter "foo" links nil) => [])))

(context #'util/parse-http-link
  (let [uri "acct:jonkulp@jonkulp.dyndns-home.com"
        url (str "http://jonkulp.dyndns-home.com/micro/main/xrd?uri=" uri)
        rel "lrdd"
        content-type "application/xrd+xml"
        link-string (format "<%s>; rel=\"%s\"; type=\"%s\""
                            url rel content-type)
        link {"href" url
              "rel" rel
              "type" content-type}]
    (util/parse-http-link link-string) => (contains link)))

(context #'util/split-uri
  (util/split-uri "bob@example.com")        => ["bob" "example.com"]
  (util/split-uri "acct:bob@example.com")   => ["bob" "example.com"]
  (util/split-uri "http://example.com/bob") => nil)
