package org.intocps.maestro.parser;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intocps.maestro.ast.*;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class ParseTree2AstConverter extends MablParserBaseVisitor<INode> {

    @Override
    public INode visitCompilationUnit(MablParser.CompilationUnitContext ctx) {

        ARootDocument doc = new ARootDocument();

        List<PCompilationUnit> list = ctx.moduleDeclaration().stream().map(this::visit).map(PCompilationUnit.class::cast)
                .collect(Collectors.toCollection(Vector::new));

        if (ctx.simulationSpecification() != null) {
            list.add((PCompilationUnit) this.visit(ctx.simulationSpecification()));
        }
        doc.setContent(list);
        return doc;
    }

    @Override
    public INode visitModuleDeclaration(MablParser.ModuleDeclarationContext ctx) {

        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();

        unit.setName(convert(ctx.IDENTIFIER()));

        unit.setFunctions(ctx.functionDeclaration().stream().map(this::visit).map(AFunctionDeclaration.class::cast).collect(Collectors.toList()));


        return unit;
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
        parameter.setType((PType) this.visit(ctx.typeType()));
        return parameter;
    }

    @Override
    public INode visitSimulationSpecification(MablParser.SimulationSpecificationContext ctx) {

        ASimulationSpecificationCompilationUnit unit = new ASimulationSpecificationCompilationUnit();

        unit.setBody((PStm) this.visit(ctx.block()));

        if (ctx.imports != null && !ctx.imports.isEmpty()) {
            unit.setImports(ctx.imports.stream().map(c -> convert(c)).collect(Collectors.toList()));
        }

        return unit;

    }


    @Override
    public INode visitBlock(MablParser.BlockContext ctx) {
        ABlockStm block = new ABlockStm();

        List<INode> processedBody = ctx.statement().stream().filter(p -> !(p instanceof MablParser.SemiContext)).map(this::visit)
                .collect(Collectors.toList());

        processedBody.stream().filter(p -> !(p instanceof PStm)).forEach(s -> System.out.println("Wrong node type in body: " + s));

        List<PStm> statements = processedBody.stream().map(PStm.class::cast).collect(Collectors.toList());

        if (statements.stream().anyMatch(p -> p == null)) {
            System.out.println("found null");
        }

        block.setBody(statements);


        return block;

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
        designator.setExp((SLiteralExp) this.visit(ctx.literal()));
        return designator;
    }

    @Override
    public INode visitIdentifierStateDesignator(MablParser.IdentifierStateDesignatorContext ctx) {
        AIdentifierStateDesignator identifierExp = new AIdentifierStateDesignator();
        identifierExp.setName(this.convert(ctx.IDENTIFIER()));
        return identifierExp;
    }

    //    @Override
    //    public INode visitAssignmentStm(MablParser.AssignmentStmContext ctx) {
    //
    //        ctx.assignment()
    //
    //        AAssigmentStm assign = new AAssigmentStm();
    //
    //        AIdentifierExp identifierExp = new AIdentifierExp();
    //        identifierExp.setName(convert(ctx.assignment().IDENTIFIER()));
    //
    //        assign.setIdentifier(identifierExp.g);
    //        assign.setExp((PExp) this.visit(ctx.assignment().expression()));
    //
    //        return assign;
    //    }


    @Override
    public INode visitWhile(MablParser.WhileContext ctx) {

        AWhileStm stm = new AWhileStm();
        stm.setTest((PExp) this.visit(ctx.parExpression()));
        stm.setBody((PStm) this.visit(ctx.statement()));
        return stm;

    }


    @Override
    public INode visitArrayIndexExp(MablParser.ArrayIndexExpContext ctx) {

        AArrayIndexExp apply = new AArrayIndexExp();

        apply.setArray((PExp) this.visit(ctx.array));

        if (ctx.expression() != null) {
            apply.setIndices(ctx.expression().stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList()));
        }
        return apply;
    }

    @Override
    public INode visitObservable(MablParser.ObservableContext ctx) {
        return new AObservableStm();
    }

    @Override
    public INode visitLoadExp(MablParser.LoadExpContext ctx) {
        ALoadExp exp = new ALoadExp();
        exp.setArgs(ctx.expressionList().expression().stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList()));
        return exp;
    }

    @Override
    public INode visitUnloadExp(MablParser.UnloadExpContext ctx) {
        AUnloadExp exp = new AUnloadExp();

        AIdentifierExp identifierExp = new AIdentifierExp();
        identifierExp.setName(convert(ctx.IDENTIFIER()));

        exp.setArgs(Collections.singletonList(identifierExp));
        return exp;
    }

    @Override
    public INode visitParExpression(MablParser.ParExpressionContext ctx) {
        return this.visit(ctx.expression());
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

    @Override
    public INode visitMethodCall(MablParser.MethodCallContext ctx) {

        ACallExp call = new ACallExp();

        if (ctx.expressionList() != null && ctx.expressionList().expression() != null) {
            List<PExp> args = ctx.expressionList().expression().stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList());
            checkList(ctx.expressionList().expression(), args);
            call.setArgs(args);
        }

        AIdentifierExp ident = new AIdentifierExp();
        ident.setName(convert(ctx.IDENTIFIER()));

        call.setRoot(ident);
        return call;
    }

    @Override
    public INode visitDotExp(MablParser.DotExpContext ctx) {

        ADotExp exp = new ADotExp();
        exp.setRoot(convertToExp(ctx.IDENTIFIER(0)));
        if (ctx.methodCall() != null) {
            exp.setExp((PExp) this.visit(ctx.methodCall()));
        }
        if (ctx.IDENTIFIER(2) != null) {
            exp.setExp(convertToExp(ctx.IDENTIFIER(2)));
        }

        return exp;
    }

    @Override
    public INode visitIdentifierExp(MablParser.IdentifierExpContext ctx) {
        return this.convertToExp(ctx.IDENTIFIER());
    }

    @Override
    public INode visitExpressionStatement(MablParser.ExpressionStatementContext ctx) {
        AExpressionStm stm = new AExpressionStm();
        stm.setExp((PExp) this.visit(ctx.statementExpression));
        return stm;
    }

    @Override
    public INode visitMethodExternalCallStm(MablParser.MethodExternalCallStmContext ctx) {

        AExternalStm stm = new AExternalStm();
        stm.setCall((ACallExp) this.visit(ctx.methodCall()));
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

        def.setType((PType) this.visit(ctx.typeType()));
        def.setName(convert(ctx.variableDeclaratorId().IDENTIFIER()));
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
    //
    //        AVariableDeclaration def = new AVariableDeclaration();
    //
    //        def.setType((PType) this.visit(ctx.type));
    //        def.setName(convert(ctx.var.varid.IDENTIFIER()));
    //        MablParser.VariableInitializerContext initializer = ctx.var.initializer;
    //        if (initializer != null) {
    //            def.setInitializer((PInitializer) this.visit(initializer));
    //        }
    //
    //        ALocalVariableStm var = new ALocalVariableStm();
    //        var.setDeclaration(def);
    //        return var;
    //    }

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
            ARealLiteralExp literal = new ARealLiteralExp();
            literal.setValue(Double.parseDouble(ctx.DECIMAL_LITERAL().getText()));
            return literal;
        } else if (ctx.FLOAT_LITERAL() != null) {
            //            AUIntLiteralExp literal = new AUIntLiteralExp();
            //            literal.setValue(Long.parseLong(ctx.FLOAT_LITERAL().getText()));
            //            return literal;

            ARealLiteralExp literal = new ARealLiteralExp();
            literal.setValue(Double.parseDouble(ctx.FLOAT_LITERAL().getText()));
            return literal;

        } else if (ctx.STRING_LITERAL() != null) {
            AStringLiteralExp literal = new AStringLiteralExp();
            //remove quotes
            literal.setValue((ctx.STRING_LITERAL().getText().substring(1, ctx.STRING_LITERAL().getText().length() - 1)));
            return literal;
        }
        throw new RuntimeException("unsupported literal");
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
    public INode visitUnitType(MablParser.UnitTypeContext ctx) {
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

    private LexIdentifier convert(Token identifier) {
        return new LexIdentifier(identifier.getText(), identifier);
    }

    private LexIdentifier convert(TerminalNode identifier) {
        return new LexIdentifier(identifier.getText(), identifier.getSymbol());
    }

    private AIdentifierExp convertToExp(TerminalNode identifier) {
        AIdentifierExp exp = new AIdentifierExp();

        exp.setName(convert(identifier));

        return exp;
    }

    @Override
    public INode visitTypeType(MablParser.TypeTypeContext ctx) {
        PType type = ctx.primitiveType() == null ? null : (SPrimitiveType) super.visit(ctx.primitiveType());

        if (ctx.IDENTIFIER() != null) {
            ANameType nt = new ANameType();
            nt.setName(convert(ctx.IDENTIFIER()));
            type = nt;

        }

        if (ctx.arrays.isEmpty()) {
            return type;
        } else {
            AArrayType t = new AArrayType();
            t.setType(type);
            return t;
        }


    }
}
