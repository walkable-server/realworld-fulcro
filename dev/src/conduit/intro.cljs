(ns conduit.intro
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.cards :refer [defcard-fulcro]]
            [conduit.ui.root :as root]
            [conduit.client :as client]))

(defcard-fulcro yolo
  root/Root
  {} ; initial state. Leave empty to use :initial-state from root component
  {:inspect-data true
   :fulcro       {:reconciler-options {:shared-fn #(select-keys % [:user/whoami])}
                  :started-callback root/started-callback
                  :networking {:remote client/remote}}})

(dc/start-devcard-ui!)
