(ns conduit.handler.user-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ring.mock.request :as mock]
            [conduit.handler.user :as user]))

(deftest smoke-test
  (testing "whoami page exists"
    (let [handler  (ig/init-key :conduit.handler.user/whoami {})
          response (handler (mock/request :get "/whoami"))]
      (is (= :ataraxy.response/ok (first response)) "response ok"))))
