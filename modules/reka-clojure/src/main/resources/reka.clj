(ns reka)

(def shutdown-collector (atom (fn [])))

(defn set-shutdown-collector [collector]
  (reset! shutdown-collector collector))

(defn on-shutdown [f]
  (.accept @shutdown-collector f))

(defn call-from-reka [f m]
  (f (into {} m)))

(defn define-callback [name runnable]
  (let [f (fn [] (.run runnable))
        [a b] (clojure.string/split name #"/")]
    (cond (nil? b) (intern *ns* (symbol a) f)
          :else (intern (create-ns (symbol a)) (symbol b) f))))