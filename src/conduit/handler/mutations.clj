(ns conduit.handler.mutations
  (:require [conduit.boundary.user :as user]
            [conduit.boundary.article :as article]
            [clojure.set :refer [rename-keys]]
            [conduit.util :as util]
            [buddy.sign.jwt :as jwt]
            [duct.logger :refer [log]]
            [com.wsscode.pathom.connect :as pc :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.tempid :refer [tempid?]]))

(def remove-comment-namespace
  (util/remove-namespace "comment" [:body]))

(defmutation log-in [{:app/keys [db] :as env} {:user/keys [email password]}]
  {::pc/sym 'conduit.ui.account/log-in}
  (user/find-login db email password))

(defmutation log-out [env _]
  {::pc/sym 'conduit.ui.account/log-out}
  {})

(defmutation sign-up [{:app/keys [db] :as env} new-user]
  {::pc/sym 'conduit.ui.account/sign-up}
  (user/create-user db new-user))

(defmutation submit-article
  [{:app/keys [db] current-user :app.auth/current-user} diff]
  {}
  (if current-user
    (let [[_ article-id] (util/get-ident diff)
          article (util/get-item diff)]
      (if (tempid? article-id)
        (let [new-id (article/create-article db current-user article)]
          {:tempids {article-id new-id}})
        (article/update-article db current-user article-id article)))
    {}))

(defmutation submit-comment
  [{:app/keys [db] current-user :app.auth/current-user} {:keys [article-id diff]}]
  {}
  (if current-user
    (let [[_ comment-id] (util/get-ident diff)
          comment-item   (-> (util/get-item diff)
                             (rename-keys remove-comment-namespace))]
      (if (tempid? comment-id)
        (let [new-id (article/create-comment db current-user article-id comment-item)]
          {:tempids {comment-id new-id}})
        (article/update-comment db current-user comment-id comment-item)))
    {}))

(defmutation submit-settings
  [{:app/keys [db] current-user :app.auth/current-user} diff]
  {}
  (if current-user
    (user/update-user db current-user (util/get-item diff))
    {}))

(defmutation delete-article
  [{:app/keys [db] current-user :app.auth/current-user} {:article/keys [id]}]
  {}
  (if current-user
    (article/destroy-article db current-user id)
    {}))

(defmutation delete-comment
  [{:app/keys [db] current-user :app.auth/current-user} {:comment/keys [id]}]
  {}
  (if current-user
    (article/destroy-comment db current-user id)
    {}))

(defmutation follow
  [{:app/keys [db] current-user :app.auth/current-user} {:user/keys [id]}]
  {}
  (if (and current-user (not= current-user id))
    (user/follow db current-user id)
    {}))

(defmutation unfollow
  [{:app/keys [db] current-user :app.auth/current-user} {:user/keys [id]}]
  {}
  (if current-user
    (user/unfollow db current-user id)
    {}))

(defmutation like
  [{:app/keys [db] current-user :app.auth/current-user} {:article/keys [id]}]
  {}
  (if current-user
    (article/like db current-user id)
    {}))

(defmutation unlike
  [{:app/keys [db] current-user :app.auth/current-user} {:article/keys [id]}]
  {}
  (if current-user
    (article/unlike db current-user id)
    {}))

(defmutation add-tag
  [{:app/keys [db] current-user :app.auth/current-user} {:keys [article-id tag]}]
  {}
  (if current-user
    (article/add-tag db current-user article-id tag)
    {}))

(defmutation remove-tag
  [{:app/keys [db] current-user :app.auth/current-user} {:keys [article-id tag]}]
  {}
  (if current-user
    (article/remove-tag db current-user article-id tag)
    {}))

(def mutations
  [log-in
   log-out
   sign-up
   submit-article
   submit-comment
   submit-settings
   delete-article
   delete-comment
   follow
   unfollow
   like
   unlike
   add-tag
   remove-tag])
