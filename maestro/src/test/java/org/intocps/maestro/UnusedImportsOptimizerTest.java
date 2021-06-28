package org.intocps.maestro;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.optimization.UnusedImportsOptimizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class UnusedImportsOptimizerTest {

    @Test
    public void Test1() throws AnalysisException {
        ASimulationSpecificationCompilationUnit simSpecCompUnit =
                newASimulationSpecificationCompilationUnit(List.of(newAIdentifier("Test")), newABlockStm());
        UnusedImportsOptimizer unusedImportsOptimizer = new UnusedImportsOptimizer();
        unusedImportsOptimizer.caseASimulationSpecificationCompilationUnit(simSpecCompUnit);
        Assertions.assertTrue(simSpecCompUnit.getImports().size() == 0);
    }
}
