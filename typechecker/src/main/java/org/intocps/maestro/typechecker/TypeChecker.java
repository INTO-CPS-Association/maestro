package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker {

    final IErrorReporter errorReporter;
    Map<INode, PType> checkedTypes = new HashMap<>();

    public TypeChecker(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    public Map<INode, PType> getCheckedTypes() {
        return checkedTypes;
    }

    public boolean typeCheck(List<ARootDocument> documents, List<? extends PDeclaration> globalFunctions) throws AnalysisException {
        TypeCheckVisitor checker = new TypeCheckVisitor(errorReporter);
        checkedTypes.clear();
        checker.typecheck(documents, globalFunctions);
        checkedTypes.putAll(checker.checkedTypes);
        return errorReporter.getErrorCount() == 0;
    }
}
