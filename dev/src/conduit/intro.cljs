(ns conduit.intro
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.routing :as r]
            [conduit.handler.mutations :as mutations]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.network :as net]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.cards :refer [defcard-fulcro]]
            [conduit.ui.components :as comp]
            [fulcro.client.dom :as dom]))

(defsc OwnedArticles [this {screen :screen user-id :user-id}]
  {:initial-state (fn [params]
                    {:screen  :screen.profile/owned-articles
                     :user-id 3})
   :ident (fn [] [screen user-id])
   :query [:screen :user-id]}
  (dom/div {} (str "Owned articles for user #" user-id)))

(defsc LikedArticles [this {screen :screen user-id :user-id}]
  {:initial-state (fn [params]
                    {:screen  :screen.profile/liked-articles
                     :user-id 3})
   :ident (fn [] [screen user-id])
   :query [:screen :user-id]}
  (dom/div {} (str "Liked articles by user #" user-id)))

(r/defrouter ProfileRouter :router/profile
  [:screen :user-id]
  :screen.profile/owned-articles OwnedArticles
  :screen.profile/liked-articles LikedArticles)

(def ui-profile-router (prim/factory ProfileRouter))

(defsc ProfileMain [this {router :router/profile}]
  {:initial-state (fn [params] {:screen         :screen/profile
                                :screen-id      :top
                                :router/profile (prim/get-initial-state ProfileRouter {})})
   :query         [:screen :screen-id {:router/profile (prim/get-query ProfileRouter)}]}
  (dom/div {}
    "Profile main"
    (ui-profile-router router)))

;; (dom/div {:onClick #(prim/transact! this `[(comp/use-article-as-form #:article{:id 2})])} "Edit")

(r/defrouter TopRouter :router/top
  ;;(fn [this props] [(:screen props) :top])
  [:screen :screen-id]
  :screen/home     comp/Home
  :screen/settings comp/SettingScreen
  :screen/editor   comp/EditorScreen
  :screen/sign-up  comp/Home
  :screen/profile  ProfileMain)

(def ui-top (prim/factory TopRouter))

(def routing-tree
  (r/routing-tree
    (r/make-route :screen/home
      [(r/router-instruction :router/top [:screen/home :top])])

    (r/make-route :screen/editor
      [(r/router-instruction :router/top [:screen/editor :param/screen-id])])

    (r/make-route :screen/settings
      [(r/router-instruction :router/top [:screen/settings :top])])
    (r/make-route :screen/sign-up
      [(r/router-instruction :router/top [:screen/sign-up :top])])

    (r/make-route :screen.feed/global
      [(r/router-instruction :router/top [:screen/home :top])
       (r/router-instruction :router/feeds [:screen.feed/global :top])])
    (r/make-route :screen.feed/personal
      [(r/router-instruction :router/top [:screen/home :top])
       (r/router-instruction :router/feeds [:screen.feed/personal :top])])

    (r/make-route :screen.profile/owned-articles
      [(r/router-instruction :router/top [:screen/profile :param/user-id])
       (r/router-instruction :router/profile [:screen.profile/owned-articles :top])])
    (r/make-route :screen.profile/liked-articles
      [(r/router-instruction :router/top [:screen/profile :param/user-id])
       (r/router-instruction :router/profile [:screen.profile/liked-articles :top])])))

(defn go-to-home [this]
  (prim/transact! this `[(r/route-to {:handler :screen/home})]))

(defsc Root [this {router :router/top :as props}]
  {:initial-state (fn [params] (merge routing-tree
                                 {:root/settings-form {:settings [:user/whoami '_]}
                                  :user/whoami {:user/name "Guest" :user/email "non@exist.com"}}
                                 {:screen.profile/owned-articles {2 {:screen :screen.profile/owned-articles :user-id 2}}
                                  :screen.profile/liked-articles {2 {:screen :screen.profile/liked-articles :user-id 2}}}
                                 {:router/top (prim/get-initial-state TopRouter {})}))
   :query [{:router/top (prim/get-query TopRouter {})}
           {:user/whoami (prim/get-query comp/NavBar)}]}
  (let [current-user (get props :user/whoami)]
    (dom/div {}
      (comp/ui-nav-bar current-user)
      (ui-top router)
      (comp/ui-footer))))

(def token-store (atom "No token"))

(defn wrap-remember-token [res]
  (when-let [new-token (or (-> (:body res) (get :user/whoami) :token))]
    ;;(println (str "found token: " new-token))
    (reset! token-store (str "Token " new-token)))
  res)

(defn wrap-with-token [req]
  (assoc-in req [:headers "Authorization"] @token-store))

(defcard-fulcro yolo
  Root
  {} ; initial state. Leave empty to use :initial-state from root component
  {:inspect-data true
   :fulcro       {:started-callback
                  (fn [app]
                    (df/load app :articles/all comp/ArticlePreview)
                    (df/load app :articles/feed comp/ArticlePreview)
                    (df/load app :tags/all comp/Tag))
                  :networking {:remote (net/fulcro-http-remote
                                         {:url "/api"
                                          :response-middleware (net/wrap-fulcro-response wrap-remember-token)
                                          :request-middleware  (net/wrap-fulcro-request wrap-with-token)})}}})
(dc/start-devcard-ui!)
