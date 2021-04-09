package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.fmi.ModelDescription;

import java.net.URI;
import java.util.Collection;

public interface IMablScope extends Fmi2Builder.Scope<PStm> {


    String getName(String prefix);


    @Override
    public BooleanVariableFmi2Api store(boolean value);

    @Override
    WhileMaBLScope enterWhile(Fmi2Builder.Predicate predicate);

    @Override
    IfMaBlScope enterIf(Fmi2Builder.Predicate predicate);

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
    StringVariableFmi2Api store(String value);

    @Override
    DoubleVariableFmi2Api store(String name, double value);

    @Override
    BooleanVariableFmi2Api store(String name, boolean value);

    @Override
    IntVariableFmi2Api store(String name, int value);

    @Override
    StringVariableFmi2Api store(String name, String value);

    @Override
    <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag);

    IntVariableFmi2Api store(String stabilisation_loop, IntVariableFmi2Api stabilisation_loop_max_iterations);

    FmuVariableFmi2Api createFMU(String name, ModelDescription modelDescription, URI path) throws Exception;

    @Override
    FmuVariableFmi2Api createFMU(String name, String loaderName, String... args) throws Exception;

    VariableFmi2Api copy(String var1, VariableFmi2Api bool);

    //    @Override
    //    <ValType extends Object, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>, RetVar extends VariableFmi2Api<Val>> RetVar copy(
    //            String name, Var value);

}
