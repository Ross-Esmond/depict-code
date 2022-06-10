(ns lang.core-test
  (:require [lang.core :refer [parse getCode getHtml spans]])
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :refer [join]]))
(use 'clojure.test)

(deftest parse-tests
  (testing "parses an anonymous function with various whitespace"
    (is (= (parse "function(){}") [:CodeBlock [:FunctionDec "function" "(" ")" "{" "}"]]))
    (is (= (parse "function () { }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                  [:CodeBlock [:WS " "]] "}"]]))
    (is (= (parse "function () {\n}") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                   [:CodeBlock [:WS "\n"]] "}"]]))
    (is (= (parse "function () {  }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                   [:CodeBlock [:WS "  "]] "}"]])))
  (testing "parses a function"
    (is (= (parse "function what() { }") [:CodeBlock
                                          [:FunctionDec "function" [:WS " "] [:Identifier "what"] "(" ")"
                                           [:WS " "] "{" [:CodeBlock [:WS " "]] "}"]])))
  (testing "parses various variable declarations"
    (is (= (parse "var what") [:CodeBlock [:VarDec "var" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "const what") [:CodeBlock [:VarDec "const" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "let what") [:CodeBlock [:VarDec "let" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "var what = 5") [:CodeBlock
                                   [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "] "=" [:WS " "] [:Number "5"]]]))
    (is (= (parse "const [a, b]=what") [:CodeBlock [:VarDec "const" [:WS " "]
                                                    [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                                    "=" [:Identifier "what"]]]))
    (is (= (parse "const [a, b] = do(5)") [:CodeBlock [:VarDec "const" [:WS " "]
                                                       [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                                       [:WS " "] "=" [:WS " "]
                                                       [:Invoke [:Identifier "do"] "(" [:Number "5"] ")"]]])))
  (testing "parses a function body"
    (is (= (parse "function () { var what }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                            [:CodeBlock [:WS " "]
                                                             [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "]]]
                                                            "}"]])))
  (testing "parses various statements"
    (is (= (parse "var one\nvar two") [:CodeBlock
                                       [:VarDec "var" [:WS " "] [:Identifier "one"] [:WS "\n"]]
                                       [:VarDec "var" [:WS " "] [:Identifier "two"]]]))
    (is (= (parse "return 5") [:CodeBlock [:Return "return" [:WS " "] [:Number "5"]]])))
  (testing "parses various comments"
    (is (= (parse "// what\n") [:CodeBlock [:WS [:Comment "//" " what" "\n"]]])))
  (testing "parses various expressions"
    (is (= (parse "5") [:CodeBlock [:Number "5"]]))
    (is (= (parse "what()") [:CodeBlock [:Invoke [:Identifier "what"] "(" ")"]]))
    (is (= (parse "what(5)") [:CodeBlock [:Invoke [:Identifier "what"] "(" [:Number "5"] ")"]]))
    (is (= [:CodeBlock [:Invoke [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"]] "(" [:Number "5"] ")"]]
           (parse "foo.bar(5)"))))
  (testing "parses jsx"
    (is (= (parse "<div />") [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] "/>"]]]))
    (is (= (parse "<div>text</div>") [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">"
                                                        [:PlainText "text"]
                                                        "</" [:Identifier "div"] ">"]]]))
    (is (= [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">" [:JS "{" [:Number "5"] "}"] "</" [:Identifier "div"] ">"]]]
           (parse "<div>{5}</div>")))
    (is (= [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" "\"" "a" "\""] [:WS " "] "/>"]]]
           (parse "<div id=\"a\" />")))
    (is (= [:CodeBlock [:JSX [:TagClosed "<" [:Identifier "div"] [:WS " "]
                              [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] [:WS " "] "/>"]]]
           (parse "<div id={5} />")))
    (is (= [:CodeBlock [:JSX
                        [:Tag "<" [:Identifier "div"] [:WS " "] [:Prop "id" "=" [:JS "{" [:Number "5"] "}"]] ">"
                         [:PlainText "foobar"] "</" [:Identifier "div"] ">"]]]
           (parse "<div id={5}>foobar</div>")))
    (is (= [:CodeBlock [:JSX [:Tag "<" [:Identifier "div"] ">"
                              [:Tag "<" [:Identifier "div"] ">" [:PlainText "foo"] "</" [:Identifier "div"] ">"]
                              "</" [:Identifier "div"] ">"]]]
           (parse "<div><div>foo</div></div>"))))
  (testing "parses object property access"
    (is (= [:CodeBlock [:PropAccess [:Identifier "foo"] "." [:Identifier "bar"] "." [:Identifier "baz"]]]
           (parse "foo.bar.baz"))))
  (testing "parses binary expressions"
    (is (= [:CodeBlock [:Number "5"] [:WS " "] "<=" [:WS " "] [:Number "2"]]
           (parse "5 <= 2")))))

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
                                                         ["line" ["keyword" "var"] " " ["identifier" "what"]] "<br />"
                                                         ["line" ["keyword" "var"] " " ["identifier" "who"]]])))))
