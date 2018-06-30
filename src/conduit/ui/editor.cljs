(ns conduit.ui.editor
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.ui.form-state :as fs]
   [conduit.handler.mutations :as mutations]
   [fulcro.tempid :refer [tempid?]]
   [conduit.ui.other :as other]
   [conduit.ui.account :as account]
   [conduit.ui.article-preview :as preview]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.dom :as dom]))

(defsc TagItem [this {:tag/keys [tag]} {:keys [on-delete]}]
  {:query [:tag/tag]}
  (dom/span :.tag-pill.tag-default
    (dom/i :.ion-close-round
      {:onClick #(on-delete tag)})
    tag))

(def ui-tag-item (prim/factory TagItem {:keyfn :tag/tag}))

(defsc ArticleEditor* [this {:article/keys [id slug title description body tags] :as props}]
  {:initial-state (fn [{:article/keys [id]}] #:article{:id :none :body "" :title "" :description "" :slug ""})
   :query         [:article/id :article/slug  :article/title :article/description :article/body
                   {:article/tags [:tag/tag]}
                   fs/form-config-join]
   :ident         [:article/by-id :article/id]})

(defsc ArticleEditor [this {:article/keys [id slug title description body tags] :as props}]
  {:initial-state (fn [{:article/keys [id]}] #:article{:id :none :body "" :title "" :description "" :slug ""})
   :query         [:article/id :article/slug  :article/title :article/description :article/body
                   :article/tags
                   fs/form-config-join]
   :ident         [:article/by-id :article/id]
   :form-fields   #{:article/slug  :article/title
                    :article/description :article/body :article/tags}}
  (dom/div :.editor-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-10.offset-md-1.col-xs-12
          (dom/div
            (dom/fieldset
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Article Title",
                   :type        "text"
                   :name        "title"
                   :value       title
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :article/title})])
                   :onChange    #(m/set-string! this :article/title :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "What's this article about?",
                   :type        "text"
                   :name        "description"
                   :value       description
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :article/description})])
                   :onChange    #(m/set-string! this :article/description :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "Slug",
                   :type        "text"
                   :name        "slug"
                   :value       slug
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :article/slug})])
                   :onChange    #(m/set-string! this :article/slug :event %)}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control
                  {:rows     "8", :placeholder "Write your article (in markdown)"
                   :name     "body"
                   :value    body
                   :onBlur   #(prim/transact! this
                                `[(fs/mark-complete! {:field :article/body})])
                   :onChange #(m/set-string! this :article/body :event %)}))
              (dom/fieldset :.form-group
                (let [new-tag (prim/get-state this :new-tag)]
                  (dom/input :.form-control
                    {:placeholder "Enter tags"
                     :name        "tag"
                     :type        "text"
                     :onChange    #(prim/set-state! this {:new-tag (.. % -target -value)})
                     :onKeyDown   #(let [key (.-key %)]
                                     (when (and (= key "Enter") (seq new-tag))
                                       (prim/transact! this `[(mutations/add-tag {:article-id ~id :tag ~new-tag})])
                                       (prim/set-state! this {:new-tag ""})))
                     :value       (or new-tag "")}))
                (dom/div :.tag-list
                  (let [on-delete-tag #(prim/transact! this `[(mutations/remove-tag {:article-id ~id :tag ~%})])]
                    (map #(ui-tag-item (prim/computed % {:on-delete on-delete-tag})) tags))))
              (dom/div :.btn.btn-lg.pull-xs-right.btn-primary
                {:onClick #(prim/transact! this `[(mutations/submit-article ~(fs/dirty-fields props false))])}
                (if (tempid? id)
                  "Publish Article"
                  "Update Article")))))))))

(def ui-article-editor (prim/factory ArticleEditor))

(defsc EditorScreen [this {:keys [screen article-to-edit article-id]}]
  {:ident         (fn [] [screen article-id])
   :initial-state (fn [params] {:screen          :screen/editor
                                :article-id      :current-temp-article
                                :article-to-edit (prim/get-initial-state ArticleEditor #:article{:id :none})})
   :query         (fn [] [:screen :article-id
                          {:article-to-edit (prim/get-query ArticleEditor)}])}
  (ui-article-editor article-to-edit))

(defn create-temp-article-if-not-found
  [tempid-fn state]
  (if (tempid? (get-in state [:screen/editor :current-temp-article :article-to-edit 1]))
    state
    (let [tempid              (tempid-fn)
          current-user        (:user/whoami state)
          [_ current-user-id] current-user

          new-item #:article {:id          tempid
                              :title       ""
                              :slug        ""
                              :description ""
                              :body        ""
                              :author      current-user
                              :tags        []}]
      (-> (assoc-in state [:article/by-id tempid] new-item)
        (update-in [:user/by-id current-user-id :user/articles]
          (fnil conj []) [:article/by-id tempid])
        (assoc-in [:screen/editor :current-temp-article]
          {:screen          :screen/editor
           :article-id      :current-temp-article
           :article-to-edit [:article/by-id tempid]})))))

(defmutation create-temp-article-if-not-found [_]
  (action [{:keys [state]}]
    (swap! state #(create-temp-article-if-not-found prim/tempid %))))

(defmutation use-current-temp-article-as-form [_]
  (action [{:keys [state]}]
    (swap! state #(let [temp-ident (get-in % [:screen/editor :current-temp-article :article-to-edit])]
                    (fs/add-form-config* % ArticleEditor temp-ident)))))

(defmutation load-article-to-editor [{:keys [article-id]}]
  (action [{:keys [state] :as env}]
    (df/load-action env [:article/by-id article-id] ArticleEditor*))
  (remote [env]
    (df/remote-load env))
  (refresh [env] [:screen]))

(defmutation use-article-as-form [{:keys [article-id]}]
  (action [{:keys [state]}]
    (swap! state #(-> %
                    (fs/add-form-config* ArticleEditor [:article/by-id article-id])
                    (assoc-in [:screen/editor article-id]
                      {:screen          :screen/editor
                       :article-id      article-id
                       :article-to-edit [:article/by-id article-id]}))))
  (refresh [env] [:screen]))
