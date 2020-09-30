parser grammar MablParser;

@header {
  //  package org.intocps.maestro.ast;
}

options { tokenVocab=MablLexer;}


compilationUnit
    : moduleDeclaration*  simulationSpecification? EOF
    ;

moduleDeclaration
    : MODULE IDENTIFIER ('{' functionDeclaration* '}')
    ;

functionDeclaration
    : ret=typeType IDENTIFIER formalParameters ';'
    ;

formalParameters
    : '(' formalParameterList? ')'
    ;

formalParameterList
    : formalParameter (',' formalParameter)*
    ;

formalParameter
    : typeType IDENTIFIER
    ;

simulationSpecification
    : SIMULATION (IMPORT imports+=IDENTIFIER ';')* block
    ;

block
    : '{' statement* '}'
    ;

statement
    : blockLabel=block                                          #blockStm
    | assignment                                                #assignmentStm
    |  var=variableDeclarator ';'                               #localVariable
    | IF parExpression then=statement (ELSE el=statement)?      #if
    | WHILE parExpression statement                             #while
    | statementExpression=expression  ';'                       #expressionStatement
    | SEMI                                                      #semi
    | OBSERVABLE                                                #observable
    | BREAK                                                     #break
    | INSTANCE_MAP identifier=IDENTIFIER '->'
                     name=STRING_LITERAL ';'                    #expandMapping
    ;


assignment
    : stateDesignator ASSIGN expression ';'
    ;

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
    | array=expression LBRACK index=expression  RBRACK  #arrayIndex
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
    : EXPAND? IDENTIFIER '(' expressionList? ')'
    ;

parExpression
    : '(' expression ')'
    ;

variableDeclarator
    : type=typeType varid=IDENTIFIER (LBRACK size+=expression (',' size+=expression )* RBRACK )? ('=' initializer=variableInitializer)?
    ;

variableInitializer
    : arrayInitializer  #arrayInit
    | expression        #expInit
    ;

arrayInitializer
    : '{' (init+=expression (',' init+=expression)* (',')? )? '}'
    ;

typeType
    : REF? (primitiveType | IDENTIFIER) (arrays+='[' ']')*
    ;

primitiveType
    : REAL              #realType
    | UINT              #uintType
    | INT               #intType
    | STRING            #stringType
    | BOOL              #boolType
    | QUESTION          #unknownType
    ;

literal
    : STRING_LITERAL
    | BOOL_LITERAL
    | DECIMAL_LITERAL
    | FLOAT_LITERAL
    | NULL_LITERAL
    ;
