(ns conduit.ui.comment
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.dom :as dom]
   [conduit.ui.routes :as routes]))

(defsc CommentForm [this {:comment/keys [id body] :as props} {:keys [article-id set-editing-comment-id]}]
  {:query             [:comment/id :comment/body]
   :initial-state     (fn [params] #:comment{:id   :none
                                             :body ""})
   :ident             [:comment/by-id :comment/id]
   :componentDidMount #(when (number? (:comment/id (prim/props this)))
                         (other/focus-field this "comment_field"))}
  (let [state  (prim/get-state this)
        whoami (prim/shared this :user/whoami)]
    (dom/form :.card.comment-form
      (dom/div :.card-block
        (dom/textarea :.form-control
          {:placeholder "Write a comment..."
           :rows        "3"
           :ref         "comment_field"
           :value       (or (:comment/body state) body "")
           :onChange    #(prim/set-state! this {:comment/body (.. % -target -value)})}))
      (dom/div :.card-footer
        (dom/img :.comment-author-img
          {:src (:user/image whoami)})
        (dom/button :.btn.btn-sm
          {:className "btn-primary"
           :onClick
           #(if (= :guest (:user/id whoami))
              (js/alert "You must log in first")
              (when (and (seq (:comment/body state))
                      (not= (:comment/body state) body))
                (prim/transact! this
                  `[(mutations/submit-comment
                      {:article-id ~article-id
                       :diff       {[:comment/by-id ~(if (= :none id) (prim/tempid) id)]
                                    ~state}})])
                (if (= :none id)
                  (prim/set-state! this {})
                  (set-editing-comment-id :none))))}
          (if (number? id)
            "Update Comment"
            "Post Comment"))))))

(def ui-comment-form (prim/factory CommentForm {:keyfn :comment/id}))

(defsc Comment [this {:comment/keys [id author body created-at] :as props}
                {:keys [delete-comment editing-comment-id set-editing-comment-id] :as computed-map}]
  {:ident         [:comment/by-id :comment/id]
   :initial-state (fn [{:comment/keys [id]}]
                    #:comment{:id     id
                              :body   ""
                              :author (prim/get-initial-state other/UserTinyPreview #:user{:id :guest})})
   :query         [:comment/id :comment/created-at :comment/body
                   {:comment/author (prim/get-query other/UserTinyPreview)}]}
  (if (= editing-comment-id id)
    (ui-comment-form (prim/computed props computed-map))
    (dom/div :.card
      (dom/div :.card-block
        (dom/p :.card-text
          body))
      (dom/div :.card-footer
        (dom/div :.comment-author {:onClick #(routes/go-to-profile this author)}
          (dom/img :.comment-author-img
            {:src (:user/image author)}))
        (dom/div :.comment-author {:onClick #(routes/go-to-profile this author)}
          (:user/name author))
        (dom/span :.date-posted
          (other/js-date->string created-at))
        (let [whoami (prim/shared this :user/whoami)]
          (when (= (:user/id whoami) (:user/id author))
            (dom/span :.mod-options
              (dom/i :.ion-edit {:onClick #(set-editing-comment-id id) } " ")
              (dom/i :.ion-trash-a {:onClick #(delete-comment id) } " "))))))))

(def ui-comment (prim/factory Comment {:keyfn :comment/id}))
