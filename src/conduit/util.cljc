(ns conduit.util)

(defn get-ident
  [x]
  (ffirst x))

(defn get-item
  [x]
  (second (first x)))

(comment
  (get-ident {[:user/by-id 19] #:user{:bio "some text"}})
  ;; => [:user/by-id 19]
  (get-item {[:user/by-id 19] #:user{:bio "some text"}})
  ;; => #:user{:bio "some text"}#:user{:bio "some text"}
)

(defn remove-namespace [namespace ks]
  (into {}
    (for [k ks]
      [(keyword namespace (name k)) k])))

(defn page-number [total items-per-page]
  (if (zero? total)
    1
    (+ (quot total items-per-page)
      (if (zero? (rem total items-per-page))
        0
        1))))

(defn list-ident-value
  [{:app.articles.list/keys [list-type list-id direction size]
    :or                     {list-type :app.articles/on-feed
                             list-id   :global
                             direction :forward
                             size      5}}]
  #:app.articles.list{:list-type list-type
                      :list-id   list-id
                      :direction direction
                      :size      size})

(defn list-ident
  [props]
  [:app.articles/list (list-ident-value props)])
