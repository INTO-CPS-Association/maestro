package org.intocps.maestro.typechecker;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

public class ModuleTest {
    public static List<ARootDocument> getModuleDocuments(List<String> modules) throws IOException {
        List<String> allModules = TypeChecker.getRuntimeModules();
        List<ARootDocument> documents = new ArrayList<>();
        if (modules != null) {
            for (String module : modules) {
                if (allModules.contains(module)) {
                    documents.add(getRuntimeModule(module));
                }
            }
        }
        return documents;
    }

    public static ARootDocument getRuntimeModule(String module) throws IOException {
        InputStream resourceAsStream = TypeChecker.getRuntimeModule(module);
        if (resourceAsStream == null) {
            return null;
        }
        return MablParserUtil.parse(CharStreams.fromStream(resourceAsStream));
    }

    @Test
    public void testAll() throws IOException, AnalysisException {
        IErrorReporter reporter = new ErrorReporter();
        TypeChecker typeChecker = new TypeChecker(reporter);
        List<AImportedModuleCompilationUnit> maestro2EmbeddedModules =
                getModuleDocuments(TypeChecker.getRuntimeModules()).stream().map(x -> NodeCollector.collect(x, AImportedModuleCompilationUnit.class))
                        .filter(Optional::isPresent).flatMap(x -> x.get().stream()).collect(Collectors.toList());
        ARootDocument defaultModules = new ARootDocument();
        defaultModules.setContent(maestro2EmbeddedModules);
        PrintWriter writer = new PrintWriter(System.err);

        ARootDocument doc = MablParserUtil.parse(CharStreams.fromStream(new ByteArrayInputStream("simulation{}".getBytes())));
        boolean res = typeChecker.typeCheck(List.of(doc, defaultModules), new Vector<>());

        if (!res) {
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
        }
        writer.flush();
        Assertions.assertTrue(res);
    }
}
