package org.intocps.maestro.parser;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

public class ParseTree2AstConverter extends MablParserBaseVisitor<INode> {
    final static Logger logger = LoggerFactory.getLogger(ParseTree2AstConverter.class);

    @Override
    public INode visitCompilationUnit(MablParser.CompilationUnitContext ctx) {

        ARootDocument doc = new ARootDocument();

        List<PCompilationUnit> list =
                ctx.moduleDeclaration().stream().map(this::visit).map(PCompilationUnit.class::cast).collect(Collectors.toCollection(Vector::new));

        if (ctx.simulationSpecification() != null) {
            list.add((PCompilationUnit) this.visit(ctx.simulationSpecification()));
        }
        doc.setContent(list);
        return doc;
    }

    @Override
    public INode visitModuleDeclaration(MablParser.ModuleDeclarationContext ctx) {

        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();

        AModuleDeclaration module = new AModuleDeclaration();
        unit.setModule(module);

        module.setName(convert(ctx.name));
        if (ctx.imports != null && !ctx.imports.isEmpty()) {
            unit.setImports(ctx.imports.stream().map(this::convert).collect(Collectors.toList()));
        }

        module.setFunctions(ctx.functionDeclaration().stream().map(this::visit).map(AFunctionDeclaration.class::cast).collect(Collectors.toList()));


        return unit;
    }

    @Override
    public INode visitTry(MablParser.TryContext ctx) {
        ATryStm tryStm = new ATryStm();
        tryStm.setBody((ABasicBlockStm) visit(ctx.tryBlock));
        tryStm.setFinally((ABasicBlockStm) visit(ctx.finallyBlock));
        return tryStm;
    }

    @Override
    public INode visitError(MablParser.ErrorContext ctx) {
        AErrorStm errStm = new AErrorStm();
        if (ctx.expression() != null) {
            errStm.setExp((PExp) visit(ctx.expression()));
        }

        return errStm;
    }

    @Override
    public INode visitFunctionDeclaration(MablParser.FunctionDeclarationContext ctx) {
        AFunctionDeclaration fun = new AFunctionDeclaration();

        fun.setName(convert(ctx.IDENTIFIER()));
        fun.setReturnType((PType) this.visit(ctx.ret));
        if (ctx.formalParameters().formalParameterList() != null) {
            fun.setFormals(ctx.formalParameters().formalParameterList().formalParameter().stream().map(this::visit).map(AFormalParameter.class::cast)
                    .collect(Collectors.toList()));
        }
        return fun;
    }

    @Override
    public INode visitFormalParameter(MablParser.FormalParameterContext ctx) {

        AFormalParameter parameter = new AFormalParameter();

        parameter.setName(convert(ctx.IDENTIFIER()));

        PType type = (PType) this.visit(ctx.typeType());
        if (!(type instanceof AArrayType)) {

            for (int i = 0; i < ctx.dimentions.size(); i++) {
                type = new AArrayType(type);
            }
        } else {
            //todo warn we have double decleration of array. either use C style or Java not both
        }

        if (ctx.direction != null && ctx.direction.getType() == MablParser.OUT) {
            AReferenceType refType = new AReferenceType();
            refType.setType(type);
            parameter.setType(refType);
        } else {
            parameter.setType(type);
        }

        return parameter;
    }

    @Override
    public INode visitSimulationSpecification(MablParser.SimulationSpecificationContext ctx) {

        ASimulationSpecificationCompilationUnit unit = new ASimulationSpecificationCompilationUnit();

        unit.setBody((PStm) this.visit(ctx.statement()));

        if (ctx.imports != null && !ctx.imports.isEmpty()) {
            unit.setImports(ctx.imports.stream().map(this::convert).collect(Collectors.toList()));
        }

        if (ctx.framework() != null && !ctx.framework().isEmpty()) {
            unit.setFramework(ctx.framework().names.stream()
                    .map(s -> new LexIdentifier(s.getText().substring(1, s.getText().length() - 1), convertToLexToken(s)))
                    .collect(Collectors.toList()));
        }

        if (ctx.frameworkConfigs() != null && !ctx.frameworkConfigs().isEmpty()) {
            unit.setFrameworkConfigs(ctx.frameworkConfigs().stream().map(this::visit).map(AConfigFramework.class::cast).collect(Collectors.toList()));
        }

        return unit;

    }

    @Override
    public INode visitFrameworkConfigs(MablParser.FrameworkConfigsContext ctx) {
        AConfigFramework config = new AConfigFramework();
        config.setName(new LexIdentifier(ctx.frameworkName.getText().substring(1, ctx.frameworkName.getText().length() - 1),
                convertToLexToken(ctx.frameworkName)));
        if (ctx.config != null) {
            config.setConfig(ctx.config.getText().substring(1, ctx.config.getText().length() - 1));
        }

        return config;
    }

