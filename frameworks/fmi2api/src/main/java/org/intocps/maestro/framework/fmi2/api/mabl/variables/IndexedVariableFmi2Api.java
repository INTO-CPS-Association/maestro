package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

public interface IndexedVariableFmi2Api<V> extends FmiBuilder.Variable<PStm, V> {

    PStm getDeclaringStm();
}
