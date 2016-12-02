(ns jank-benchmark.util)

(defn keywordify [m]
  (cond
    (map? m) (into {} (for [[k v] m] [(keyword k) (keywordify v)]))
    (coll? m) (vec (map keywordify m))
    :else m))

(defn extract
  "Extract from the given map each pair for the given keys.
   Example: (= (extract {:a 0 :b 1 :c 2 :d 3} [:a :d]) {:a 0 :d 3})"
  [m ks]
  (loop [ret {}
         ks' ks]
    (let [k (keyword (first ks'))]
      (if (empty? ks')
        ret
        (recur (assoc ret k (get m k))
               (rest ks'))))))
