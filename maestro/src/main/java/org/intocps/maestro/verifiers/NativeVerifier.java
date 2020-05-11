package org.intocps.maestro.verifiers;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.IMaestroVerifier;
import org.intocps.maestro.plugin.SimulationFramework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

@SimulationFramework(framework = Framework.Any)
public class NativeVerifier implements IMaestroVerifier {
    @Override
    public boolean verify(ARootDocument doc, IErrorReporter reporter) {

        LoadUnloadRelationAnalysis analysis = new LoadUnloadRelationAnalysis();

        try {
            doc.apply(analysis);

            analysis.loadsWithNoVarDecl.forEach(load -> reporter.report(0, "No variable found for load", null));
            analysis.getMissingUnloads().forEach((unload, name) -> reporter.report(0, "No unload found for loaded name: " + name, name.getSymbol()));
            analysis.getMissingLoads().forEach((unload, name) -> reporter.report(0, "No load found for unloaded name: " + name, name.getSymbol()));

        } catch (AnalysisException e) {
            throw new RuntimeException(getClass().getSimpleName() + " error processing analysis", e);
        }

        return analysis.getMissingLoads().isEmpty() && analysis.getMissingUnloads().isEmpty() && analysis.loadsWithNoVarDecl.isEmpty();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }

    class LoadUnloadRelationAnalysis extends DepthFirstAnalysisAdaptor {
        private final Map<LexIdentifier, ALoadExp> identifierToLoad = new HashMap<>();
        Map<ALoadExp, AUnloadExp> relation = new HashMap<>();
        List<ALoadExp> loadsWithNoVarDecl = new Vector<>();
        Map<AUnloadExp, LexIdentifier> unloadsWithNoLoad = new HashMap<>();
        AVariableDeclaration lastVisitedVarDecl;

        Map<ALoadExp, LexIdentifier> getMissingUnloads() {

            Map<ALoadExp, LexIdentifier> map = identifierToLoad.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

            relation.keySet().forEach(map::remove);

            return map;
        }

        Map<AUnloadExp, LexIdentifier> getMissingLoads() {
            return unloadsWithNoLoad;
        }

        @Override
        public void caseAVariableDeclaration(AVariableDeclaration node) throws AnalysisException {
            this.lastVisitedVarDecl = node;
            super.caseAVariableDeclaration(node);
        }

        @Override
        public void caseALoadExp(ALoadExp node) throws AnalysisException {

            if (lastVisitedVarDecl == null) {
                loadsWithNoVarDecl.add(node);
                return;
            }

            identifierToLoad.put(lastVisitedVarDecl.getName(), node);

            lastVisitedVarDecl = null;
        }

        @Override
        public void caseAUnloadExp(AUnloadExp node) throws AnalysisException {

            PExp arg0 = node.getArgs().get(0);

            if (arg0 instanceof AIdentifierExp) {
                LexIdentifier name = ((AIdentifierExp) arg0).getName();

                ALoadExp aLoadExp = identifierToLoad.get(name);
                if (aLoadExp == null) {
                    unloadsWithNoLoad.put(node, name);
                } else {
                    relation.put(aLoadExp, node);
                }
            }
        }
    }

}
