(ns conduit.ui.home
  (:require
   [conduit.session :as session :refer [ui-current-user]]
   [com.fulcrologic.fulcro.dom :as dom]))

;; FIXME
(defn match-route? [current-route route])

(defn ui-nav-bar [{:keys [logged-in? current-route current-user]}]
  (dom/nav :.navbar.navbar-light
    (dom/div :.container
      (dom/div :.navbar-brand
        "conduit")
      (dom/ul :.nav.navbar-nav.pull-xs-right
        (dom/li :.nav-item
          (dom/a :.nav-link
            {:className (when-not (match-route? current-route #{:screen/editor :screen/log-in :screen/sign-up})
                          "active")
             :href      "/home"}
            "Home"))
        (when logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/new) "active")
               :href      "/new"}
              (dom/i :.ion-compose)
              "New Post")))
        (when logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/settings) "active")
               :href "/settings"}
              (dom/i :.ion-gear-a)
              "Settings")))
        (when-not logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/log-in) "active")
               :href      "/login"}
              "Login")))

        (when-not logged-in?
          (dom/li :.nav-item
            (dom/a :.nav-link
              {:className (when (match-route? current-route :screen/sign-up) "active")
               :href      "/sign-up"}
              "Sign up")))

        (ui-current-user current-user)))))

(defn ui-footer [{}]
  (dom/footer
    (dom/div :.container
      (dom/div :.logo-font "conduit")
      (dom/span :.attribution
        "An interactive learning project from "
        (dom/a {:href "https://thinkster.io"} "Thinkster")
        ". Code &amp; design licensed under MIT."))))