    @Override
    public INode visitBlock(MablParser.BlockContext ctx) {
        ABasicBlockStm block = new ABasicBlockStm();

        List<INode> processedBody =
                ctx.statement().stream().filter(p -> !(p instanceof MablParser.SemiContext)).map(this::visit).collect(Collectors.toList());

        processedBody.stream().filter(p -> !(p instanceof PStm)).forEach(s -> System.out.println("Wrong node type in body: " + s));

        List<PStm> statements = processedBody.stream().map(PStm.class::cast).collect(Collectors.toList());

        if (statements.stream().anyMatch(Objects::isNull)) {
            logger.warn("found null");
        }

        block.setBody(statements);
        return block;
    }

    @Override
    public INode visitParallelBlockStm(MablParser.ParallelBlockStmContext ctx) {
        AParallelBlockStm parBlock = new AParallelBlockStm();
        parBlock.setBody((((ABasicBlockStm) this.visit(ctx.block())).getBody()));
        return parBlock;
    }

    @Override
    public INode visitConfig(MablParser.ConfigContext ctx) {
        AConfigStm config = new AConfigStm();
        config.setConfig(ctx.config.getText().substring(1, ctx.STRING_LITERAL().getText().length() - 1));
        return config;
    }

    @Override
    public INode visitBreak(MablParser.BreakContext ctx) {

        return new ABreakStm(convertToLexToken(ctx.BREAK().getSymbol()));
    }

    @Override
    public INode visitAssignment(MablParser.AssignmentContext ctx) {

        AAssigmentStm assign = new AAssigmentStm();

        assign.setTarget((PStateDesignator) this.visit(ctx.stateDesignator()));
        assign.setExp((PExp) this.visit(ctx.expression()));

        return assign;
    }


    @Override
    public INode visitArrayStateDesignator(MablParser.ArrayStateDesignatorContext ctx) {
        AArrayStateDesignator designator = new AArrayStateDesignator();
        designator.setTarget((PStateDesignator) this.visit(ctx.stateDesignator()));
        designator.setExp((PExp) this.visit(ctx.expression()));
        return designator;
    }

    @Override
    public INode visitIdentifierStateDesignator(MablParser.IdentifierStateDesignatorContext ctx) {
        AIdentifierStateDesignator identifierExp = new AIdentifierStateDesignator();
        identifierExp.setName(this.convert(ctx.IDENTIFIER()));
        return identifierExp;
    }

    @Override
    public INode visitWhile(MablParser.WhileContext ctx) {

        AWhileStm stm = new AWhileStm();
        stm.setTest((PExp) this.visit(ctx.parExpression()));
        stm.setBody((PStm) this.visit(ctx.statement()));
        return stm;

    }

    @Override
    public INode visitBinaryExp(MablParser.BinaryExpContext ctx) {

        SBinaryExp exp = null;
        if (ctx.MUL() != null) {
            exp = new AMultiplyBinaryExp();
        } else if (ctx.DIV() != null) {
            exp = new ADivideBinaryExp();
        } else if (ctx.ADD() != null) {
            exp = new APlusBinaryExp();
        } else if (ctx.SUB() != null) {
            exp = new AMinusBinaryExp();
        } else if (ctx.LE() != null) {
            exp = new ALessEqualBinaryExp();
        } else if (ctx.GE() != null) {
            exp = new AGreaterEqualBinaryExp();
        } else if (ctx.GT() != null) {
            exp = new AGreaterBinaryExp();
        } else if (ctx.LT() != null) {
            exp = new ALessBinaryExp();
        } else if (ctx.EQUAL() != null) {
            exp = new AEqualBinaryExp();
        } else if (ctx.NOTEQUAL() != null) {
            exp = new ANotEqualBinaryExp();
        } else if (ctx.AND() != null) {
            exp = new AAndBinaryExp();
        } else if (ctx.OR() != null) {
            exp = new AOrBinaryExp();
        }

        exp.setLeft((PExp) this.visit(ctx.left));
        exp.setRight((PExp) this.visit(ctx.right));

        return exp;
    }

    @Override
    public INode visitParenExp(MablParser.ParenExpContext ctx) {
        return new AParExp((PExp) this.visit(ctx.expression()));
    }

    @Override
    public INode visitLiteralExp(MablParser.LiteralExpContext ctx) {
        return this.visit(ctx.literal());
    }

