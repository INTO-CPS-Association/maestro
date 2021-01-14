package org.intocps.maestro.framework.fmi2.api.mabl.statements;

import org.intocps.maestro.ast.node.PStm;

import java.util.List;
import java.util.stream.Collectors;

public class AMaBLStatementFactory {
    public static SingleStatement createSingleStatement(PStm stm) {
        return new SingleStatement(stm);
    }

    public static List<SingleStatement> createSingleStatements() {
        return createSingleStatements();
    }

    public static List<SingleStatement> createSingleStatements(List<PStm> stms) {
        return stms.stream().map(x -> createSingleStatement(x)).collect(Collectors.toList());
    }
}
