package org.intocps.maestro.optimization;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.PExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Remove an import IFF:
// It has NO associated load call OR
// It's associated load call is only used for: standalone null checks, null assignment or unload().
public class UnusedImportsOptimizer extends DepthFirstAnalysisAdaptor {
    private final Set<String> usedImports = new HashSet<>();
    private List<? extends LexIdentifier> imports;

    @Override
    public void caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node) throws AnalysisException {
        imports = node.getImports();
        super.caseASimulationSpecificationCompilationUnit(node);
        node.setImports(usedImports.stream().map(x -> new LexIdentifier(x, null)).collect(Collectors.toList()));

    }

    @Override
    public void caseALoadExp(ALoadExp node) throws AnalysisException {
        // Get load name, which has to be a string.
        if (node.getArgs().size() > 0) {
            PExp loadName = node.getArgs().get(0);
            AStringLiteralExp loadNameString;
            if (loadName instanceof AStringLiteralExp) {
                loadNameString = (AStringLiteralExp) loadName;
                usedImports.add(loadNameString.getValue());
            }
        }
    }
}
