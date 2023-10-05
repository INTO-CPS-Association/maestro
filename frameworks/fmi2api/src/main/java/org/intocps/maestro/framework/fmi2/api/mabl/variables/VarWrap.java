package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import static org.intocps.maestro.ast.MableAstFactory.*;

/**
 * Utility class to help wrap literals as variables to better reuse code
 * <p>
 * Note that the only method that is valid to call on these objects is getExp
 */
public class VarWrap {
    public static BooleanVariableFmi2Api wrap(boolean bool) {
        return new BooleanVariableFmi2Api(null, null, null, null, newABoolLiteralExp(bool));
    }

    public static DoubleVariableFmi2Api wrap(double v) {
        return new DoubleVariableFmi2Api(null, null, null, null, newARealLiteralExp(v));
    }

    public static IntVariableFmi2Api wrap(int v) {
        return new IntVariableFmi2Api(null, null, null, null, newAIntLiteralExp(v));
    }
}
