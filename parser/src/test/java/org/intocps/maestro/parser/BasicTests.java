package org.intocps.maestro.parser;


import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.INode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class BasicTests {

    final File source;
    private final String name;

    public BasicTests(String name, File source) {
        this.name = name;
        this.source = source;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "basic_tests").toFile().listFiles()))
                .map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }


    @Test
    public void test() throws IOException {
        MablLexer l = new MablLexer(new ANTLRInputStream(new FileInputStream(this.source)));
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
