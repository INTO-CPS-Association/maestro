package org.intocps.maestro.parser.template;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.ParseTree2AstConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MablSwapConditionParserUtil {

    public static PExp parse(CharStream specStreams) {
        MablSwapConditionLexer l = new MablSwapConditionLexer(specStreams);
        MablSwapConditionParser p = new MablSwapConditionParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                System.out.println(specStreams);
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablSwapConditionParser.ExpressionContext unit = p.expression();

        PExp root = (PExp) new SwapConditionParseTree2AstConverter().visit(unit);
        return root;
    }

    public static PExp parse(CharStream specStreams, IErrorReporter reporter) {
        MablSwapConditionLexer l = new MablSwapConditionLexer(specStreams);
        MablSwapConditionParser p = new MablSwapConditionParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                //throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
                reporter.report(0, msg, new LexToken("", line, 0));
            }
        });
        MablSwapConditionParser.ExpressionContext unit = p.expression();

        PExp root = (PExp) new SwapConditionParseTree2AstConverter().visit(unit);
        return root;
    }

    static class SwapConditionParseTree2AstConverter extends MablSwapConditionParserBaseVisitor<INode> {
        //TODO implement me
        final static Logger logger = LoggerFactory.getLogger(ParseTree2AstConverter.class);


        @Override
        public INode visitBinaryExp(MablSwapConditionParser.BinaryExpContext ctx) {

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
        public INode visitParenExp(MablSwapConditionParser.ParenExpContext ctx) {
            return new AParExp((PExp) this.visit(ctx.expression()));
        }

        @Override
        public INode visitLiteralExp(MablSwapConditionParser.LiteralExpContext ctx) {
            return this.visit(ctx.literal());
        }

        @Override
        public INode visitDotPrefixExp(MablSwapConditionParser.DotPrefixExpContext ctx) {

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
        public INode visitPlainMetodExp(MablSwapConditionParser.PlainMetodExpContext ctx) {
            return this.visit(ctx.methodCall());
        }


        @Override
        public INode visitUnaryExp(MablSwapConditionParser.UnaryExpContext ctx) {
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
        public INode visitIdentifierExp(MablSwapConditionParser.IdentifierExpContext ctx) {
            return new AIdentifierExp(convert(ctx.IDENTIFIER()));
        }

        @Override

        public INode visitArrayIndex(MablSwapConditionParser.ArrayIndexContext ctx) {
            AArrayIndexExp apply = new AArrayIndexExp();

            apply.setArray((PExp) this.visit(ctx.array));

            if (ctx.index != null) {
                apply.setIndices(ctx.index.stream().map(e -> (PExp) this.visit(e)).collect(Collectors.toList()));
            }
            return apply;
        }


        @Override
        public INode visitParExpression(MablSwapConditionParser.ParExpressionContext ctx) {
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
        public INode visitMethodCall(MablSwapConditionParser.MethodCallContext ctx) {

            ACallExp call = new ACallExp();

            if (ctx.expressionList() != null && ctx.expressionList().expression() != null) {
                List<PExp> args = ctx.expressionList().expression().stream().map(this::visit).map(PExp.class::cast).collect(Collectors.toList());
                checkList(ctx.expressionList().expression(), args);
                call.setArgs(args);
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
        public INode visitLiteral(MablSwapConditionParser.LiteralContext ctx) {
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


        private LexIdentifier convert(Token identifier) {
            return new LexIdentifier(identifier.getText(), convertToLexToken(identifier));
        }

        private LexIdentifier convert(TerminalNode identifier) {
            return new LexIdentifier(identifier.getText(), convertToLexToken(identifier.getSymbol()));
        }


    }
}
