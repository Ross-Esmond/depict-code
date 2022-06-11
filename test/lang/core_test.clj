(ns lang.core-test
  (:require [lang.core :refer [parse getCode getHtml spans localize]])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :refer [join]]))
(use 'clojure.test)

(def cases
  [
   ["function(){}" [:CodeBlock [:FunctionDec "function" "(" ")" "{" "}"]]
    ["line" ["keyword" "function"] "(){}"]]
   ["function () { }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                   [:CodeBlock [:WS " "]] "}"]]
    ["line" ["keyword" "function"] " () { }"]]
   ["function () {\n}" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                    [:CodeBlock [:WS "\n"]] "}"]]
    [:frag ["line" ["keyword" "function"] " () {"] "\n" ["line" "}"]]]
   ["function () {  }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                    [:CodeBlock [:WS "  "]] "}"]]
    ["line" ["keyword" "function"] " () {  }"]]
   ["function what() { }" [:CodeBlock
                           [:FunctionDec "function" [:WS " "] [:Identifier "what"] "(" ")"
                            [:WS " "] "{" [:CodeBlock [:WS " "]] "}"]]
    ["line" ["keyword" "function"] " " ["identifier" "what"] "() { }"]]
   ["var what" [:CodeBlock [:VarDec "var" [:WS " "] [:Identifier "what"]]]
    ["line" ["keyword" "var"] " " ["identifier" "what"]]]
   ["const what" [:CodeBlock [:VarDec "const" [:WS " "] [:Identifier "what"]]]
    ["line" ["keyword" "const"] " " ["identifier" "what"]]]
   ["let what" [:CodeBlock [:VarDec "let" [:WS " "] [:Identifier "what"]]]
    ["line" ["keyword" "let"] " " ["identifier" "what"]]]
   ["var what = 5" [:CodeBlock
                    [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "] "=" [:WS " "] [:Number "5"]]]
    ["line" ["keyword" "var"] " " ["identifier" "what"] " = " ["number" "5"]]]
   ["const [a, b]=what" [:CodeBlock [:VarDec "const" [:WS " "]
                                     [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                     "=" [:Identifier "what"]]]
    ["line" ["keyword" "const"] " [" ["identifier" "a"] ", " ["identifier" "b"] "]=" ["identifier" "what"]]]
   ["const [a, b] = do(5)" [:CodeBlock [:VarDec "const" [:WS " "]
                                        [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                        [:WS " "] "=" [:WS " "]
                                        [:Invoke [:Identifier "do"] "(" [:Number "5"] ")"]]]
    ["line" ["keyword" "const"] " [" ["identifier" "a"] ", " ["identifier" "b"] "] = " ["identifier" "do"] "(" ["number" "5"] ")"]]
   ["function () { var what }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                            [:CodeBlock [:WS " "]
                                             [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "]]]
                                            "}"]]
    ["line" ["keyword" "function"] " () { " ["keyword" "var"] " " ["identifier" "what"] " }"]]
   ["var one\nvar two" [:CodeBlock
                        [:VarDec "var" [:WS " "] [:Identifier "one"] [:WS "\n"]]
                        [:VarDec "var" [:WS " "] [:Identifier "two"]]]
    [:frag ["line" ["keyword" "var"] " " ["identifier" "one"]] "\n" ["line" ["keyword" "var"] " " ["identifier" "two"]]]]
   ["return 5" [:CodeBlock [:Return "return" [:WS " "] [:Number "5"]]]
    ["line" ["keyword" "return"] " " ["number" "5"]]]
   ["// what\n" [:CodeBlock [:WS [:Comment "//" " what"] "\n"]]
    [:frag ["line" ["comment" "// what"]] "\n" ["line"]]]
   ["5" [:CodeBlock [:Number "5"]]
    ["line" ["number" "5"]]]
   ["what()" [:CodeBlock [:Invoke [:Identifier "what"] "(" ")"]]
    ["line" ["identifier" "what"] "()"]]
   ["what(5)" [:CodeBlock [:Invoke [:Identifier "what"] "(" [:Number "5"] ")"]]
    ["line" ["identifier" "what"] "(" ["number" "5"] ")"]]
   ["foo.bar(5)" [:CodeBlock [:Invoke [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"]] "(" [:Number "5"] ")"]]
    ["line" ["identifier" "foo"] "." ["identifier" "bar"] "(" ["number" "5"] ")"]]
   ["<div />" [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] "/>"]]]
    ["line" "&lt;" ["identifier" "div"] " />"]]
   ["<div>text</div>" [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">" [:PlainText "text"] "</" [:Identifier "div"] ">"]]]
    ["line" "&lt;" ["identifier" "div"] ">text&lt;/" ["identifier" "div"] ">"]]
   ["<div>{5}</div>"
    [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">" [:JS "{" [:Number "5"] "}"] "</" [:Identifier "div"] ">"]]]
    ["line" "&lt;" ["identifier" "div"] ">{" ["number" "5"] "}&lt;/" ["identifier" "div"] ">"]]
   ["<div id=\"a\" />"
    [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" "\"" "a" "\""] [:WS " "] "/>"]]]
    ["line" "&lt;" ["identifier" "div"] " " ["property" "id"] "=\"a\" />"]]
   ["<div id={5} />" [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "]
                                        [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] [:WS " "] "/>"]]]
    ["line" "&lt;" ["identifier" "div"] " " ["property" "id"] "={" ["number" "5"] "} />"]]
   ["<div id={5}>foobar</div>"
    [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] ">"
                       [:PlainText "foobar"] "</" [:Identifier "div"] ">"]]]
    ["line" "&lt;" ["identifier" "div"] " " ["property" "id"] "={" ["number" "5"] "}>foobar</" ["identifier" "div"] ">"]]
   ["<div><div>foo</div></div>"
    [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">"
                       [:Tag "<" [:Identifier "div"] ">" [:PlainText "foo"] "</" [:Identifier "div"] ">"]
                       "</" [:Identifier "div"] ">"]]]
    ["line" "&lt;" ["identifier" "div"] ">&lt;"
     ["identifier" "div"] ">foo&lt;/" ["identifier" "div"] ">&lt;/" ["identifier" "div"] ">"]]
   ["foo.bar.baz" [:CodeBlock [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"] "." [:Identifier "baz"]]]
    ["line" ["identifier" "foo"] "." ["identifier" "bar"] "." ["identifier" "baz"]]]
   ["5 <= 2" [:CodeBlock [:Binary [:Number "5"] [:WS " "] "<=" [:WS " "] [:Number "2"]]]
    ["line" ["number" "5"] " <= " ["number" "2"]]]])

