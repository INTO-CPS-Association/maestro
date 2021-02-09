package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.StringExpressionValue;

public class Fmi2ApiStaticCreator {

    public static DoubleExpressionValue of(double v) {
        return DoubleExpressionValue.of(v);
    }

    public static IntExpressionValue of(int v) {
        return IntExpressionValue.of(v);
    }

    public static BooleanExpressionValue of(boolean v) {
        return BooleanExpressionValue.of(v);
    }

    public static StringExpressionValue of(String v) {
        return StringExpressionValue.of(v);
    }
}
