/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
grammar Expression;

options {
    output = AST;
    language = Java;
}

tokens {
    ASSIGNEE;
    SIGN_MINUS;
    SIGN_PLUS;
    FUNCTION;
    OPERATOR;
    UNARY_OPERATOR;
    VARIABLE;
    PROMPTVARIABLE;
    STRING;
}

@header { package net.rptools.parser; }

@lexer::header { package net.rptools.parser; }

expression:
                expr
                |
                assignmentExpression
    ;
    
assignmentExpression:
               (id=IDENTIFIER -> ^(ASSIGNEE[$id]))
               (t1=ASSIGN expr -> ^(OPERATOR[$t1]))
               expr
    ;

expr:
                orExpression
    ;
    
orExpression:
		andExpression
		(
                 (t1=OR -> ^(OPERATOR[$t1]))
                 andExpression
        )*
	    ;
    
andExpression:
		compareExpression
		(
                  (t1=AND compareExpression-> ^(OPERATOR[$t1]))
                  compareExpression
        ) *
	    ;
	
compareExpression:
		notExpression
                (
                    (
                    	t1=GE -> ^(OPERATOR[$t1])
                       |t2=GT -> ^(OPERATOR[$t2])
                       |t3=LT -> ^(OPERATOR[$t3])
                       |t4=LE -> ^(OPERATOR[$t4])
                       |t5=EQUALS -> ^(OPERATOR[$t5])
                       |t6=NOTEQUALS -> ^(OPERATOR[$t6])
                     ) 
                     notExpression
                 )*
                 ;
	
notExpression:
                (
                    ((t1=NOT -> ^(UNARY_OPERATOR[$t1])) additiveExpression)?
                    additiveExpression
                )
    ;
    
additiveExpression:   
                multiplicitiveExpression
                (
                    (
                    	t1=PLUS  -> ^(OPERATOR[$t1])
                       |t2=MINUS -> ^(OPERATOR[$t2])
                     ) 
                     multiplicitiveExpression
               )*
    ;

multiplicitiveExpression:
                powerExpression
                (
                	(
                	    t1=MULTIPLY -> ^(OPERATOR[$t1])
                	   |t2=DIVIDE   -> ^(OPERATOR[$t2])
                	) 
                	powerExpression
                )*
    ;
    
powerExpression:
                unaryExpression
                (
                	(t1=POWER -> ^(OPERATOR[$t1]))
                	unaryExpression)*
	;

unaryExpression:
                (
                    (t1=PLUS -> ^(UNARY_OPERATOR[$t1])
                    |
                    t2=MINUS -> ^(UNARY_OPERATOR[$t2])
                    |
                    t3=NOT constantExpression -> ^(UNARY_OPERATOR[$t3]))?
                    constantExpression
                )
    ;

constantExpression:
                (function) => function
                |
                NUMBER
                |
                HEXNUMBER
                |
                TRUE
                |
                FALSE
                |
                variable
                |
                t1=SINGLE_QUOTED_STRING -> ^(STRING[$t1])
                |
                t2=DOUBLE_QUOTED_STRING -> ^(STRING[$t2])
                | 
                LPAREN! expr RPAREN!
    ;



variable:
               QUESTION id2=IDENTIFIER -> ^(PROMPTVARIABLE[$id2])
               |
               id1=IDENTIFIER -> ^(VARIABLE[$id1])
    ;

function:
               id=IDENTIFIER LPAREN parameterList RPAREN -> ^(FUNCTION[$id]) parameterList
    ;

parameterList: (expr (COMMA! expr)* )?
    ;


ASSIGN  :   '=' ;
QUOTE   :   '"' ;

// Logical operators
OR      :    '||';
AND     :    '&&';
NOT	:     '!';
EQUALS  :    '==';
NOTEQUALS:   '!=';
GE      :    '>=';
GT      :    '>' ;
LT      :    '<' ;
LE      :    '<=';
TRUE	:  'true';
FALSE   :  'false';

// Math operators
PLUS    :   '+' ;
MINUS   :   '-' ;
MULTIPLY:   '*' ;
DIVIDE  :   '/' ;
POWER   :   '^' ;

LPAREN  :   '(' ;
RPAREN  :   ')' ;
COMMA   :   ',' ;
WS      :   ( ' '
            | '\r\n'
            | '\n'
            | '\t'
            )
            {skip();}
    ;
SEMI     : ';' ;
QUESTION : '?' ;

NUMBER               : INT ('.' INT)? ;
HEXNUMBER            : '0' 'x' (HEXDIGIT)+ ;
IDENTIFIER           : LETTER (LETTER|DIGIT|'.'|'_')* ;
SINGLE_QUOTED_STRING : '\'' ( ~'\'' )* '\'' ;
DOUBLE_QUOTED_STRING : '\"' ( ~'\"' )* '\"' ;

fragment HEXDIGIT   : ('0'..'9'|'A'..'F'|'a'..'f');
fragment DID        : ('d' | 'D') ;
fragment INT        : ('0'..'9')+ ;
fragment DIGIT      : '0'..'9' ;
fragment LETTER     : ('A'..'Z'|'a'..'z'|'\u00c0'..'\u00d6'|'\u00d8'..'\u00f6'|'\u00f8'..'\u00ff') ;



