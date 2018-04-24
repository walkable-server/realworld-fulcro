(ns conduit.handler.article
  (:require [ataraxy.core :as ataraxy]
            [buddy.sign.jwt :as jwt]
            [conduit.boundary.article :as article]
            [ataraxy.response :as response]
            [integrant.core :as ig]))

(defmethod ig/init-key ::create [_ {:keys [db]}]
  (fn [{[_ article] :ataraxy/result
        id          :identity}]
    (when id
      (let [article-id (article/create-article db
                         (assoc article :author_id (:user-id id)))]
        [::response/created (str "/artiles/" article-id)]))))

(defmethod ig/init-key ::all-articles
  [_ {:keys [resolver]}]
  (fn [{id :identity}]
    [::response/ok
     (resolver (:user-id id)
       '[{(:articles/all {:order-by [:article/created-at]
                          :filters  {:article/tags     [:in :tag/tag "me"]
                                     :article/author   [:in :user/username "kope"]
                                     :article/liked-by [:in :user/username "jope"]}})
          [:article/slug :article/title :article/description :article/body
           :article/created-at :article/updated-at
           {:article/liked-by-count [:agg/count]}
           {:article/liked-by [:user/username]}
           {:article/liked-by-me? [:agg/count]}
           {:article/tags [:tag/tag]}
           {:article/comments [:comment/id :comment/created-at :comment/updated-at :comment/body]}
           {:article/author [:user/username :user/bio :user/image {:user/followed-by-me? [:agg/count]}]}]}])]))
