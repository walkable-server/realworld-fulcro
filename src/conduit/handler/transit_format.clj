(ns conduit.handler.transit-format
  (:require [muuntaja.format.transit :as mt]
            [integrant.core :as ig]
            [fulcro.tempid]
            [fulcro.transit])
  (:import  [fulcro.tempid TempId]
            [com.cognitect.transit ReadHandler]
            [fulcro.transit TempIdHandler]))

(defmethod ig/init-key ::transit-json-format [_ _params]
  {:decoder [(fn [opts] (mt/make-transit-decoder :json
                          (assoc-in opts
                            [:handlers "fulcro/tempid"]
                            (reify
                              ReadHandler
                              (fromRep [_ id] (TempId. id))))))]
   :encoder [(fn [opts] (mt/make-transit-encoder :json
                          (assoc-in opts [:handlers TempId] (TempIdHandler.))))]})
