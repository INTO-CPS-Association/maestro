package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker extends DepthFirstAnalysisAdaptor {

    final static PType VOID_TYPE = new AVoidType();
    final IErrorReporter errorReporter;
    private final Map<AModuleType, List<PType>> modules = new HashMap<>();
    Map<INode, PType> types = new HashMap<>();


    public TypeChecker(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Override
    public void caseABreakStm(ABreakStm node) throws AnalysisException {

        if (node.getAncestor(AWhileStm.class) == null) {
            errorReporter.report(0, "Break must be enclosed in a while statement", node.getToken());
        }
        types.put(node, VOID_TYPE);
    }

    @Override
    public void caseARootDocument(ARootDocument node) throws AnalysisException {
        super.caseARootDocument(node);
    }

    @Override
    public void caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node) throws AnalysisException {
        //super.caseASimulationSpecificationCompilationUnit(node);

    }


    @Override
    public void defaultOutPStm(PStm node) throws AnalysisException {
        types.put(node, VOID_TYPE);
    }

    @Override
    public void caseAImportedModuleCompilationUnit(AImportedModuleCompilationUnit node) throws AnalysisException {
        TypeCheckVisitor tc = new TypeCheckVisitor();
        AModuleType moduleType = new AModuleType();
        // TODO Thule: Should I clone here? Why is it an identifierExp, why not just a LexIdentifier?
        // TODO Thule: Why does moduleType not have member data?
        moduleType.setName(MableAstFactory.newAIdentifierExp(node.getName()));
        List<PType> functions = new ArrayList<>();
        for (AFunctionDeclaration function : node.getFunctions()) {
            functions.add(function.apply(tc));
        }
        modules.put(moduleType, functions);
    }

    public void addModules(List<ARootDocument> rootDocments) throws AnalysisException {
        for (ARootDocument rootDoc : rootDocments) {
            rootDoc.apply(this);
        }
    }

    public void typecheck(ARootDocument rootDocument) throws AnalysisException {
        rootDocument.apply(this);
    }

}
