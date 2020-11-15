parser grammar MablParser;

@header {
  //  package org.intocps.maestro.ast;
}

options { tokenVocab=MablLexer;}


compilationUnit
    : moduleDeclaration*  simulationSpecification? EOF
    ;

moduleDeclaration
    : MODULE name=IDENTIFIER (IMPORT imports+=IDENTIFIER ';')* ('{' functionDeclaration* '}')
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

//TODO: Multidimensional arrays
formalParameter
    : typeType IDENTIFIER (LBRACK RBRACK )?
    ;

framework
    : AT_FRAMEWORK LPAREN (names+=STRING_LITERAL ','? )* RPAREN
    ;

frameworkConfigs
    : AT_FRAMEWORK_CONFIG LPAREN frameworkName=STRING_LITERAL ',' config=STRING_LITERAL  RPAREN ';'
    ;

simulationSpecification
    : SIMULATION (IMPORT imports+=IDENTIFIER ';')* (frameworkList=framework ';')? frameworkConfigList=frameworkConfigs* statement
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
    | AT_CONFIG LPAREN  config=STRING_LITERAL  RPAREN ';'       #config
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
// TODO: Multidimensional arrays - The current way of multi size arrays is a[1,2,3] which does not make sense. It should be: a[1][2][3].
variableDeclarator
    : type=typeType varid=IDENTIFIER (LBRACK (size+=expression (',' size+=expression )*)? RBRACK )? ('=' initializer=variableInitializer)?
    ;

variableInitializer
    : arrayInitializer  #arrayInit
    | expression        #expInit
    ;

arrayInitializer
    : '{' (init+=expression (',' init+=expression)* (',')? )? '}'
    ;

typeType
    : REF? (primitiveType | IDENTIFIER)
    ;

primitiveType
    : REAL              #realType
    | UINT              #uintType
    | INT               #intType
    | STRING            #stringType
    | BOOL              #boolType
    | QUESTION          #unknownType
    | VOID              #voidType
    ;

literal
    : STRING_LITERAL
    | BOOL_LITERAL
    | DECIMAL_LITERAL
    | FLOAT_LITERAL
    | NULL_LITERAL
    ;
