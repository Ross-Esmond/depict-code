(ns lang.core
  (:require [instaparse.core :as insta])
  (:require [clojure.pprint :as pp])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as st :refer [join lower-case]]))

(defn log [what] (do (pp/pprint what) what))

(def insta-parser (insta/parser (slurp "./src/lang/js.bnf")))

(defn parse [in] (insta-parser in))

(defn getCode [node]
        (match node
               [& items] (join (map getCode items))
               (s :guard string?) node
               (kw :guard keyword?) ""))

(defn span [className inner] (str "<span class=\"" className "\">" inner "</span>"))
(defn spans [in]
  (match in
         [(className :guard string?) & children] (span className (join (map spans children)))
         [:frag & items] (join (map spans items))
         (s :guard string?) (st/replace s "<" "&lt;")))

(def keywords #{"function" "var" "let" "const" "return"})

(defmulti htmlVisit #(cond (vector? %1) (first %1) (string? %1) :str :else %1))
(defmethod htmlVisit ::Number [[_ n]] ["number" n])
(defmethod htmlVisit ::Comment [[_ & items]] ["comment" (join items)])
(defmethod htmlVisit ::named [[kw value]] [(lower-case (name kw)) value])
(defmethod htmlVisit ::Prop [node]
        (match node
               [::Prop title "=" "\"" value "\""] [:frag ["property" title] (str "=\"" value "\"")]
               [::Prop title "=" js] [:frag ["property" title] "=" (htmlVisit js)]))
(defmethod htmlVisit :str [s] (if (contains? keywords s) ["keyword" s] s))
(defmethod htmlVisit :default [s]
        (match s
               [_ & items] (vec (concat [:frag] (map htmlVisit items)))
               :else (log s)))

(derive ::Identifier ::named)

(defn localize [node]
        (cond
          (keyword? node) (keyword "lang.core" (name node))
          (vector? node) (vec (map localize node))
          :else node))

(defn lineify [html]
        (str "<span class=\"line\">" (st/replace html "\n" "</span>\n<span class=\"line\">") "</span>"))

(defn getHtml [node]
        (lineify (spans (htmlVisit (localize node)))))

(defn parser [structure]
        (fn [code]))

(defn -main [& args] (println (getHtml (parse (slurp "./src/lang/target.js")))))

