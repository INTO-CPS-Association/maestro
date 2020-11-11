package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker extends DepthFirstAnalysisAdaptor {

    final static PType VOID_TYPE = new AVoidType();
    final IErrorReporter errorReporter;
    private final Map<AModuleType, ModuleEnvironment> modules = new HashMap<>();
    Map<INode, PType> types = new HashMap<>();
    TypeCheckVisitor typeCheckVisitor;
    TypeCheckInfo typeCheckInfo = new TypeCheckInfo();


    public TypeChecker(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        this.typeCheckInfo.addModules(modules);
        typeCheckInfo.addEnvironment(new CTMEnvironment(null));
        this.typeCheckVisitor = new TypeCheckVisitor(this.errorReporter);

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
        super.caseASimulationSpecificationCompilationUnit(node);
    }

    @Override
    public void caseABlockStm(ABlockStm node) throws AnalysisException {
        super.caseABlockStm(node);
    }

    @Override
    public void caseAVariableDeclaration(AVariableDeclaration node) throws AnalysisException {
        PType nodeType = node.apply(typeCheckVisitor, typeCheckInfo);
        this.typeCheckInfo.getCtmEnvironment().addVariable(node.getName(), nodeType);

        // Add the variable and its type to environment.
    }

    @Override
    public void defaultOutPStm(PStm node) throws AnalysisException {
        types.put(node, VOID_TYPE);
    }

    @Override
    public void caseAImportedModuleCompilationUnit(AImportedModuleCompilationUnit node) throws AnalysisException {
        AModuleType moduleType = new AModuleType();
        moduleType.setName(MableAstFactory.newAIdentifierExp(node.getName()));
        ModuleEnvironment env = new ModuleEnvironment(null, node.getFunctions());
        modules.put(moduleType, env);
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
