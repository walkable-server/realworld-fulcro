(defproject conduit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.8.2"
  :dependencies [[org.clojure/clojure "1.10.1"]

                 [duct/core "0.8.0"]
                 [duct/module.logging "0.5.0"]
                 [duct/module.web "0.7.1"]
                 [duct/module.ataraxy "0.3.0"]
                 [duct/module.cljs "0.4.1" :exclusions [org.clojure/clojurescript]]
                 [duct/module.sql "0.6.1"]

                 [duct/middleware.buddy "0.2.0"]

                 ;; this version of clojurescript doesn't cause compiling error to specter
                 ;; it's used by specter itself
                 [org.clojure/clojurescript "1.10.773"]
                 [com.rpl/specter "1.1.3"]
                 [metosin/reitit "0.5.10"]
                 [kibu/pushy "0.3.8"]
                 [cheshire "5.10.0"]
                 [duct/handler.sql "0.4.0"]
                 [buddy/buddy-hashers "1.6.0"]
                 [com.fulcrologic/fulcro "3.4.12" :exclusions [com.cognitect/transit-clj]]
                 [walkable/transit-clj "1.0.343"]
                 [com.fulcrologic/fulcro-garden-css "3.0.8"]
                 [walkable "1.3.0-alpha0" :exclusions [com.wsscode/pathom]]
                 [com.wsscode/pathom "2.3.0-alpha10"]
                 [com.fulcrologic/guardrails "0.0.12"]
                 [org.clojure/core.async "1.3.610"]
                 [org.postgresql/postgresql "42.2.18"]]
  :middleware [lein-duct.plugin/middleware]
  :plugins [[duct/lein-duct "0.11.2"]]
  :main ^:skip-aot conduit.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         ;; :timeout          120000
                         ;; :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                         }}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [ ;; cljs
                                   [cider/piggieback "0.5.2"]
                                   [duct/server.figwheel "0.3.1" :exclusions [cider.piggieback figwheel-sidecar]]
                                   [figwheel-sidecar "0.5.20" :exclusions [org.clojure/clojurescript]]
                                   [org.clojure/test.check "1.1.0"]
                                   [devcards "0.2.7" :exclusions [org.clojure/clojurescript]]
                                   [integrant/repl "0.3.2"]
                                   [eftest "0.5.9"]
                                   [kerodon "0.9.1"]]}})
