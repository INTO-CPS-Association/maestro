package org.intocps.maestro.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;

import javax.xml.xpath.XPathExpressionException;

public class TemplateGenerator {
    public static ASimulationSpecificationCompilationUnit generateTemplate(ScenarioConfiguration configuration){
        try {
            return TemplateGeneratorFromScenario.generateTemplate(configuration);
        } catch (AnalysisException e) {
            throw new RuntimeException("Unable to generate template");
        }
    }

    public static ASimulationSpecificationCompilationUnit generateTemplate(MaBLTemplateConfiguration configuration){
        try {
            return MaBLTemplateGenerator.generateTemplate(configuration);
        } catch (XPathExpressionException | JsonProcessingException e) {
            throw new RuntimeException("Unable to generate template");
        }
    }
}
