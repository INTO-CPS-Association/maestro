package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;

public class VariableUtil {
    static public PExp getAsExp(VariableFmi2Api var) {
        return var.getReferenceExp();
    }
}
