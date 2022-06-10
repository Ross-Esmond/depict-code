(ns lang.core
  (:require [instaparse.core :as insta])
  (:require [clojure.pprint :as pp])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :refer [join]]))

(defn log [what] (do (pp/pprint what) what))

(defn convert [node]
        (match node
          [:WS & ws] (reduce
                        #(if
                          (and (string? (last %1)) (string? %2))
                          (conj (vec (butlast %1)) (str (last %1) %2))
                          (conj %1 %2))  
                        [:WS]
                        ws)
          [:Number n] [:Number (Integer/parseInt n)]
          [& items] (map convert items)
          :else node))

(def insta-parser (insta/parser (slurp "./src/lang/js.bnf")))

(defn parse [in] (convert (insta-parser in)))

(defn -main [& args] (pp/pprint (parse (first args))))

