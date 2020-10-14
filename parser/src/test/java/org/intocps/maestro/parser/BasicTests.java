package org.intocps.maestro.parser;


import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.INode;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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
    public void test() throws IOException, AnalysisException {

        System.out.println("############################## Source #################################\n\n");
        System.out.println(Files.readString(this.source.toPath()));

        MablLexer l = new MablLexer(CharStreams.fromPath(this.source.toPath(), StandardCharsets.UTF_8));
        MablParser p = new MablParser(new CommonTokenStream(l));
        AtomicInteger errorCount = new AtomicInteger(0);
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                errorCount.addAndGet(1);
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        INode root = new ParseTree2AstConverter().visit(unit);
        System.out.println("############################## Parsed to string #################################\n\n");
        System.out.println(root);
        Assert.assertEquals("No errors should exist", 0, errorCount.get());


        String ppRoot = PrettyPrinter.print(root);

        System.out.println("############################## PrettyPrint #################################\n\n");
        System.out.println(ppRoot);

        System.out.println("############################## PrettyPrint parsed #################################\n\n");
        l = new MablLexer(CharStreams.fromString(ppRoot));
        p = new MablParser(new CommonTokenStream(l));
        AtomicInteger errorCountPp = new AtomicInteger(0);
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                errorCountPp.addAndGet(1);
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        try {
            unit = p.compilationUnit();

            root = new ParseTree2AstConverter().visit(unit);
            System.out.println(root);
            Assert.assertEquals("No errors should exist", 0, errorCountPp.get());
        } catch (IllegalStateException e) {
            System.err.println(ppRoot);
            throw e;
        }
    }
}