    @Override
    public INode visitDotPrefixExp(MablParser.DotPrefixExpContext ctx) {

        PExp root = (PExp) this.visit(ctx.expression());

        if (ctx.IDENTIFIER() != null) {
            //field access
            AFieldExp fieldExp = new AFieldExp();
            fieldExp.setRoot(root);
            fieldExp.setField(convert(ctx.IDENTIFIER()));
            return fieldExp;
        } else if (ctx.methodCall() != null) {
            //object call
            ACallExp call = (ACallExp) this.visit(ctx.methodCall());
            call.setObject(root);
            return call;
        }

        return null;
    }

    @Override
    public INode visitPlainMetodExp(MablParser.PlainMetodExpContext ctx) {
        return this.visit(ctx.methodCall());
    }


    @Override
    public INode visitUnaryExp(MablParser.UnaryExpContext ctx) {
        SUnaryExp exp = null;
        if (ctx.BANG() != null) {
            exp = new ANotUnaryExp();
        } else if (ctx.ADD() != null) {
            exp = new APlusUnaryExp();
        } else if (ctx.SUB() != null) {
            exp = new AMinusUnaryExp();
        }
        exp.setExp((PExp) this.visit(ctx.expression()));

        return exp;
    }

    @Override
    public INode visitIdentifierExp(MablParser.IdentifierExpContext ctx) {
        return new AIdentifierExp(convert(ctx.IDENTIFIER()));
    }

    @Override

    public INode visitArrayIndex(MablParser.ArrayIndexContext ctx) {
        AArrayIndexExp apply = new AArrayIndexExp();

        apply.setArray((PExp) this.visit(ctx.array));

        if (ctx.index != null) {
            apply.setIndices(ctx.index.stream().map(e -> (PExp) this.visit(e)).collect(Collectors.toList()));
        }
        return apply;
    }

    @Override
    public INode visitObservable(MablParser.ObservableContext ctx) {
        return new AObservableStm();
    }

    @Override
    public INode visitParExpression(MablParser.ParExpressionContext ctx) {
        return new AParExp((PExp) this.visit(ctx.expression()));
    }

    void checkList(List source, List processed) {
        if (!source.stream().anyMatch(p -> p == null)) {
            return;
        }

        for (int i = 0; i < source.size(); i++) {
            if (processed.get(i) == null) {
                System.out.println("Problem translating: " + source.get(i).getClass().getSimpleName());
            }
        }
    }

    private LexToken convertToLexToken(Token token) {
        return new LexToken(token.getText(), token.getLine(), token.getCharPositionInLine());
    }

    @Override
    public INode visitMethodCall(MablParser.MethodCallContext ctx) {

        ACallExp call = new ACallExp();

        if (ctx.expressionList() != null && ctx.expressionList().expression() != null) {
            List<PExp> args = ctx.expressionList().expression().stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList());
            checkList(ctx.expressionList().expression(), args);
            call.setArgs(args);
        }

        if (ctx.EXPAND() != null) {
            call.setExpand(convertToLexToken(ctx.EXPAND().getSymbol()));
        }

        call.setMethodName(convert(ctx.IDENTIFIER()));

        if (call.getMethodName().getText().equals("load")) {
            ALoadExp load = new ALoadExp();
            load.setArgs(call.getArgs());
            return load;
        } else if (call.getMethodName().getText().equals("unload")) {
            AUnloadExp unload = new AUnloadExp();
            unload.setArgs(call.getArgs());
            return unload;
        }

