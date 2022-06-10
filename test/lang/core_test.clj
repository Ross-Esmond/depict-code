(ns lang.core-test
  (:require
    [lang.core :refer [parse]]))
(use 'clojure.test)

(deftest anon-fn
  (testing "parses an anonymous function with various whitespace"
    (is (= (parse "function(){}") [:CodeBlock [:FunctionDec "function" "(" ")" "{" "}"]]))
    (is (= (parse "function () { }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                  [:CodeBlock [:WS " "]] "}"]]))
    (is (= (parse "function () {\n}") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                   [:CodeBlock [:WS "\n"]] "}"]]))
    (is (= (parse "function () {  }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                   [:CodeBlock [:WS "  "]] "}"]]))))

(deftest var-dec
  (testing "parses various variable declarations"
    (is (= (parse "var what") [:CodeBlock [:VarDec "var" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "const what") [:CodeBlock [:VarDec "const" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "let what") [:CodeBlock [:VarDec "let" [:WS " "] [:Identifier "what"]]]))
    (is (= (parse "var what = 5") [:CodeBlock [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "] "=" [:WS " "] [:Number 5]]]))
    (is (= (parse "const [a, b]=what") [:CodeBlock [:VarDec "const" [:WS " "]
                                                   [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                                   "=" [:Identifier "what"]]]))
    (is (= (parse "const [a, b] = do(5)") [:CodeBlock [:VarDec "const" [:WS " "]
                                                       [:Array "[" [:Identifier "a"] "," [:WS " "] [:Identifier "b"] "]"]
                                                       [:WS " "] "=" [:WS " "]
                                                       [:Invoke [:Identifier "do"] "(" [:Number 5] ")"]]]))))

(deftest function-body
  (testing "parses a function body"
    (is (= (parse "function () { var what }") [:CodeBlock [:FunctionDec "function" [:WS " "] "(" ")" [:WS " "] "{"
                                                            [:CodeBlock [:WS " "]
                                                             [:VarDec "var" [:WS " "] [:Identifier "what"] [:WS " "]]]
                                                            "}"]]))))

(deftest statements
  (testing "parses various statements"
    (is (= (parse "var one\nvar two") [:CodeBlock
                                       [:VarDec "var" [:WS " "] [:Identifier "one"] [:WS "\n"]]
                                       [:VarDec "var" [:WS " "] [:Identifier "two"]]]))))

(deftest comments
  (testing "parses various comments"
    (is (= (parse "// what\n") [:CodeBlock [:WS [:Comment "//" " what" "\n"]]]))))
