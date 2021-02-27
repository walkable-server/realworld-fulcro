(ns conduit.ui.other
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]))

(def default-user-image
  "https://static.productionready.io/images/smiley-cyrus.jpg")

(defsc UserTinyPreview [this props]
  {:query [:user/id :user/username :user/name :user/image]
   :initial-state {:user/id :none}
   :ident :user/id})

(defsc UserPreview [this props]
  {:query [:user/id :user/image :user/username :user/name :user/followed-by-me :user/followed-by-count]
   :initial-state {:user/id :none}
   :ident :user/id})

(defn display-name [user]
  (or (:user/name user)
    (:user/username user)
    (str "user-" (:user/id user))))

(defn focus-field [component ref-name]
  (let [input-field        (dom/node component ref-name)
        input-field-length (.. input-field -value -length)]
    (.focus input-field)
    (.setSelectionRange input-field input-field-length input-field-length)))

(defn js-date->string [date]
  (when (instance? js/Date date)
    (.toDateString date)))
