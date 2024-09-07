(defproject net.kronkltd/jiksnu "0.3.0-SNAPSHOT"
  :description "distributed social network"
  :url "https://github.com/kronkltd/jiksnu"
  :author "Daniel E. Renfer <duck@kronkltd.net>"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src" "src-cljs"]
  :resource-paths ["resources" "target/resources" "node_modules"]
  :test-paths ["test" "test-cljs" "specs"]
  :cucumber-glue-paths ["specs"]
  :dependencies [[cider/cider-nrepl "0.50.2"]
                 [ciste "0.6.0-SNAPSHOT"
                  :exclusions [ring/ring-core
                               org.clojure/clojure
                               org.clojure/tools.reader
                               org.clojure/clojurescript
                               xerces/xercesImpl]]
                 [ciste/ciste-incubator "0.1.0-20170109.012825-2" :exclusions [ciste ciste/ciste-core]]
                 [clj-factory "0.2.2-SNAPSHOT"]
                 [clj-time "0.15.2"]
                 [clj-http "3.13.0"]
                 [compojure "1.7.1"]
                 [com.cemerick/friend "0.2.3"]
                 [com.getsentry.raven/raven "8.0.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [com.novemberain/monger "3.6.0" :exclusions [com.google.guava/guava]]
                 [com.novemberain/validateur "2.6.0"]
                 [com.taoensso/timbre "6.5.0"]
                 [crypto-random "1.2.1"]
                 [hiccup "1.0.5"]
                 [hiccups "0.3.0"]
                 ;; https://mvnrepository.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api
                 [jakarta.xml.bind/jakarta.xml.bind-api "4.0.2"]
                 [liberator "0.15.3"]
                 [manifold "0.4.3"]
                 [mvxcvi/puget "1.3.4"]
                 [net.kronkltd/clj-gravatar "0.1.0-20120321.005702-1"]
                 [net.kronkltd/octohipster "0.3.0-20151001.045924-2"
                  :exclusions [inflections]]
                 [org.clojure/clojure "1.11.4"]
                 [org.clojure/clojurescript "1.11.132"]
                 [org.clojure/core.async "1.6.681"
                  :exclusions [org.clojure/tools.reader org.clojure/core.cache]]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.clojure/tools.reader "1.5.0"]
                 [org.clojure/data.json "2.5.0"]
                 ;; https://mvnrepository.com/artifact/org.glassfish.jaxb/jaxb-runtime
                 [org.glassfish.jaxb/jaxb-runtime "4.0.5"]
                 [org.slf4j/slf4j-log4j12 "2.0.16"]
                 [ring "1.12.2"]
                 [ring/ring-defaults "0.5.0"]
                 [slingshot "0.12.2"]]
  :cljfmt {:indents {#".*fact.*" [[:inner 0]]}}
  :plugins [[codox "0.8.13" :exclusions [org.clojure/clojure]]
            [lein-ancient "0.7.0"]
            [lein-annotations "0.1.0" :exclusions [org.clojure/clojure]]
            [lein-checkall "0.1.1" :exclusions [org.clojure/tools.namespace org.clojure/clojure]]
            [lein-cljfmt "0.5.2"  :exclusions [org.clojure/clojure
                                               org.clojure/clojurescript
                                               org.clojure/tools.reader]]
            [lein-cljsbuild "1.1.4" :exclusions [org.clojure/clojure]]
            [lein-cloverage "1.0.2" :exclusions [org.clojure/clojure]]
            [lein-figwheel "0.5.8"
             :exclusions [net.java.dev.jna/jna]]
            [lein-midje "3.1.3" :exclusions [org.clojure/clojure]]
            [lein-shell "0.4.0" :exclusions [org.clojure/clojure]]
            [org.clojars.punkisdead/lein-cucumber "1.0.7"]]
  :hiera {:ignore-ns #{"jiksnu.modules.core.db" "jiksnu.mock" "jiksnu.modules.core.channels" "jiksnu.modules.core.model"
                       "jiksnu.modules.core.factory" "jiksnu.modules.core.ops" "jiksnu.namespace"
                       "jiksnu.registry" "jiksnu.session" "jiksnu.util"}}
  :aliases {"guard"            ["shell" "bundle" "exec" "guard"]
            "karma"            ["shell" "./node_modules/.bin/karma" "start"]
            "protractor"       ["shell" "./node_modules/.bin/protractor" "protractor.config.js"]
            "webdriver-start"  ["shell" "./node_modules/.bin/webdriver-manager" "start"]
            "webdriver-update" ["shell" "./node_modules/.bin/webdriver-manager" "update"]
            "wscat"            ["shell" "./node_modules/.bin/wscat" "-c" "ws://localhost/"]}
  :auto-clean false
  :jvm-opts ["-server"
             "-Dfile.encoding=UTF-8"
             "-Djava.library.path=native"
             ;; "-Dcom.sun.management.jmxremote"
             ;; "-Dcom.sun.management.jmxremote.ssl=false"
             ;; "-Dcom.sun.management.jmxremote.authenticate=false"
             ;; "-Dcom.sun.management.jmxremote.port=43210"
             ]
  :warn-on-reflection false
  :repl-options {:init-ns ciste.runner :host "0.0.0.0" :port 7888}
  :main ciste.runner
  :aot [ciste.runner]
  :cljsbuild {:builds
              {:none {:figwheel true
                      :source-paths ["src-cljs" "test-cljs"]
                      :notify-command ["notify-send"]
                      :compiler {:output-to "target/resources/public/cljs-none/jiksnu.js"
                                 :output-dir "target/resources/public/cljs-none"
                                 :optimizations :none
                                 :main "jiksnu.main"
                                 :asset-path "cljs-none"
                                 :pretty-print true}}
               :main {:source-paths ["src-cljs"]
                      :notify-command ["notify-send"]
                      :compiler {:output-to "target/resources/public/cljs/jiksnu.js"
                                 :output-dir "target/resources/public/cljs"
                                 :source-map "target/resources/public/cljs/jiksnu.js.map"
                                 ;; :main "jiksnu.app"
                                 :optimizations :simple
                                 :asset-path "cljs"
                                 ;; :verbose true
                                 :pretty-print true}}}}
  :profiles {:dev {:dependencies
                   [[midje "1.10.10" :exclusions [org.clojure/clojure]]
                    [figwheel-sidecar "0.5.20"
                     :exclusions [http-kit org.clojure/core.cache]]
                    [org.clojure/test.check "1.1.1"]
                    [org.clojure/tools.nrepl "0.2.13"]
                    [ring-mock "0.1.5"]
                    [clj-webdriver "0.7.2"]
                    [org.seleniumhq.selenium/selenium-server "3.141.59"
                     :exclusions [org.eclipse.jetty/jetty-io
                                  org.eclipse.jetty/jetty-util
                                  net.java.dev.jna/jna xerces/xercesImpl
                                  org.seleniumhq.selenium/selenium-support
                                  org.seleniumhq.selenium/selenium-api]]
                    [org.seleniumhq.selenium/selenium-api "4.24.0"]
                    [org.seleniumhq.selenium/selenium-support "4.24.0"]
                    [slamhound "1.5.5"]]}
             :e2e {:cljsbuild {:builds
                               {:protractor
                                {:source-paths ["specs-cljs"]
                                 :notify-command ["notify-send"]
                                 :compiler {:output-to "target/protractor-tests.js"
                                            ;; :output-dir "target/specs/"
                                            :optimizations :simple
                                            :target :nodejs
                                            :language-in :ecmascript5
                                            :pretty-print true}}}}}
             :production {:cljsbuild {:builds
                                      {:advanced
                                       {:source-paths ["src-cljs"]
                                        :notify-command ["notify-send"]
                                        :compiler {:output-to "target/resources/public/cljs/jiksnu.min.js"
                                                   :optimizations :advanced
                                                   :pretty-print false}}}}}
             :test {:resource-paths ["target/resources" "resources" "test-resources"]
                    :cljsbuild {:builds
                                {:karma
                                 {:source-paths ["src-cljs" "test-cljs"]
                                  :notify-command ["notify-send"]
                                  :compiler {:output-to "target/karma-cljs/karma-test.js"
                                             :output-dir "target/karma-cljs"
                                             :optimizations :none
                                             ;; Fix for $q's use of 'finally'
                                             :language-in :ecmascript5
                                             :pretty-print true}}}}}}
  :filespecs [{:type :path :path "ciste.clj"}]
  ;; :repositories [["snapshots" {:url "http://repo.jiksnu.org/repository/maven-snapshots/"
  ;;                              :username [:gpg :env/repo_username]
  ;;                              :password [:gpg :env/repo_password]}]
  ;;                ["releases" {:url "http://repo.jiksnu.org/repository/maven-releases/"
  ;;                             :username [:gpg :env/repo_username]
  ;;                             :password [:gpg :env/repo_password]}]
  ;;                ["maven-mirror" {:url "http://repo.jiksnu.org/repository/maven-central/"}]]
  )
