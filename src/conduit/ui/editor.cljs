(ns conduit.ui.editor
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [conduit.handler.mutations :as mutations]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid :refer [tempid?]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [conduit.session :as session]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc TagItem [this {:tag/keys [tag]} {:keys [on-delete]}]
  {:query [:tag/tag]}
  (dom/span :.tag-pill.tag-default
    (dom/i :.ion-close-round
      {:onClick #(on-delete tag)})
    tag))

(def ui-tag-item (comp/factory TagItem {:keyfn :tag/tag}))

(defsc ArticleEditor* [this props]
  {:query         [:article/id :article/slug :article/title :article/description :article/body
                   {:article/tags [:tag/tag]}]
   :ident         :article/id})

(defsc ArticleEditor [this {:article/keys [id slug title description body tags] :as props}]
  {:initial-state #:article{:id :none :body "" :title "" :description "" :slug ""}
   :query         [:article/id :article/slug  :article/title :article/description :article/body :article/tags 
                   fs/form-config-join]
   :ident         :article/id
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
                   :value       (or title "")
                   :onBlur      #(comp/transact! this
                                   [(fs/mark-complete! {:field :article/title})])
                   :onChange    #(m/set-string! this :article/title :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "What's this article about?",
                   :type        "text"
                   :name        "description"
                   :value       (or description "")
                   :onBlur      #(comp/transact! this
                                   [(fs/mark-complete! {:field :article/description})])
                   :onChange    #(m/set-string! this :article/description :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "Slug",
                   :type        "text"
                   :name        "slug"
                   :value       (or slug "")
                   :onBlur      #(comp/transact! this
                                   [(fs/mark-complete! {:field :article/slug})])
                   :onChange    #(m/set-string! this :article/slug :event %)}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control
                  {:rows     "8", :placeholder "Write your article (in markdown)"
                   :name     "body"
                   :value    (or body "")
                   :onBlur   #(comp/transact! this
                                [(fs/mark-complete! {:field :article/body})])
                   :onChange #(m/set-string! this :article/body :event %)}))
              (dom/fieldset :.form-group
                (let [new-tag (comp/get-state this :new-tag)]
                  (dom/input :.form-control
                    {:placeholder "Enter tags"
                     :name        "tag"
                     :type        "text"
                     :onChange    #(comp/set-state! this {:new-tag (.. % -target -value)})
                     :onKeyDown   #(let [key (.-key %)]
                                     (when (and (= key "Enter") (seq new-tag))
                                       (comp/transact! this [(mutations/add-tag {:article-id id :tag new-tag})])
                                       (comp/set-state! this {:new-tag ""})))
                     :value       (or new-tag "")}))
                (dom/div :.tag-list
                  (let [on-delete-tag #(comp/transact! this [(mutations/remove-tag {:article-id id :tag %})])]
                    (map #(ui-tag-item (comp/computed % {:on-delete on-delete-tag})) tags))))
              (dom/div :.btn.btn-lg.pull-xs-right.btn-primary
                {:onClick #(comp/transact! this [(mutations/submit-article (fs/dirty-fields props false))])}
                (if (tempid? id)
                  "Publish Article"
                  "Update Article")))))))))

(def ui-article-editor (comp/factory ArticleEditor))

(defn create-temp-article-if-not-found*
  [tempid-fn state]
  (if (tempid? (get-in state [:component/id :new :article 1]))
    state
    (let [tempid              (tempid-fn)

          new-item #:article {:id          tempid
                              :title       ""
                              :slug        ""
                              :description ""
                              :body        ""
                              :author      [:session/session :current-user]
                              :tags        []}]
      (-> (assoc-in state [:article/id tempid] new-item)
        (assoc-in [:component/id :new :article] [:article/id tempid])))))

(defmutation create-temp-article-if-not-found [_]
  (action [{:keys [state]}]
    (swap! state #(create-temp-article-if-not-found* tempid/tempid %))))

(defmutation use-current-temp-article-as-form [_]
  (action [{:keys [app state]}]
    (swap! state #(let [temp-ident (get-in % [:component/id :new :article])]
                    (fs/add-form-config* % ArticleEditor temp-ident)))
    (dr/target-ready! app [:component/id :new])))

(defmutation load-article-to-editor [{:article/keys [id]}]
  (action [{:keys [app]}]
    (df/load! app [:article/id id] ArticleEditor*)))

(defmutation use-article-as-form [{:article/keys [id]}]
  (action [{:keys [app state]}]
    (swap! state #(-> %
                    (fs/add-form-config* ArticleEditor [:article/id id])
                    (assoc-in [:component/id :edit :article]
                      [:article/id id])))
    (dr/target-ready! app [:component/id :edit])))

(defsc New [this {:keys [article] :as props}]
  {:query [{:article (comp/get-query ArticleEditor)}
           {[:session/session :current-user] (comp/get-query session/CurrentUser)}]
   :initial-state (fn [_] {:article (comp/get-initial-state ArticleEditor {})})
   :route-segment ["new"]
   :will-enter (fn [app _route-params]
                 (dr/route-deferred [:component/id :new]
                   #(comp/transact! app [(create-temp-article-if-not-found {})
                                         (use-current-temp-article-as-form {})])))
   :ident (fn [] [:component/id :new])}
  (let [{:keys [:user/valid?]} (get props [:session/session :current-user])]
    (if valid?
      (ui-article-editor article)
      "You must log in first")))

(defsc Edit [this {:keys [article] :as props}]
  {:query [{:article (comp/get-query ArticleEditor)}
           {[:session/session :current-user] (comp/get-query session/CurrentUser)}]
   :initial-state (fn [_] {:article (comp/get-initial-state ArticleEditor {})})
   :route-segment ["edit" :article/id]
   :will-enter (fn [app {:article/keys [id]}]
                 (let [id (if (string? id) (js/parseInt id) id)]
                   (dr/route-deferred [:component/id :edit]
                     #(comp/transact! app [(load-article-to-editor {:article/id id})
                                           (use-article-as-form {:article/id id})]))))
   :ident (fn [] [:component/id :edit])}
  (let [{:keys [:user/valid?]} (get props [:session/session :current-user])]
    (if valid?
      (ui-article-editor article)
      "You must log in first")))
