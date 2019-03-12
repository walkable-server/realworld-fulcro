(ns conduit.ui.return)

(defn result [result]
  #:submission{:status :ok
               :result result})

(defn errors [errors]
  #:submission{:status :failed
               :errors errors})

(defn status [result]
  (:submission/status result))

(defn unbox-result [return]
  (get return :submission/result))

(defn update-result [return f]
  (update-in return [:submission/result] f))

(comment
  (= (update-result (result 2) inc) (result 3)))

(defn append-errors [return errors]
  (reduce-kv (fn [acc k v]
               (update-in acc [:submission/errors k]
                 #(into [] (concat % v))))
    return errors))

(defn ast->result-query [ast]
  (->> (:children ast)
    (some #(when (= (:dispatch-key %) :submission/result) %))
    :query))

(comment
  (=
    (errors {:a [1 2] :b [2 3 4] :c [0]})
    (append-errors
      (errors {:a [1] :b [2 3]})
      {:a [2] :b [4] :c [0]})))
