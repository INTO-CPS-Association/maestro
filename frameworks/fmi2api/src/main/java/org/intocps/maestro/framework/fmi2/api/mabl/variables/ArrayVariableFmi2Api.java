package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;

import java.util.Collections;
import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.newAArayStateDesignator;
import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;

public class ArrayVariableFmi2Api<T> extends VariableFmi2Api<Fmi2Builder.NamedVariable<PStm>> {
    private final List<VariableFmi2Api<T>> items;

    public ArrayVariableFmi2Api(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp, List<VariableFmi2Api<T>> items) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.items = Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public void setValue(IntExpressionValue index, Fmi2Builder.ExpressionValue value){
        AAssigmentStm stm = newAAssignmentStm(newAArayStateDesignator(this.getDesignator(), index.getExp()), value.getExp());
    }

//    public Fmi2Builder.ExpressionValue getValue(IntExpressionValue index) {
//        return null;
//    }

    public List<VariableFmi2Api<T>> items() {
        return items;
    }
}
