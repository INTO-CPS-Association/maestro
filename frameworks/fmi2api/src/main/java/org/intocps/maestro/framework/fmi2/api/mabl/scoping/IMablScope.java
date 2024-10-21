package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public interface IMablScope extends FmiBuilder.Scope<PStm> {

    @Override
    FmiBuilder.ScopeElement<PStm> parent();

    IntVariableFmi2Api getFmiStatusVariable();

    String getName(String prefix);

    @Override
    IMablScope enterScope();

    @Override
    TryMaBlScope enterTry();

    @Override
    BooleanVariableFmi2Api store(boolean value);

    @Override
    WhileMaBLScope enterWhile(FmiBuilder.Predicate predicate);

    @Override
    IfMaBlScope enterIf(FmiBuilder.Predicate predicate);

    @Override
    IMablScope parallel();

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

    void addAfterOrTop(PStm item, PStm... commands);

    int indexOf(PStm stm);


    @Override
    IMablScope activate();

    @Override
    DoubleVariableFmi2Api store(double value);
    @Override
    FloatVariableFmi2Api store(float value);

    @Override
    IntVariableFmi2Api store(int value);

    @Override
    UIntVariableFmi2Api storeUInt(long value);

    @Override
    StringVariableFmi2Api store(String value);

    @Override
    DoubleVariableFmi2Api store(String name, double value);

    @Override
    FloatVariableFmi2Api store(String name, float value);

    @Override
    BooleanVariableFmi2Api store(String name, boolean value);

    @Override
    IntVariableFmi2Api store(String name, int value);

    @Override
    UIntVariableFmi2Api storeUInt(String name, long value);

    @Override
    StringVariableFmi2Api store(String name, String value);

    @Override
    <V> ArrayVariableFmi2Api<V> store(String name, V value[]);

    @Override
 <V > ArrayVariableFmi2Api<V> createArray(String name,Class<? extends V> type, FmiBuilder.IntVariable<PStm>... sizes ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;
    <V > ArrayVariableFmi2Api<V> createArray(String name,Class<? extends V> type, FmiBuilder.UIntVariable<PStm>... sizes ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    @Override
    <V> FmiBuilder.Variable<PStm, V> store(FmiBuilder.Value<V> tag);

    IntVariableFmi2Api store(String stabilisation_loop, IntVariableFmi2Api stabilisation_loop_max_iterations);

    DoubleVariableFmi2Api store(String namePrefix, DoubleVariableFmi2Api variable);

    FloatVariableFmi2Api store(String namePrefix, FloatVariableFmi2Api variable);
    ArrayVariableFmi2Api storeInArray(String name, VariableFmi2Api[] variables);


    FmuVariableFmi2Api createFMU(String name, Fmi2ModelDescription modelDescription, URI path) throws Exception;

    @Override
    FmuVariableFmi2Api createFMU(String name, String loaderName, String... args) throws Exception;

    FmuVariableFmi3Api createFMU(String name, Fmi3ModelDescription modelDescription, URI path) throws Exception;

    FmuVariableFmi3Api createFMU3(String name, String loaderName, String... args) throws Exception;

    <Var extends VariableFmi2Api> Var copy(String name, Var variable);

    /**
     * Retrieves a set of allComponentFmi2Variables including those of parents
     *
     * @return
     */
    Set<ComponentVariableFmi2Api> getAllComponentFmi2Variables();

    /**
     * This is used to maintain a register of stored ComponentVariableFmi2API, such that they can be freed in case of an error.
     */
    void registerComponentVariableFmi2Api(ComponentVariableFmi2Api componentVariableFmi2Api);

    void registerInstanceVariableFmi3Api(InstanceVariableFmi3Api instanceVariableFmi3Api);


    <S> S findParentScope(Class<S> type);
}
