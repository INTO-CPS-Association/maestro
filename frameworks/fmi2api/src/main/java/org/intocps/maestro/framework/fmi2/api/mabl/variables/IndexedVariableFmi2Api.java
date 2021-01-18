package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

public interface IndexedVariableFmi2Api<V> extends Fmi2Builder.Variable<PStm, V> {

    PStm getDeclaringStm();
}
