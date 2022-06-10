(ns lang.core
  (:require [instaparse.core :as insta])
  (:require [clojure.pprint :as pp])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :refer [join replace]]))

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
          [:PlainText & pt] [:PlainText (join pt)]
          [& items] (vec (map convert items))
          :else node))

(def insta-parser (insta/parser (slurp "./src/lang/js.bnf")))

(defn parse [in] (convert (insta-parser in)))

(defn getCode [node]
        (match node
               [:Number n] (str n)
               [& items] (join (map getCode items))
               (s :guard string?) node
               (kw :guard keyword?) ""))

(defn span [className inner] (str "<span class=\"" className "\">" inner "</span>"))
(defn spans [in]
  (match in
         [(className :guard string?) & children] (span className (join (map spans children)))
         [:frag & items] (join (map spans items))
         (s :guard string?) s))

(defn htmlVisit [node]
        (match node
               [:Number n] ["number" n]
               [(:or :CodeBlock :WS) & items] (vec (concat [:frag] (map htmlVisit items)))
               [:VarDec s & more] (vec (concat [:frag ["keyword" s]] (map htmlVisit more)))
               [:Identifier id] ["identifier" id]
               (s :guard string?) s
               :else (log node)))

(defn lineify [html]
        (str "<span class=\"line\">" (replace html "\n" "</span><br /><span class=\"line\">") "</span>"))

(defn getHtml [node]
        (lineify (spans (htmlVisit node))))

(defn -main [& args] (pp/pprint (parse (slurp "./src/lang/target.js"))))

