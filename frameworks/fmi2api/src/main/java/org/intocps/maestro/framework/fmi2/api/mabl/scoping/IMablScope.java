package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableCreatorFmi2Api;

import java.util.Collection;

public interface IMablScope extends Fmi2Builder.Scope<PStm> {


    @Override
    VariableCreatorFmi2Api getVariableCreator();

    String getName(String prefix);


    @Override
    public BooleanVariableFmi2Api store(boolean value);

    @Override
    ScopeFmi2Api enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate);

    @Override
    IfMaBlScope enterIf(Fmi2Builder.LogicBuilder.Predicate predicate);

    @Override
    ScopeFmi2Api leave();

    @Override
    void add(PStm... commands);

    @Override
    void addAll(Collection<PStm> commands);

    @Override
    void addBefore(PStm item, PStm... commands);

    @Override
    void addAfter(PStm item, PStm... commands);

    @Override
    IMablScope activate();

    @Override
    DoubleVariableFmi2Api store(double value);

    @Override
    IntVariableFmi2Api store(int value);

    @Override
    DoubleVariableFmi2Api store(String name, double value);

    @Override
    BooleanVariableFmi2Api store(String name, boolean value);

    @Override
    IntVariableFmi2Api store(String name, int value);

    @Override
    <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag);

    IntVariableFmi2Api store(String stabilisation_loop, IntVariableFmi2Api stabilisation_loop_max_iterations);
}
