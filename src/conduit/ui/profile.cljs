(ns conduit.ui.profile
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.comment :as comment]
   [conduit.ui.routes :as routes]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.routing :as r]
   [fulcro.client.dom :as dom]
   [conduit.ui.article-preview :as preview]
   [conduit.ui.pagination :as pagination]
   [conduit.util :as util]))

(defsc Profile [this {:user/keys [id name username image bio followed-by-me followed-by-count]}]
  {:ident         [:user/by-id :user/id]
   :initial-state (fn [params] #:user {:id :guest})
   :query         [:user/id :user/name :user/username :user/image :user/bio
                   :user/followed-by-me :user/followed-by-count]}
  (dom/div :.user-info
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/img :.user-img {:src image})
          (dom/h4 name)
          (dom/p bio)
          (let [current-user-id (-> (prim/shared this :user/whoami) :user/id)]
            (if (= id current-user-id)
              (dom/button :.btn.btn-sm.btn-outline-secondary
                "You have " followed-by-count " followers")
              (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
                {:onClick #(if (= :guest current-user-id)
                             (js/alert "You must log in first")
                             (if followed-by-me
                               (prim/transact! this `[(mutations/unfollow {:user/id ~id})])
                               (prim/transact! this `[(mutations/follow {:user/id ~id})])))}
                (dom/i :.ion-plus-round)
                (if followed-by-me "Unfollow " "Follow ")
                name "(" followed-by-count ")"))))))))

(def ui-profile (prim/factory Profile))

(defsc ListSelector [this props {:keys [current-page current-user-name]}]
  {:query []}
  (let [{:pagination/keys [list-type list-id]} current-page
        whoami                                 (prim/shared this :user/whoami)]
    (dom/div :.articles-toggle
      (dom/ul :.nav.nav-pills.outline-active
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (when (= list-type :owned-articles/by-user-id) "active")
             :onClick   #(prim/transact! this `[(load-page #:pagination{:list-type :owned-articles/by-user-id
                                                                        :list-id   ~list-id
                                                                        :size      5})])}
            (if (= (:user/id whoami) list-id)
              "My"
              (str current-user-name "'s"))
            " Articles"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            {:className (when (= list-type :liked-articles/by-user-id) "active")
             :onClick   #(prim/transact! this `[(load-page #:pagination {:list-type :liked-articles/by-user-id
                                                                         :list-id   ~list-id
                                                                         :size      5})])}
            "Favorited Articles"))))))

(def ui-list-selector (prim/factory ListSelector))

(defsc ProfileScreen [this {:keys  [screen profile-to-view user-id current-page]}]
  {:ident         (fn [] [screen user-id])
   :initial-state (fn [params] {:screen                :screen.profile/by-user-id
                                :user-id               :guest
                                :profile-to-view       (prim/get-initial-state Profile #:user{:id :guest})})
   :query         (fn [] [:screen :user-id
                          {:current-page (prim/get-query pagination/Page)}
                          {:profile-to-view (prim/get-query Profile)}])}
  (dom/div :.profile-page
    (ui-profile profile-to-view)
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (ui-list-selector (prim/computed {} {:current-page current-page
                                               :current-user-name (:user/name profile-to-view)}))
          (pagination/ui-page (prim/computed current-page {:load-page #(prim/transact! this `[(load-page ~%)])})))))))

(defmutation load-profile [{:keys [user-id]}]
  (action [{:keys [state] :as env}]
    (swap! state
      #(update-in % [:screen.profile/by-user-id user-id]
         (fn [x] (if x
                   x
                   {:screen          :screen.profile/by-user-id
                    :user-id         user-id
                    :current-page    {}
                    :profile-to-view [:user/by-id user-id]}))))
    (df/load-action env [:user/by-id user-id] Profile)
    (df/load-action env :paginated-list/articles
      pagination/Page {:params #:pagination{:list-type :owned-articles/by-user-id
                                            :list-id user-id
                                            :size 5}
                       :target [:screen.profile/by-user-id user-id :current-page]}))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:screen :profile-to-view]))

;; mutations
(defmutation load-page [{:pagination/keys [list-id] :as page}]
  (action [{:keys [state] :as env}]
    (df/load-action env :paginated-list/articles
      pagination/Page {:params page
                       :target [:screen.profile/by-user-id list-id :current-page]}))
  (remote [env]
    (df/remote-load env))
  (refresh [env]
    [:user-id :current-page]))
