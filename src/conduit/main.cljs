(ns conduit.main
  (:require [conduit.ui.root :as root]
            [conduit.client :as client]
            [fulcro.client :as fc]))

(reset! client/app (fc/mount @client/app root/Root "app"))
