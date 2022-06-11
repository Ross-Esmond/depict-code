(ns lang.core-test
  (:require [lang.core :refer [parse getCode getHtml spans localize]])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :refer [join]]))
(use 'clojure.test)

(def cases
  {
   "function(){}" [:CodeBlock [:FunctionDec "function" "(" ")" "{" "}"]]
   "function () { }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                  [:CodeBlock [:WS " "]] "}"]]
   "function () {\n}" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                   [:CodeBlock [:WS "\n"]] "}"]]
   "function () {  }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                   [:CodeBlock [:WS "  "]] "}"]]
   "function what() { }" [:CodeBlock
                          [:FunctionDec "function" [:WS " "] [:Identifier "what"] "(" ")"
                           [:WS " "] "{" [:CodeBlock [:WS " "]] "}"]]
   "var what" [:CodeBlock [:VarDec "var" [:WS " "] [:Identifier "what"]]]
   "const what" [:CodeBlock [:VarDec "const" [:WS " "] [:Identifier "what"]]]
   "let what" [:CodeBlock [:VarDec "let" [:WS " "] [:Identifier "what"]]]
   "var what = 5" [:CodeBlock
                   [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "] "=" [:WS " "] [:Number "5"]]]
   "const [a, b]=what" [:CodeBlock [:VarDec "const" [:WS " "]
                                    [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                    "=" [:Identifier "what"]]]
   "const [a, b] = do(5)" [:CodeBlock [:VarDec "const" [:WS " "]
                                       [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                       [:WS " "] "=" [:WS " "]
                                       [:Invoke [:Identifier "do"] "(" [:Number "5"] ")"]]]
   "function () { var what }" [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                           [:CodeBlock [:WS " "]
                                            [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "]]]
                                           "}"]]
   "var one\nvar two" [:CodeBlock
                       [:VarDec "var" [:WS " "] [:Identifier "one"] [:WS "\n"]]
                       [:VarDec "var" [:WS " "] [:Identifier "two"]]]
   "return 5" [:CodeBlock [:Return "return" [:WS " "] [:Number "5"]]]
   "// what\n" [:CodeBlock [:WS [:Comment "//" " what" "\n"]]]
   "5" [:CodeBlock [:Number "5"]]
   "what()" [:CodeBlock [:Invoke [:Identifier "what"] "(" ")"]]
   "what(5)" [:CodeBlock [:Invoke [:Identifier "what"] "(" [:Number "5"] ")"]]
   "foo.bar(5)" [:CodeBlock [:Invoke [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"]] "(" [:Number "5"] ")"]]
   "<div />" [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] "/>"]]]
   "<div>text</div>" [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">" [:PlainText "text"] "</" [:Identifier "div"] ">"]]]
   "<div>{5}</div>" [:CodeBlock
                     [:JSX [:Tag "<" [:Identifier "div"] ">" [:JS "{" [:Number "5"] "}"] "</" [:Identifier "div"] ">"]]]
   "<div id=\"a\" />" [:CodeBlock
                       [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" "\"" "a" "\""] [:WS " "] "/>"]]]
   "<div id={5} />" [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "]
                                       [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] [:WS " "] "/>"]]]
   "<div id={5}>foobar</div>"
   [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] ">"
                      [:PlainText "foobar"] "</" [:Identifier "div"] ">"]]]
   "<div><div>foo</div></div>"
   [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">"
                      [:Tag "<" [:Identifier "div"] ">" [:PlainText "foo"] "</" [:Identifier "div"] ">"]
                      "</" [:Identifier "div"] ">"]]]
   "foo.bar.baz" [:CodeBlock [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"] "." [:Identifier "baz"]]]
   "5 <= 2" [:CodeBlock [:Number "5"] [:WS " "] "<=" [:WS " "] [:Number "2"]]})

(deftest parse-tests-auto
  (testing "parses various code"
    (doseq [[code parsed] cases]
      (is (= parsed (parse code))))))

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
                                                         ["line" ["keyword" "var"] " " ["identifier" "what"]] "<br />\n"
                                                         ["line" ["keyword" "var"] " " ["identifier" "who"]]])))))

(deftest localize-tests
  (testing "localize converts keywords"
    (is (= (localize [:CodeBlock [:Number "5"]]) [:lang.core/CodeBlock [:lang.core/Number "5"]]))))
