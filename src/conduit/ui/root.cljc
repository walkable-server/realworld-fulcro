(ns conduit.ui.root
  (:require
   [fulcro.client.mutations :as m]
    [fulcro.client.data-fetch :as df]
    #?(:cljs [fulcro.client.dom :as dom] :clj [fulcro.client.dom-server :as dom])
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.i18n :as i18n :refer [tr trf]]))

;; The main UI of your application

(defsc Root [this props]
  (dom/div nil "TODO"))
