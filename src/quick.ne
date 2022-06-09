code -> (FunctionDeclaration):* {% ([statements]) => statements.map(([s]) => s) %}
FunctionDeclaration -> FunctionKeyword Identifier Parameters CodeBlock {% (data) => ['function', ...data] %}
FunctionKeyword -> "function" __ {% ([kw, [ws]]) => ['keyword', kw, ws] %}
Parameters -> "(" ParameterList:? ")" __ {% ([l, list, r, ws]) => ['parameters', l, list, r, ws] %}
ParameterList -> __ (Identifier "," __):* Identifier
Identifier -> [a-zA-Z]:+ __ {% ([name, ws]) => ['identifier', name.join(''), ws] %}
CodeBlock -> "{" __ code "}" __ {% ([l, ws, code, r, tws]) => ['codeblock', l, ws, code, r, tws] %}
__ -> (" " | "\r\n" | "\r" | "\n"):* {% ([data]) => data.join('') %}

