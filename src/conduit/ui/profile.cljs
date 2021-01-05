(ns conduit.ui.profile
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.session :as session]
   [conduit.ui.other :as other :refer [display-name]]
   [conduit.ui.article-preview :as preview]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.dom :as dom]))

(defmutation set-profile-tab [{user-id :user/id tab :ui/current-tab}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:user/id user-id :ui/current-tab] tab)))

(declare Profile)

(defmutation load-profile [{:user/keys [id]}]
  (action [{:keys [app]}]
    (df/load! app [:user/id id] Profile
      {:without #{:user/likes}})
    (dr/target-ready! app [:user/id id])))

(defmutation load-profile-articles [{:user/keys [id]}]
  (action [{:keys [app]}]
    (df/load! app [:user/id id] Profile
      {:focus #{:user/articles}})))

(defmutation load-profile-likes [{:user/keys [id]}]
  (action [{:keys [app]}]
    (df/load! app [:user/id id] Profile
      {:focus #{:user/likes}})))

(defn ui-list-selector 
  [this {:user/keys [id]
         :ui/keys [current-user current-tab]
         :as props}]
  (dom/div :.articles-toggle
    (dom/ul :.nav.nav-pills.outline-active
      (dom/li :.nav-item
        (dom/div :.nav-link
          {:className (when (= current-tab :articles)
                        "active")
           :onClick #(comp/transact! this [(load-profile-articles {:user/id id})
                                           (set-profile-tab {:user/id id :ui/current-tab :articles})])}
          (if (= id (:user/id current-user))
            "My"
            (str (display-name props) "'s"))
          " articles"))
      (dom/li :.nav-item
        (dom/div :.nav-link
          {:className (when (not= current-tab :articles)
                        "active")
           :onClick #(comp/transact! this [(load-profile-likes {:user/id id})
                                           (set-profile-tab {:user/id id :ui/current-tab :likes})])}
          "Favorite articles")))))

(defsc Profile
  [this {:user/keys [id name image bio followed-by-me followed-by-count
                     likes articles]
         :ui/keys [current-user current-tab]
         :as props}]
  {:ident :user/id
   :initial-state (fn [_]
                    #:user{:id :none
                           :user/likes (comp/get-initial-state preview/ArticlePreview {})
                           :user/articles (comp/get-initial-state preview/ArticlePreview {})
                           :ui/current-tab :articles
                           :ui/current-user (comp/get-initial-state session/CurrentUser {})})
   :query [:user/id :user/name :user/username :user/image :user/bio
           :user/followed-by-me :user/followed-by-count
           {:user/likes (comp/get-query preview/ArticlePreview)}
           {:user/articles (comp/get-query preview/ArticlePreview)}
           :ui/current-tab
           {:ui/current-user (comp/get-query session/CurrentUser)}]
   :route-segment ["profile" :user/id]
   :will-enter (fn [app {:user/keys [id]}] 
                 (let [id (if (string? id) (js/parseInt id) id)]
                   (comp/transact! app [(mutations/ensure-ident {:ident [:user/id id]
                                                                 :state {:ui/current-tab :articles}})])
                   (dr/route-deferred [:user/id id]
                     #(comp/transact! app [(load-profile {:user/id id})]))))}
  (dom/div :.profile-page
    (dom/div :.user-info
      (dom/div :.container
        (dom/div :.row
          (dom/div :.col-xs-12.col-md-10.offset-md-1
            (dom/img :.user-img {:src (or image other/default-user-image)})
            (dom/h4 name)
            (dom/p bio)
            (if (= id (:user/id current-user))
              (dom/button :.btn.btn-sm.btn-outline-secondary
                "You have " followed-by-count " followers")
              (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
                {:onClick #(if (= :none (:user/id current-user))
                             (js/alert "You must log in first")
                             (if followed-by-me
                               (comp/transact! this [(mutations/unfollow {:user/id id})])
                               (comp/transact! this [(mutations/follow {:user/id id})])))}
                (dom/i :.ion-plus-round)
                (if followed-by-me "Unfollow" "Follow")
                name " (" followed-by-count ")"))))))
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (ui-list-selector this props)
          (preview/ui-article-list this
            {:ui/articles
             (if (= current-tab :articles)
               articles
               likes)
             :ui/empty-message "no articles"}))))))
