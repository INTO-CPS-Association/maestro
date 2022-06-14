parser grammar MablSwapConditionParser;

@header {
  //  package org.intocps.maestro.ast;
}

options { tokenVocab=MablSwapConditionLexer;}

stateDesignator
    : IDENTIFIER                                    #identifierStateDesignator
    | stateDesignator LBRACK expression RBRACK         #arrayStateDesignator
    ;

expTest : expression EOF;
//See https://github.com/antlr/grammars-v4/blob/master/java/java/JavaParser.g4#L471
expression
    : LPAREN expression RPAREN                          #parenExp
    | literal                                           #literalExp
    | IDENTIFIER                                        #identifierExp
    | expression '.'
        ( IDENTIFIER
        | methodCall)                                   #dotPrefixExp
    | array=expression (LBRACK index+=expression  RBRACK)+  #arrayIndex
    | methodCall                                        #plainMetodExp
    |  <assoc=right> op=('+'|'-') expression            #unaryExp
    | op=BANG expression                                #unaryExp
    | left=expression  bop=('*'|'/') right=expression   #binaryExp
    | left=expression  bop=('+'|'-') right=expression   #binaryExp
    | left=expression
        bop=
            ('<='
            | '>='
            | '>'
            | '<') right=expression                     #binaryExp
    | left=expression
        bop=
            ('=='
            | '!=') right=expression                    #binaryExp
    | left=expression   '&&' right=expression           #binaryExp
    | left=expression   '||' right=expression           #binaryExp
    ;

expressionList
    : expression (',' expression)*
    ;

methodCall
    : IDENTIFIER '(' expressionList? ')'
    ;

parExpression
    : '(' expression ')'
    ;








literal
    : STRING_LITERAL
    | BOOL_LITERAL
    | DECIMAL_LITERAL
    | FLOAT_LITERAL
    | NULL_LITERAL
    ;
