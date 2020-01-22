(defproject conduit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.8.2"
  :dependencies [[org.clojure/clojure "1.10.1"]

                 [duct/core "0.8.0"]
                 [duct/module.logging "0.5.0"]
                 [duct/module.web "0.7.0"]
                 [duct/module.ataraxy "0.3.0"]
                 [duct/module.cljs "0.4.1" :exclusions [org.clojure/clojurescript]]
                 [duct/module.sql "0.6.0"]

                 [duct/middleware.buddy "0.1.0"]

                 ;; this version of clojurescript doesn't cause compiling error to specter
                 ;; it's used by specter itself
                 [org.clojure/clojurescript "1.10.597"]
                 [com.rpl/specter "1.1.3"]
                 [metosin/reitit "0.4.2"]
                 [kibu/pushy "0.3.8"]
                 [cheshire "5.9.0"]
                 [duct/handler.sql "0.4.0"]
                 [buddy/buddy-hashers "1.4.0"]
                 [fulcrologic/fulcro "2.8.13"]
                 [walkable "1.2.0-SNAPSHOT"]
                 [org.clojure/core.async "0.7.559"]
                 [org.postgresql/postgresql "42.2.9"]]
  :middleware [lein-duct.plugin/middleware]
  :plugins [[duct/lein-duct "0.11.2"]]
  :main ^:skip-aot conduit.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         :timeout          120000
                         :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [ ;; cljs
                                   [cider/piggieback "0.4.2"]
                                   [walkable/duct.server.figwheel "0.4.0-SNAPSHOT" :exclusions [org.clojure/clojurescript]]
                                   [org.clojure/test.check "0.10.0"]
                                   [devcards "0.2.6" :exclusions [org.clojure/clojurescript]]

                                   [fulcrologic/fulcro-inspect "2.2.5" :exclusions [fulcrologic/fulcro-css]]

                                   [integrant/repl "0.3.1"]
                                   [eftest "0.5.9"]
                                   [kerodon "0.9.1"]]}})
