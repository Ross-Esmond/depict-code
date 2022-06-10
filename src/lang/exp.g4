grammar exp;

program
 : Function
 ;

Function
 : 'function' WS? '(' ')' WS? '{' '}'
 ;

WS
 : ([\t\u000B\u000C\u0020\u00A0\u2028\u2029]|'\r'|'\n'|'\r\n')+
 ;
