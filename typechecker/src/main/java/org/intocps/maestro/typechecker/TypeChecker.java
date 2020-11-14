package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.typechecker.context.Context;

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

    public boolean typeCheck(List<ARootDocument> documents, Context rootContext) throws AnalysisException {
        TypeCheckVisitor checker = new TypeCheckVisitor(errorReporter);
        checkedTypes.clear();
        checker.typecheck(documents);
        checkedTypes.putAll(checker.checkedTypes);
        return errorReporter.getErrorCount() == 0;
    }
}
