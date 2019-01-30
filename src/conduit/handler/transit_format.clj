(ns conduit.handler.transit-format
  (:require [muuntaja.format.transit :as mt]
            [integrant.core :as ig]
            [fulcro.transit :refer [->TempIdHandler]])
  (:import  [fulcro.tempid TempId]
            [com.cognitect.transit ReadHandler]))

(def write-handlers {TempId (->TempIdHandler)})

(def read-handlers
  {"fulcro/tempid" (reify
                     ReadHandler
                     (fromRep [_ id] (TempId. id)))})

(defmethod ig/init-key ::transit-json-format [_ _params]
  {:encoder-opts {:handlers write-handlers}
   :decoder-opts {:handlers read-handlers}})
