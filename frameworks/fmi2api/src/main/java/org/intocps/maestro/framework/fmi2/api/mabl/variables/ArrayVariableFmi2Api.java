package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.Collections;
import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.newAArayStateDesignator;
import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;

public class ArrayVariableFmi2Api<T> extends VariableFmi2Api<FmiBuilder.NamedVariable<PStm>> implements FmiBuilder.ArrayVariable<PStm, T> {
    private final List<VariableFmi2Api<T>> items;

    public ArrayVariableFmi2Api(PStm declaration, PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp, List<VariableFmi2Api<T>> items) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.items = Collections.unmodifiableList(items);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public List<VariableFmi2Api<T>> items() {
        return items;
    }

    @Override
    public void setValue(FmiBuilder.IntExpressionValue index, FmiBuilder.ExpressionValue value) {
        AAssigmentStm stm = newAAssignmentStm(newAArayStateDesignator(this.getDesignator().clone(), index.getExp().clone()), value.getExp());
        this.dynamicScope.add(stm);
    }
}
