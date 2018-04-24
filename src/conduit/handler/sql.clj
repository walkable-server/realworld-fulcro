(ns conduit.handler.sql
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]))

(defmethod ig/init-key ::run-query [_ _params]
  (fn [db query]
    (jdbc/with-db-connection [conn (:spec db)]
      (jdbc/query conn query))))
