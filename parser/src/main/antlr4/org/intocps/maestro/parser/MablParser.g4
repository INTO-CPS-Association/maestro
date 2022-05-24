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

formalParameter
    //java style only for array otherwise use: (dimentions+=LBRACK RBRACK )* after identifier
    : direction=OUT? typeType IDENTIFIER (dimentions+=LBRACK RBRACK )*
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
    | '||' block                                                #parallelBlockStm
    | assignment                                                #assignmentStm
    | var=variableDeclarator ';'                                #localVariable
    | IF parExpression then=statement (ELSE el=statement)?      #if
    | WHILE parExpression statement                             #while
    | statementExpression=expression  ';'                       #expressionStatement
    | SEMI                                                      #semi
    | OBSERVABLE                                                #observable
    | BREAK                                                     #break
    | AT_FMU_MAP LPAREN identifier=IDENTIFIER '->'
        name=STRING_LITERAL RPAREN ';'                          #fmuMapping
    | AT_INSTANCE_MAP LPAREN identifier=IDENTIFIER '->'
                     name=STRING_LITERAL RPAREN ';'             #instanceMapping
    | AT_TRANSFER (LPAREN (names+=STRING_LITERAL
                    (',' names+=STRING_LITERAL)*)*  RPAREN)* ';'  #transfer
    | AT_CONFIG LPAREN  config=STRING_LITERAL  RPAREN ';'       #config
    | ERROR expression? ';'                                     #error
    | TRY tryBlock=block FINALLY finallyBlock=block                                   #try
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
    | array=expression (LBRACK index+=expression  RBRACK)+  #arrayIndex
    | methodCall                                        #plainMetodExp
    |  <assoc=right> op=('+'|'-') expression            #unaryExp
    | op=BANG expression                                #unaryExp
    | REF expression                                    #refExpression
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
    : type=typeType varid=IDENTIFIER  (LBRACK size+=expression RBRACK)*  ('=' initializer=variableInitializer)?
    ;

variableInitializer
    : arrayInitializer  #arrayInit
    | '{' elementInitializer+= variableInitializer (','  elementInitializer+=variableInitializer)* '}' #arrayMultidimentionalInit
    | expression        #expInit
    ;

arrayInitializer
    : '{' (init+=expression (',' init+=expression)* (',')? )? '}'
    ;



typeType
    : type=namedOrPrimitiveType                    #andmedOrPrimitiveTypeType
    | type=typeType (dimentions+=LBRACK RBRACK)+    #arrayTypeType
    ;

namedOrPrimitiveType
    :  type=IDENTIFIER                              #identifierTypeType
    |  type=primitiveType                           #primitiveTypeType
    ;

primitiveType
    : REAL                              #realType
    | UINT                              #uintType
    | INT                               #intType
    | STRING                            #stringType
    | BOOL                              #boolType
    | QUESTION                          #unknownType
    | VOID                              #voidType
    ;

literal
    : STRING_LITERAL
    | BOOL_LITERAL
    | DECIMAL_LITERAL
    | FLOAT_LITERAL
    | NULL_LITERAL
    ;
