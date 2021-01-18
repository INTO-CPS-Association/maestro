package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.Collections;
import java.util.List;

public class ArrayVariable<T> extends AMablVariable<Fmi2Builder.NamedVariable<PStm>> {
    private final List<AMablVariable<T>> items;

    public ArrayVariable(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp, List<AMablVariable<T>> items) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.items = Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public List<AMablVariable<T>> items() {
        return items;
    }
}
