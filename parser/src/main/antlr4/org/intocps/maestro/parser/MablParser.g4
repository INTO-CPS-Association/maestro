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

//blockStatement
//    : localVariableDecleration ';'
//    | statement
//    ;

statement
    : blockLabel=block                                          #blockStm
    | assignment                                                #assignmentStm
    |  var=variableDeclarator ';'                               #localVariable
    | IF parExpression then=statement (ELSE el=statement)?      #if
 //   | FOR '(' forControl ')' statement
    | WHILE parExpression statement                             #while
    | statementExpression=expression  ';'                       #expressionStatement
//    | EXTERNAL methodCall  ';'                                  #methodExternalCallStm
    | SEMI                                                      #semi
    | OBSERVABLE                                                #observable
    | BREAK                                                     #break
    ;


assignment
    : stateDesignator ASSIGN expression ';'
    ;

stateDesignator
    : IDENTIFIER                                    #identifierStateDesignator
    | stateDesignator LBRACK literal RBRACK         #arrayStateDesignator
    ;


//FIXME fix operator precendence ||, && should bind harder than e.g. ==
expression
    : parExpression                                                                         #par
    | IDENTIFIER                                                                            # identifierExp
    | literal                                                                               # literalExp
    | LOAD LPAREN expressionList? RPAREN                                                    # loadExp
    | UNLOAD LPAREN IDENTIFIER RPAREN                                                       # unloadExp
    | fieldExpression                                                                       # fieldExp
//    | root=IDENTIFIER bop='.'
//        (
//             second= IDENTIFIER
//            | expression
//        )                                                   # dotExp
    | expression bop=('+'|'-') expression                                                   # plusMinusExp
    |  expression bop=('&&'|'||') expression                                                 # logicExp
    | expression bop=('<=' | '>=' | '>' | '<') expression                                   # numberComparizonExp
    |  expression bop=('==' | '!=') expression                                               # equalityExp
    | op=(BANG|SUB) expression                                                                   # unaryExp
    | <assoc=right> expression
          bop=('=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '>>=' | '>>>=' | '<<=' | '%=')
          expression                                                                        # updateExp
    | array=expression LBRACK indecies+=expression (',' indecies+=expression )* RBRACK      #arrayIndexExp
    | (root=fieldOrIdentifier bop='.') methodCall                                           #objectCallExp
    | methodCall                                                                            #callExp
    ;




fieldOrIdentifier
    : fieldExpression
    | IDENTIFIER
    ;

fieldExpression
    : root=IDENTIFIER bop='.' field=IDENTIFIER
    | rootExp=fieldExpression bop='.' field=IDENTIFIER
    ;

parExpression
    : '(' expression ')'
    ;

expressionList
    : expression (',' expression)*
    ;

methodCall
    : EXPAND? IDENTIFIER '(' expressionList? ')'
    ;



//localVariableDecleration
//    : type=typeType var=variableDeclarator ';'
//    ;

variableDeclarator
    : type=typeType varid=IDENTIFIER (LBRACK size+=expression (',' size+=expression )* RBRACK )? ('=' initializer=variableInitializer)?
    ;

//variableDeclaratorId
//    : IDENTIFIER ('[' DECIMAL_LITERAL ']')*
//
//    ;

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
    ;

//Module<FMI2> FMUA = Load("FMI2", "path/to/FMUA.fmu")
//Module<FMI2> FMUB = Load("FMI2", "path/to/FMUB.fmu")
