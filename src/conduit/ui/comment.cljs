(ns conduit.ui.comment
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [conduit.ui.other :as other]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc CommentForm
  [this {:comment/keys [id body]}
   {:keys [article-id set-editing-comment-id current-user]}]
  {:query             [:comment/id :comment/body]
   :initial-state     (fn [_params] #:comment{:id :none})
   :ident             :comment/id
   :componentDidMount (fn [this]
                        (when (number? (:comment/id (comp/props this)))
                          (other/focus-field this "comment_field")))}
  (let [state  (comp/get-state this)]
    (dom/form :.card.comment-form
      {:onSubmit
       #(do (.preventDefault %)
            (when (and (seq (:comment/body state))
                    (not= (:comment/body state) body))
              (comp/transact! this
                [(mutations/submit-comment
                   {:article-id article-id
                    :diff {[:comment/id (if (= :none id) (tempid/tempid) id)]
                           state}})])
              (if (= :none id)
                (comp/set-state! this {:comment/body ""})
                (set-editing-comment-id :none))))}
      (dom/div :.card-block
        (dom/textarea :.form-control
          {:disabled (when-not (:user/valid? current-user) true)
           :placeholder (if (:user/valid? current-user)
                          "Write a comment..."
                          "Log in to write a comment...")
           :rows        "3"
           :ref         "comment_field"
           :value       (or (:comment/body state) body "")
           :onChange    #(comp/set-state! this {:comment/body (.. % -target -value)})}))
      (dom/div :.card-footer
        (dom/img :.comment-author-img
          {:src (:user/image current-user other/default-user-image)})
        (dom/button :.btn.btn-sm.btn-primary
          {:type "submit" :value "submit"}
          (if (number? id)
            "Update Comment"
            "Post Comment"))))))

(def ui-comment-form (comp/computed-factory CommentForm {:keyfn :comment/id}))

(defsc Comment
  [this {:comment/keys [id author body created-at can-edit] :as props}
   {:keys [delete-comment editing-comment-id set-editing-comment-id]
    :as computed-map}]
  {:ident :comment/id
   :initial-state (fn [{:comment/keys [id]}]
                    #:comment{:id id
                              :author (comp/get-initial-state other/UserTinyPreview #:user{:id :guest})})
   :query [:comment/id :comment/created-at :comment/body
           :comment/can-edit
           {:comment/author (comp/get-query other/UserTinyPreview)}]}
  (if (= editing-comment-id id)
    (ui-comment-form props computed-map)
    (dom/div :.card
      (dom/div :.card-block
        (dom/p :.card-text
          body))
      (dom/div :.card-footer
        (dom/a :.comment-author {:href (str "/profile/" (:user/id author))}
          (dom/img :.comment-author-img
            {:src (:user/image author other/default-user-image)}))
        (dom/a :.comment-author {:href (str "/profile/" (:user/id author))}
          (:user/name author))
        (dom/span :.date-posted
          (other/js-date->string created-at))
        (when can-edit
          (dom/span :.mod-options
            (dom/i :.ion-edit {:onClick #(set-editing-comment-id id) } " ")
            (dom/i :.ion-trash-a {:onClick #(delete-comment id) } " ")))))))

(def ui-comment (comp/computed-factory Comment {:keyfn :comment/id}))
