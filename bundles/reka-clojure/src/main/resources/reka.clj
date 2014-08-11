(ns reka)

(defn call-from-reka [f m]
  (f (into {} m)))

  
(defn define-callback [name runnable]
  (let [f (fn [] (.run runnable))
        [a b] (clojure.string/split name #"/")]
    (cond (nil? b) (intern *ns* (symbol a) f)
          :else (intern (create-ns (symbol a)) (symbol b) f))))