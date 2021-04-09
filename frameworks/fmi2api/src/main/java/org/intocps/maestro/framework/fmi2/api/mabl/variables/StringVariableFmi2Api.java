package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.StringExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.newARealNumericPrimitiveType;

public class StringVariableFmi2Api extends VariableFmi2Api<StringExpressionValue> implements Fmi2Builder.StringVariable<PStm> {
    public StringVariableFmi2Api(PStm declaration, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }

    @Override
    public StringVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new StringVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
