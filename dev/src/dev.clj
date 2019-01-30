(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [duct.repl.figwheel :refer [cljs-repl]]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]))

(duct/load-hierarchy)

(defmethod ig/init-key :fulcro.module/cljs-build-options
  [_ {build-id :build-id :or {build-id 0}}]
  (fn [config]
    (update-in config
      [:duct.server/figwheel :builds build-id :build-options]
      duct/merge-configs
      (:fulcro/cljs-build-options config))))

(defmethod ig/init-key :fulcro/cljs-build-options [_ _]
  identity)

(defn read-config []
  (duct/read-config (io/resource "conduit/config.edn")))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
