(ns conduit.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.inspect.preload]
   [conduit.app :refer [APP init!]]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc Root [this props]
  (dom/div "hello monster"))

(defn refresh []
  (app/mount! APP Root "app"))

(defn ^:export init
  []
  (refresh)
  (init!))

(init)
