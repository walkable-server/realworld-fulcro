(ns conduit.ui.routes
  (:require
   [fulcro.client.routing :as r]
   [reitit.core :as rr]
   [reitit.coercion.spec]
   [reitit.coercion :as coercion]
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.util :as util]))

(def router
  (rr/router
    [["/not-found" :screen/not-found]
     ["/settings" :screen/settings]
     ["/sign-up" :screen/sign-up]
     ["/log-in" :screen/log-in]
     ["/new" :screen/new]
     ["" {:coercion reitit.coercion.spec/coercion}
      ["/edit" {:name :screen/editor}
       ["/:article-id" {:parameters {:path {:article-id int?}}}]]
      ["/article" {:name :screen/article}
       ["/:slug/:article-id" {:parameters {:path {:slug       string?
                                                  :article-id int?}}}]]
      ["/feed/:feed-id" {:name       :screen/feed
                         :parameters {:path {:feed-id keyword?}}}]
      ["/tag/:tag" {:name       :screen/tag
                    :parameters {:path {:tag string?}}}]
      ["/profile/:user-id" {:name       :screen.profile/by-user-id
                            :parameters {:path {:user-id int?}}}]]]
    {:compile coercion/compile-request-coercers}))

(defn from-path [uri]
  (when-let [m (rr/match-by-path router uri)]
    {:handler      (-> m :data :name)
     :route-params (:path (coercion/coerce! m))}))

(defn to-path [{:keys [handler route-params]}]
  (:path (rr/match-by-name router handler route-params)))

(defn profile-url [user]
  (to-path {:handler      :screen.profile/by-user-id
            :route-params {:user-id (:user/id user)}}))

(defn feed-url [feed-id]
  (to-path {:handler      :screen/feed
            :route-params {:feed-id feed-id}}))

(defn pre-route-transaction
  [{:keys [handler route-params] :as routing-data}]
  (case handler
    :screen/sign-up
    `[(conduit.ui.account/load-sign-up-form)]
    :screen/settings
    `[(conduit.ui.account/use-settings-as-form)]
    :screen/article
    `[(conduit.ui.article/load-article-to-screen ~route-params)]
    :screen.profile/by-user-id
    `[(conduit.ui.profile/load-profile ~route-params)]
    :screen/feed
    `[(conduit.ui.home/load-feed #:pagination{:list-type :articles/by-feed
                                              :list-id   ~(:feed-id route-params)
                                              :size      5})]
    :screen/tag
    `[(conduit.ui.home/load-tag #:pagination{:list-type :articles/by-tag
                                             :list-id   ~(:tag route-params)
                                             :size      5})]
    :screen/editor
    `[(conduit.ui.editor/load-article-to-editor ~route-params)
      (conduit.ui.editor/use-article-as-form ~route-params)]
    :screen/new
    `[(conduit.ui.editor/create-temp-article-if-not-found)
      (conduit.ui.editor/use-current-temp-article-as-form)]
    ;; default
    []))

(defn nav-to! [component routing-data]
  (prim/transact! component `[~@(pre-route-transaction routing-data)
                              (r/route-to ~routing-data)]))

(defn log-out [component]
  (prim/transact! component `[(conduit.ui.account/log-out)]))
