package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import static org.intocps.maestro.ast.MableAstFactory.newAStringPrimitiveType;

public class StringVariableFmi2Api extends VariableFmi2Api<FmiBuilder.StringExpressionValue> implements FmiBuilder.StringVariable<PStm> {
    public StringVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newAStringPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }

    @Override
    public StringVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new StringVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
