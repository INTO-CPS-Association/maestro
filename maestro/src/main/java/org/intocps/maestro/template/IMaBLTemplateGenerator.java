package org.intocps.maestro.template;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;

public interface IMaBLTemplateGenerator {
    ASimulationSpecificationCompilationUnit generateTemplate() throws AnalysisException;
}
