package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

public class DynamicActiveBuilderScope implements IMablScope, FmiBuilder.DynamicActiveScope<PStm> {

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

    FmiBuilder.Scope<PStm> getRootScope() {
        return root;
    }


    @Override
    public WhileMaBLScope enterWhile(FmiBuilder.Predicate predicate) {
        return activeScope.enterWhile(predicate);
    }

    @Override
    public IfMaBlScope enterIf(FmiBuilder.Predicate predicate) {
        return activeScope.enterIf(predicate);
    }

    @Override
    public IMablScope parallel() {
        return activeScope.parallel();
    }

    @Override
    public ScopeFmi2Api leave() {
        return activeScope.leave();
    }


    @Override
    public FmiBuilder.ScopeElement<PStm> parent() {
        return this.activeScope.parent();
    }

    @Override
    public PStm getDeclaration() {
        return this.activeScope.getDeclaration();
    }

    @Override
    public <P extends FmiBuilder.ScopeElement<PStm>> P findParent(Class<P> clz) {
        return this.activeScope.findParent(clz);
    }

    @Override
    public IntVariableFmi2Api getFmiStatusVariable() {
        return this.activeScope.getFmiStatusVariable();
    }

    @Override
    public String getName(String prefix) {
        return activeScope.getName(prefix);
    }

    @Override
    public IMablScope enterScope() {
        return this.activeScope.enterScope();
    }

    @Override
    public TryMaBlScope enterTry() {
        return this.activeScope.enterTry();
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
    public void addAfterOrTop(PStm item, PStm... commands) {
        this.activeScope.addAfterOrTop(item, commands);
    }

    @Override
    public int indexOf(PStm stm) {
        return this.activeScope.indexOf(stm);
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
    public StringVariableFmi2Api store(String value) {
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
    public StringVariableFmi2Api store(String name, String value) {
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

/*    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var store(String name, Var value) {
        return activeScope.store(name, value);
    }
    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var copy(String name, Var value) {
        return activeScope.copy(name, value);
    }
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var copy(String name, BooleanVariableFmi2Api value) {
        return activeScope.copy(name, value);
    }*/

    @Override
    public <V> ArrayVariableFmi2Api<V> store(String name, V value[]) {
        return activeScope.store(name, value);
    }

    @Override
    public <V> FmiBuilder.Variable<PStm, V> store(FmiBuilder.Value<V> tag) {
        return activeScope.store(tag);
    }

    @Override
    public IntVariableFmi2Api store(String namePrefix, IntVariableFmi2Api variable) {
        return activeScope.store(namePrefix, variable);
    }

    @Override
    public DoubleVariableFmi2Api store(String namePrefix, DoubleVariableFmi2Api variable) {
        return activeScope.store(namePrefix, variable);
    }

    @Override
    public ArrayVariableFmi2Api storeInArray(String name, VariableFmi2Api[] variables) {
        return activeScope.storeInArray(name, variables);
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, String loaderName, String... args) throws Exception {
        return activeScope.createFMU(name, loaderName, args);
    }

    @Override
    public FmuVariableFmi3Api createFMU(String name, Fmi3ModelDescription modelDescription, URI path) throws Exception {
        return activeScope.createFMU(name, modelDescription, path);
    }

    @Override
    public FmuVariableFmi3Api createFMU3(String name, String loaderName, String... args) throws Exception {
        return activeScope.createFMU3(name, loaderName, args);
    }

    @Override
    public void markTransferPoint(String... names) {
        activeScope.markTransferPoint(names);
    }

    @Override
    public void addTransferAs(String... names) {
        activeScope.addTransferAs(names);
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, Fmi2ModelDescription modelDescription, URI path) throws Exception {
        return activeScope.createFMU(name, modelDescription, path);
    }


    @Override
    public <Var extends VariableFmi2Api> Var copy(String name, Var variable) {
        return activeScope.copy(name, variable);
    }

    @Override
    public Set<ComponentVariableFmi2Api> getAllComponentFmi2Variables() {
        return activeScope.getAllComponentFmi2Variables();
    }

    @Override
    public void registerComponentVariableFmi2Api(ComponentVariableFmi2Api componentVariableFmi2Api) {
        activeScope.registerComponentVariableFmi2Api(componentVariableFmi2Api);

    }


    @Override
    public void registerInstanceVariableFmi3Api(InstanceVariableFmi3Api instanceVariableFmi3Api) {
        activeScope.registerInstanceVariableFmi3Api((instanceVariableFmi3Api));
    }

    @Override
    public <S> S findParentScope(Class<S> type) {
        return this.activeScope.findParentScope(type);
    }
}
