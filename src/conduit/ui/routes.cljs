(ns conduit.ui.routes
  (:require
   [fulcro.client.routing :as r]
   [fulcro.client.primitives :as prim :refer [defsc]]))

;; conduit.ui.account

(defn go-to-log-in [component]
  (prim/transact! component
    `[(r/route-to {:handler :screen/log-in})
      :screen]))

(defn go-to-sign-up [component]
  (prim/transact! component
    `[(conduit.ui.account/load-sign-up-form)
      (r/route-to {:handler :screen/sign-up})]))

(defn go-to-settings [component {:user/keys [id]}]
  (prim/transact! component
    `[(conduit.ui.account/use-settings-as-form {:user/id ~id})
      (r/route-to {:handler :screen/settings})]))

(defn log-out [component]
  (prim/transact! component `[(conduit.ui.account/log-out)]))

;; conduit.ui.article

(defn go-to-article [component {:article/keys [id] :as article}]
  (prim/transact! component
    `[(conduit.ui.article/load-article-to-screen ~article)
      (r/route-to {:handler      :screen/article
                   :route-params {:article-id ~id}})
      :article-to-view]))

;; conduit.ui.profile

(defn go-to-profile [component {:user/keys [id] :as profile}]
  (prim/transact! component
    `[(conduit.ui.profile/load-profile-to-screen ~profile)
      (r/route-to {:handler      :screen.owned-articles/by-user-id
                   :route-params {:user-id ~id}})]))

(defn go-to-owned-article [component {:user/keys [id] :as profile}]
  (prim/transact! component
    `[(conduit.ui.profile/load-owned-articles-to-screen ~profile)
      (r/route-to {:handler      :screen.owned-articles/by-user-id
                   :route-params {:user-id ~id}})]))

(defn go-to-liked-article [component {:user/keys [id] :as profile}]
  (prim/transact! component
    `[(conduit.ui.profile/load-liked-articles-to-screen ~profile)
      (r/route-to {:handler      :screen.liked-articles/by-user-id
                   :route-params {:user-id ~id}})]))

;; conduit.ui.home
(defn go-to-personal-feed [component]
  (prim/transact! component `[(conduit.ui.home/load-personal-feed)
                              (r/route-to {:handler :screen.feed/personal})]))

(defn go-to-global-feed [component]
  (prim/transact! component `[(conduit.ui.home/load-global-feed)
                              (r/route-to {:handler :screen.feed/global})]))

;; conduit.ui.editor

(defn go-to-new-article [component]
  (prim/transact! component
    `[(conduit.ui.editor/create-temp-article-if-not-found)
      (conduit.ui.editor/use-current-temp-article-as-form)
      (r/route-to {:handler      :screen/editor
                   :route-params {:article-id :current-temp-article}})
      :article-to-edit]))

(defn edit-article [component {:article/keys [id] :as article}]
  (prim/transact! component
    `[(conduit.ui.editor/load-article-to-editor ~article)
      (conduit.ui.editor/use-article-as-form ~article)
      (r/route-to {:handler      :screen/editor
                   :route-params {:article-id ~id}})
      :article-to-edit]))
