package org.intocps.maestro.typechecker;

import org.antlr.v4.runtime.*;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.LexToken;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.parser.MablParser;
import org.intocps.maestro.parser.ParseTree2AstConverter;
import org.intocps.maestro.typechecker.context.LocalContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class StatementsTcTest {

    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "single_file").toFile().listFiles()))
                .filter(f -> f.getName().equals("statements.mabl")).map(f -> {

                    try {
                        return FileUtils.readLines(f, StandardCharsets.UTF_8).stream().filter(l -> !l.trim().isEmpty() && !l.trim().startsWith("//"))
                                .map(spec -> new Object[]{f.getName(), spec});
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new Vector<Object[]>().stream();
                    }

                }).flatMap(Function.identity()).collect(Collectors.toList());
    }

    public static PStm parseExp(CharStream specStreams, IErrorReporter reporter) {
        MablLexer l = new MablLexer(specStreams);
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                //throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
                reporter.report(0, msg, new LexToken("", line, 0));
            }
        });
        return (PStm) new ParseTree2AstConverter().visit(p.statement());

    }

    public static PType parseType(CharStream specStreams, IErrorReporter reporter) {
        MablLexer l = new MablLexer(specStreams);
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                //throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
                reporter.report(0, msg, new LexToken("", line, 0));
            }
        });
        return (PType) new ParseTree2AstConverter().visit(p.typeType());
    }

    @ParameterizedTest(name = "{index} \"{1}\" in {0}")
    @MethodSource("data")
    public void test(String name, String spec) throws IOException, AnalysisException {

        //        String template =
        //                FileUtils.readFileToString(Paths.get("src", "test", "resources", "statements", "template.mabl").toFile(), StandardCharsets.UTF_8);

        //  String testString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        //        template = template.replace("TEMPLATE", testString);

        int split = spec.indexOf(",");
        String expectedTypeString = spec.substring(0, split);
        String testExp = spec.substring(split + 1);


        IErrorReporter errorReporter = new ErrorReporter();
        //        ARootDocument doc = MablParserUtil.parse(CharStreams.fromString(template), errorReporter);
        PStm doc = parseExp(CharStreams.fromString(testExp), errorReporter);
        PType expectedType =
                expectedTypeString.equalsIgnoreCase("error") ? null : parseType(CharStreams.fromString(expectedTypeString), errorReporter);
        PrettyPrinter.printLineNumbers(doc);
        TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor(errorReporter);

        DeclarationList defs = new DeclarationList();
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("i"), MableAstFactory.newAIntNumericPrimitiveType()));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("r"), MableAstFactory.newARealNumericPrimitiveType()));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("b"), MableAstFactory.newABoleanPrimitiveType()));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("s"), MableAstFactory.newAStringPrimitiveType()));


        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("ia"),
                MableAstFactory.newAArrayType(MableAstFactory.newAIntNumericPrimitiveType()), 1, null));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("ra"),
                MableAstFactory.newAArrayType(MableAstFactory.newARealNumericPrimitiveType()), 1, null));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("be"),
                MableAstFactory.newAArrayType(MableAstFactory.newABoleanPrimitiveType()), 1, null));
        defs.add(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier("sa"),
                MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), 1, null));

        defs.add(MableAstFactory.newAFunctionDeclaration(MableAstFactory.newAIdentifier("IcallI"), Collections.singletonList(
                MableAstFactory.newAFormalParameter(MableAstFactory.newAIntNumericPrimitiveType(), MableAstFactory.newAIdentifier("a"))),
                MableAstFactory.newAIntNumericPrimitiveType()));

        for (PDeclaration def : defs) {
            def.apply(typeCheckVisitor, new LocalContext(defs, null));
        }

        PType type = doc.apply(typeCheckVisitor, new LocalContext(defs, null));
        errorReporter.printErrors(new PrintWriter(System.out, true));
        errorReporter.printWarnings(new PrintWriter(System.out, true));

        if (expectedType != null) {
            boolean compatible = new TypeComparator().compatible(expectedType, type);
            if (!compatible) {
                System.err.println(doc);
                System.err.println(PrettyPrinter.print(doc));
                System.err.flush();
            }
            Assertions.assertTrue(compatible, "Type mismatch. Type '" + type + "' is not compatible with expected: '" + expectedType + "'");
            Assertions.assertEquals(0, errorReporter.getErrorCount());
        } else {
            //            Assert.assertFalse(errorReporter.getErrorCount() == 0);
        }
        System.out.flush();

    }
}
