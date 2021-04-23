package org.intocps.maestro.parser;

import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.INode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assertions;

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


public class BasicTests {

    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "basic_tests").toFile().listFiles()))
                .map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(String name, File source) throws IOException, AnalysisException {

        System.out.println("############################## Source #################################\n\n");
        System.out.println(Files.readString(source.toPath()));

        MablLexer l = new MablLexer(CharStreams.fromPath(source.toPath(), StandardCharsets.UTF_8));
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
        Assertions.assertEquals(0, errorCount.get(), "No errors should exist");


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
            Assertions.assertEquals(0, errorCountPp.get(), "No errors should exist");
        } catch (IllegalStateException e) {
            System.err.println(ppRoot);
            throw e;
        }
    }
}