        return call;
    }

    @Override
    public INode visitExpressionStatement(MablParser.ExpressionStatementContext ctx) {
        AExpressionStm stm = new AExpressionStm();
        stm.setExp((PExp) this.visit(ctx.statementExpression));
        return stm;
    }

    @Override
    public INode visitIf(MablParser.IfContext ctx) {

        AIfStm stm = new AIfStm();
        stm.setTest((PExp) this.visit(ctx.parExpression()));
        stm.setThen((PStm) this.visit(ctx.then));
        if (ctx.el != null) {
            stm.setElse((PStm) this.visit(ctx.el));
        }
        return stm;
    }

    @Override
    public INode visitVariableDeclarator(MablParser.VariableDeclaratorContext ctx) {
        AVariableDeclaration def = new AVariableDeclaration();

        PType primitiveType = (PType) this.visit(ctx.typeType());
        def.setType(primitiveType);
        def.setName(convert(ctx.IDENTIFIER()));


        //build
        //            PType arrayType = primitiveType;
        if (ctx.size != null && !ctx.size.isEmpty()) {
            List<PExp> sizes = new Vector<>();


            for (int i = 0; i < ctx.size.size(); i++) {

                PExp elementSize = (PExp) this.visit(ctx.size.get(i));
                if (elementSize != null) {
                    sizes.add(elementSize);
                }

                //                arrayType = new AArrayType(arrayType);

            }
            def.setSize(sizes);
            //            if (def.getType() == null) {
            //                def.setType(arrayType);
            //            } else {
            //            }
            //        }
        }

        MablParser.VariableInitializerContext initializer = ctx.variableInitializer();
        if (initializer != null) {
            def.setInitializer((PInitializer) this.visit(initializer));
        }

        ALocalVariableStm var = new ALocalVariableStm();
        var.setDeclaration(def);
        return var;
    }

    @Override
    public INode visitLocalVariable(MablParser.LocalVariableContext ctx) {
        return this.visit(ctx.variableDeclarator());
    }

    @Override
    public INode visitArrayInit(MablParser.ArrayInitContext ctx) {
        AArrayInitializer initializer = new AArrayInitializer();

        initializer.setExp(ctx.arrayInitializer().init.stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList()));

        return initializer;
    }

    @Override
    public INode visitExpInit(MablParser.ExpInitContext ctx) {
        AExpInitializer init = new AExpInitializer();
        init.setExp((PExp) this.visit(ctx.expression()));
        return init;
    }


    @Override
    public INode visitLiteral(MablParser.LiteralContext ctx) {
        if (ctx.BOOL_LITERAL() != null) {
            ABoolLiteralExp literal = new ABoolLiteralExp();
            literal.setValue(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
            return literal;
        } else if (ctx.DECIMAL_LITERAL() != null) {
            AIntLiteralExp literal = new AIntLiteralExp();
            literal.setValue(Integer.parseInt(ctx.DECIMAL_LITERAL().getText()));
            return literal;
        } else if (ctx.FLOAT_LITERAL() != null) {
            ARealLiteralExp literal = new ARealLiteralExp();
            literal.setValue(Double.parseDouble(ctx.FLOAT_LITERAL().getText()));
            return literal;

        } else if (ctx.STRING_LITERAL() != null) {
            AStringLiteralExp literal = new AStringLiteralExp();
            //remove quotes
            literal.setValue((ctx.STRING_LITERAL().getText().substring(1, ctx.STRING_LITERAL().getText().length() - 1)));
            return literal;
        } else if (ctx.NULL_LITERAL() != null) {
            ANullExp literal = new ANullExp();
            //remove quotes
            literal.setToken(convertToLexToken(ctx.NULL_LITERAL().getSymbol()));
            return literal;
        }
        throw new RuntimeException("unsupported literal");
    }

    @Override
    public INode visitExpandMapping(MablParser.ExpandMappingContext ctx) {
        return new AInstanceMappingStm(convert(ctx.identifier), ctx.name.getText().substring(1, ctx.name.getText().length() - 1));
    }

    @Override
    public INode visitUnknownType(MablParser.UnknownTypeContext ctx) {
        return new AUnknownType();
    }


    @Override
    public INode visitBoolType(MablParser.BoolTypeContext ctx) {
        return new ABooleanPrimitiveType();
    }

    @Override
    public INode visitRealType(MablParser.RealTypeContext ctx) {
        return new ARealNumericPrimitiveType();
    }

    @Override
    public INode visitUintType(MablParser.UintTypeContext ctx) {
        return new AUIntNumericPrimitiveType();
    }

    @Override
    public INode visitIntType(MablParser.IntTypeContext ctx) {
        return new AIntNumericPrimitiveType();
    }

    @Override
    public INode visitStringType(MablParser.StringTypeContext ctx) {
        return new AStringPrimitiveType();
    }

    @Override
    public INode visitVoidType(MablParser.VoidTypeContext ctx) {
        return new AVoidType();
    }

    private LexIdentifier convert(Token identifier) {
        return new LexIdentifier(identifier.getText(), convertToLexToken(identifier));
    }

    private LexIdentifier convert(TerminalNode identifier) {
        return new LexIdentifier(identifier.getText(), convertToLexToken(identifier.getSymbol()));
    }


    @Override
    public INode visitArrayTypeType(MablParser.ArrayTypeTypeContext ctx) {
        PType type = (PType) this.visit(ctx.type);
        for (int i = 0; i < ctx.dimentions.size(); i++) {
            type = new AArrayType(type);
        }
        return type;
    }

    @Override
    public INode visitIdentifierTypeType(MablParser.IdentifierTypeTypeContext ctx) {
        return new ANameType(convert(ctx.type));
    }

    @Override
    public INode visitPrimitiveTypeType(MablParser.PrimitiveTypeTypeContext ctx) {
        return this.visit(ctx.type);
    }

    @Override
    public INode visitRefExpression(MablParser.RefExpressionContext ctx) {
        ARefExp exp = new ARefExp();
        exp.setExp((PExp) this.visit(ctx.expression()));
        return exp;
    }

    @Override
    public INode visitAndmedOrPrimitiveTypeType(MablParser.AndmedOrPrimitiveTypeTypeContext ctx) {
        return visit(ctx.type);
    }
}