(deftest parse-tests
  (testing "parses various code"
    (doseq [[code parsed] cases]
      (is (= parsed (parse code))))))

(deftest getHtml-test-auto
  (doseq [c cases]
    (if (= 3 (count c)) (testing (str "generates html for " (nth c 0)) (is (= (spans (nth c 2)) (getHtml (parse (nth c 0)))))))))

(comment
  (deftest parser-tests
    (testing "parses various code"
      (is (= [:Foo] ((parser [[:Foo "foo"]]) "foo"))))))

(deftest getCode-tests
  (testing "getCode recreates the string from a parse"
    (is (= (getCode (parse "5")) "5"))
    (is (= (getCode (parse "function what () { var foo = 5 }")) "function what () { var foo = 5 }"))))

(deftest spans-tests
  (testing "spans turns vectors into html"
    (is (= (spans ["foo" ["bar" "text" [:frag ["baz" "more text"]]] "last text"])
           "<span class=\"foo\"><span class=\"bar\">text<span class=\"baz\">more text</span></span>last text</span>"))
    (is (= (spans [:frag ["foo" "text"] " " ["bar" "more text"]])
           "<span class=\"foo\">text</span> <span class=\"bar\">more text</span>"))
    (is (= (spans [:frag ["foo" "text"] "\n" ["bar" "more text"]])
           "<span class=\"foo\">text</span>\n<span class=\"bar\">more text</span>"))))

(deftest getHtml-tests
  (testing "getHtml builds valid and expressive html"
    (is (= (getHtml (parse "5")) (spans ["line" ["number" "5"]])))
    (is (= (getHtml (parse "var what")) (spans ["line" ["keyword" "var"] " " ["identifier" "what"]])))
    (is (= (getHtml (parse "var what = 5")) (spans ["line" ["keyword" "var"] " " ["identifier" "what"] " = " ["number" "5"]])))
    (is (= (getHtml (parse "var what\nvar who")) (spans [:frag
                                                         ["line" ["keyword" "var"] " " ["identifier" "what"]] "\n"
                                                         ["line" ["keyword" "var"] " " ["identifier" "who"]]])))))

(deftest localize-tests
  (testing "localize converts keywords"
    (is (= (localize [:CodeBlock [:Number "5"]]) [:lang.core/CodeBlock [:lang.core/Number "5"]]))))
