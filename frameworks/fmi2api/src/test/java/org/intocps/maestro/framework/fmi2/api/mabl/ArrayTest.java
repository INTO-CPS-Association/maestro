package org.intocps.maestro.framework.fmi2.api.mabl;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;

public class ArrayTest extends BaseApiTest{
    @Test
    public void mdArrayFaultyAssignmentTest() throws Exception {
        //Arrange
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
        IErrorReporter reporter = new ErrorReporter();
        TypeChecker typeChecker = new TypeChecker(reporter);
        List<AImportedModuleCompilationUnit> maestro2EmbeddedModules =
                getModuleDocuments(TypeChecker.getRuntimeModules()).stream().map(x -> NodeCollector.collect(x, AImportedModuleCompilationUnit.class))
                        .filter(Optional::isPresent).flatMap(x -> x.get().stream()).collect(Collectors.toList());
        ARootDocument defaultModules = new ARootDocument();
        defaultModules.setContent(maestro2EmbeddedModules);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        // Setup MaBL
        Double[][] mdArrayVals = {{0.0, 0.0}, {0.0, 0.0}, {0.0, 0.0}};
        ArrayVariableFmi2Api<Double[]> multiDimensionalArray = dynamicScope.store("multiDimensionalArray", mdArrayVals);
        DoubleVariableFmi2Api realVar = dynamicScope.store("realVar", 1.1);
        PExp val = realVar.getReferenceExp().clone();

        // Make faulty assignment of sharedDataDerivatives[0] = realVar
        builder.getDynamicScope().add(newAAssignmentStm(multiDimensionalArray.items().get(0).getDesignatorClone(), val));
        String spec = PrettyPrinter.print(builder.build());

        //Act
        ARootDocument doc = MablParserUtil.parse(CharStreams.fromStream(new ByteArrayInputStream(spec.getBytes())));
        boolean res = typeChecker.typeCheck(List.of(doc, defaultModules), new Vector<>());

        if (!res) {
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
        }

        //Assert
        Assertions.assertFalse(res, "Expected error: Invalid assignment to cannot assign: 'real' to 'real[]' in: multidimensionalarray[0] = " +
                "realvar; null");
        Assertions.assertThrows(RuntimeException.class,
                () -> new MableInterpreter(new DefaultExternalValueFactory(new File("target"), null)).execute(doc));

    }
}
