(ns tuples.core
  (:import (clojure.lang IPersistentVector
                         Associative
                         Sequential
                         IPersistentStack
                         Reversible
                         Indexed
                         IFn
                         Counted
                         Util
                         IObj
                         IMeta
                         IPersistentCollection
                         ILookup
                         Seqable
                         IMapEntry
                         MapEntry
                         RT)
           (java.util RandomAccess List Collection
                      Map$Entry)
           (java.io Serializable)))

(defmulti
  ^{:inline (fn [& args]
              (let [class-name (symbol (format "tuples.core.Tuple%s"
                                               (count args)))]
                `(new ~class-name
                      (into-array Object [~@args])
                      ~@args {})))}
  tuple (comp count list))

(defprotocol TupleAccess
  (get0 [t])
  (get1 [t])
  (get2 [t])
  (get3 [t])
  (get4 [t])
  (get5 [t])
  (get6 [t])
  (get7 [t])
  (get8 [t])
  (get9 [t]))

(defmacro tuple-for [n]
  (let [class-name (symbol (format "Tuple%s" n))
        fields (into {} (for [i (range n)]
                          [i (symbol (format "e%s" (inc i)))]))
        field-names (map second (sort-by first fields))
        ary (with-meta 'ary
              {:tag "[Ljava.lang.Object;"})]
    `(do
       (deftype ~class-name [~ary ~@field-names md#]
         ~@(when (= n 2)
             `[IMapEntry
               (key [v#]
                    ~(get fields 0))
               (val [v#]
                    ~(get fields 1))
               Map$Entry
               (getKey [v#]
                       ~(get fields 0))
               (getValue [v#]
                         ~(get fields 1))
               (setValue [v# _#]
                         (throw (UnsupportedOperationException.)))])
         TupleAccess
         ~@(for [i (range 10)]
             `(~(symbol (str "get" i)) [~'t]
               ~(if (> i n)
                  `(throw (UnsupportedOperationException.))
                  (get fields i))))
         Associative
         (containsKey [v# key#]
           (and (Util/isInteger key#)
                (<= 0 key# ~(dec n))))
         (entryAt [v# key#]
           (when (Util/isInteger key#)
             (let [key# (int key#)]
               (when (< key# ~n)
                 (MapEntry. key# (aget ~ary key#))))))
         (assoc [v# key# val#]
           (.assocN v# key# val#))
         Sequential ; Marker
         IPersistentStack
         (peek [v#]
           ~(last (sort-by first fields)))
         (pop [v#]
           (throw (UnsupportedOperationException.)))
         Reversible
         (rseq [v#]
           (throw (UnsupportedOperationException.)))
         Indexed
         (nth [v# i#]
           (.nth v# i# nil))
         (nth [v# i# not-found#]
           (let [i# (int i#)]
             (if (< i# ~n)
               (aget ~ary i#)
               not-found#)))
         Counted
         (count [v#] ~n)
         IPersistentVector
         (length [v#] ~n)
         (assocN [v# i# val#]
           (throw (UnsupportedOperationException.)))
         (cons [v# o#]
           (throw (UnsupportedOperationException.)))
         IFn
         (invoke [v# arg1#]
           (.nth v# arg1#))
         IObj
         (withMeta [v# m#]
           (throw (UnsupportedOperationException.)))
         IMeta
         (meta [v#] md#)
         IPersistentCollection
         (empty [v#] [])
         (equiv [v1# v2#]
           (if (instance? Sequential v2#)
             (if (= (count v2#) ~n)
               (zero? (.compareTo v1# v2#))
               false)
             false))
         Seqable
         (seq [v#]
           (seq ~ary))
         ILookup
         (valAt [v# key#]
           (.nth v# key# nil))
         (valAt [v# key# not-found#]
           (.nth v# key# not-found#))
         ;;JAVA
         RandomAccess ; Marker
         List
         (lastIndexOf [v# ~'obj]
           (cond
            ~@(for [i (range (dec n) 0 -1)
                    itm [`(= ~(get fields i) ~'obj) i]]
                itm)
            :else -1))
         (subList [v# fidx# tidx#]
           (throw (UnsupportedOperationException.)))
         (set [v# idx# o#]
           (throw (UnsupportedOperationException.)))
         (listIterator [v#]
           (throw (UnsupportedOperationException.)))
         (listIterator [v# idx#]
           (throw (UnsupportedOperationException.)))
         (add [v# obj# idx#]
           (throw (UnsupportedOperationException.)))
         (get [v# idx#]
           (.nth v# idx#))
         (indexOf [v# ~'obj]
           (cond
            ~@(for [i (range n)
                    itm [`(= ~(get fields i) ~'obj) i]]
                itm)
            :else -1))
         Collection
         (isEmpty [v#] false)
         (contains [v# k#]
           (and (Util/isInteger k#)
                (> ~(inc n) (long k#))
                (> (long k#) -1)))
         (size [v#]
           ~n)
         (toArray [v#]
           (RT/seqToArray (.seq v#)))
         (toArray [v# obj#]
           ; TODO bounds checking technically required
           (System/arraycopy ~ary 0 obj# 0 ~n))
         (addAll [v# collection#]
           (throw (UnsupportedOperationException.)))
         (iterator [v#]
           (.iterator (.seq v#)))
          (removeAll [v# c#]
            (throw (UnsupportedOperationException.)))
          (retainAll [v# c#]
            (throw (UnsupportedOperationException.)))
         (add [v# obj#]
           (throw (UnsupportedOperationException.))) 
         (clear [v#]
           (throw (UnsupportedOperationException.)))
         Comparable
         (compareTo [v1# v2#]
           (let [^IPersistentVector v2# v2#]
             (cond
              (> (count v2#) ~n)
              -1
              (< (count v2#) ~n)
              1
              :else
              (loop [i# 0]
                (if (> ~n i#)
                  (let [c# (Util/compare (.nth v1# i#) (.nth v2# i#))]
                    (if (zero? c#)
                      (recur (inc i#))
                      c#))
                  0)))))
         Serializable ; Marker
         )
       (defmethod tuple ~n [~@field-names]
         (new ~class-name
              (into-array Object [~@field-names])
              ~@field-names {})))))

(defmacro generate-tuples []
  `(do
     ~@(for [i (range 10)]
         `(tuple-for ~i))))

(generate-tuples)

