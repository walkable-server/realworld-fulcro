(ns conduit.handler.fulcro
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key ::index [_ _options]
  (fn [{:keys [anti-forgery-token]}]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (format (slurp (io/resource "conduit/public/index.html")) anti-forgery-token)}))
