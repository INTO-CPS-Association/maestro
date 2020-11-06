package org.intocps.maestro.parser;

import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.node.INode;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class MablParserTest {

    @Before
    public void before() {
        System.out.println("\n\n###########################################################################\n\n");
    }

    @Test
    public void simplifiedTest() throws IOException {
        MablLexer l = new MablLexer(new ANTLRInputStream(getClass().getResourceAsStream("/m1-simplified.mabl")));
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        INode root = new ParseTree2AstConverter().visit(unit);
        System.out.println(root);

    }


    @Test
    public void jacobianTest() throws IOException {
        MablLexer l = new MablLexer(new ANTLRInputStream(getClass().getResourceAsStream("/jacobian.mabl")));
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        INode root = new ParseTree2AstConverter().visit(unit);
        System.out.println(root);

    }
}


