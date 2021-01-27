package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.net.URI;
import java.util.Collection;

public class DynamicActiveBuilderScope implements IMablScope, Fmi2Builder.DynamicActiveScope<PStm> {

    final private IMablScope root;
    private IMablScope activeScope;

    public DynamicActiveBuilderScope(ScopeFmi2Api root) {
        this.root = root;
        this.activeScope = root;
    }

    public IMablScope getActiveScope() {
        return activeScope;
    }

    public IMablScope activate(ScopeFmi2Api activeScope) {
        this.activeScope = activeScope;
        return this.activeScope;
    }

    Fmi2Builder.Scope<PStm> getRootScope() {
        return root;
    }


    @Override
    public WhileMaBLScope enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return activeScope.enterWhile(predicate);
    }

    @Override
    public IfMaBlScope enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return activeScope.enterIf(predicate);
    }

    @Override
    public ScopeFmi2Api leave() {
        return activeScope.leave();
    }


    @Override
    public String getName(String prefix) {
        return activeScope.getName(prefix);
    }

    @Override
    public BooleanVariableFmi2Api store(boolean value) {
        return activeScope.store(value);
    }

    @Override
    public void add(PStm... commands) {
        activeScope.add(commands);
    }

    @Override
    public void addAll(Collection<PStm> commands) {
        activeScope.addAll(commands);
    }

    @Override
    public void addBefore(PStm item, PStm... commands) {
        this.activeScope.addBefore(item, commands);
    }

    @Override
    public void addAfter(PStm item, PStm... commands) {
        this.activeScope.addAfter(item, commands);
    }

    @Override
    public IMablScope activate() {
        return activeScope;
    }

    @Override
    public DoubleVariableFmi2Api store(double value) {
        return activeScope.store(value);
    }

    @Override
    public IntVariableFmi2Api store(int value) {
        return activeScope.store(value);

    }

    @Override
    public DoubleVariableFmi2Api store(String name, double value) {
        return activeScope.store(name, value);

    }

    @Override
    public BooleanVariableFmi2Api store(String name, boolean value) {
        return activeScope.store(name, value);

    }

    @Override
    public IntVariableFmi2Api store(String name, int value) {
        return activeScope.store(name, value);
    }

    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var store(String name, Var value) {
        return activeScope.store(name, value);
    }

    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var copy(String name, Var value) {
        return activeScope.copy(name, value);
    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag) {
        return activeScope.store(tag);
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, URI path) throws Exception {
        return activeScope.createFMU(name, path);
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, ModelDescription modelDescription, URI path) throws Exception {
        return activeScope.createFMU(name, modelDescription, path);
    }


}
