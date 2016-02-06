(ns redux.utils)

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defmacro hndlr
  ([& body]
   `(fn [~'event] ~@body nil)))

(defn get-elem [id]
  (js/document.getElementById id))
