package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class ScopeFmi2Api implements IMablScope {
    final IMablScope parent;
    private final MablApiBuilder builder;
    private final ABlockStm block;
    VariableCreatorFmi2Api variableCreator;

    public ScopeFmi2Api(MablApiBuilder builder) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABlockStm();
        this.variableCreator = new VariableCreatorFmi2Api(this, builder);

    }

    public ScopeFmi2Api(MablApiBuilder builder, IMablScope parent, ABlockStm block) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
        this.variableCreator = new VariableCreatorFmi2Api(this, builder);
    }

    public ABlockStm getBlock() {
        return block;
    }

    @Override
    public VariableCreatorFmi2Api getVariableCreator() {
        return variableCreator;
    }

    @Override
    public Fmi2Builder.WhileScope<PStm> enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {

        ABlockStm whileBlock = new ABlockStm();
        AWhileStm whileStm = newWhile(predicate.getExp(), whileBlock);
        add(whileStm);
        WhileMaBLScope scope = new WhileMaBLScope(builder, whileStm, this, whileBlock);
        scope.activate();
        return scope;
    }

    @Override
    public Fmi2Builder.IfScope<PStm> enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
        //todo
        ABlockStm thenStm = newABlockStm();
        ABlockStm elseStm = newABlockStm();

        AIfStm ifStm = newIf(newABoolLiteralExp(true), thenStm, elseStm);
        add(ifStm);
        ScopeFmi2Api thenScope = new ScopeFmi2Api(builder, this, thenStm);
        ScopeFmi2Api elseScope = new ScopeFmi2Api(builder, this, elseStm);
        return new IfMaBlScope(builder, ifStm, this, thenScope, elseScope);
    }

    @Override
    public Fmi2Builder.Scope<PStm> leave() {
        return null;
    }

    @Override
    public void add(PStm... commands) {
        addAll(Arrays.asList(commands));
    }

    @Override
    public void addAll(Collection<PStm> commands) {
        block.getBody().addAll(commands);
    }

    @Override
    public void addBefore(PStm item, PStm... commands) {

        int index = block.getBody().indexOf(item);

        if (index == -1) {
            add(commands);
        } else {
            int insertAt = index - 1;
            if (insertAt < 0) {
                block.getBody().addAll(0, Arrays.asList(commands));
            } else {
                block.getBody().addAll(insertAt, Arrays.asList(commands));
            }
        }

    }

    @Override
    public void addAfter(PStm item, PStm... commands) {
        int index = block.getBody().indexOf(item);
        int insertAt = index + 1;
        if (index == -1 || insertAt > block.getBody().size()) {
            add(commands);
        } else {
            block.getBody().addAll(insertAt, Arrays.asList(commands));
        }
    }

    @Override
    public Fmi2Builder.Scope<PStm> activate() {
        return builder.getDynamicScope().activate(this);
    }

    @Override
    public Fmi2Builder.DoubleVariable<PStm> store(double value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public Fmi2Builder.BoolVariable<PStm> store(boolean value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public Fmi2Builder.IntVariable<PStm> store(int value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public Fmi2Builder.DoubleVariable<PStm> store(String prefix, double value) {
        return store(() -> builder.getNameGenerator().getName(prefix), value);
    }

    @Override
    public Fmi2Builder.BoolVariable<PStm> store(String name, boolean value) {
        return store(() -> builder.getNameGenerator().getName(name), value);
    }

    @Override
    public Fmi2Builder.IntVariable<PStm> store(String name, int value) {
        return store(() -> builder.getNameGenerator().getName(name), value);
    }

    protected Fmi2Builder.DoubleVariable<PStm> store(Supplier<String> nameProvider, double value) {
        String name = nameProvider.get();
        ARealLiteralExp initial = newARealLiteralExp(value);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), initial);
        add(var);
        return new DoubleVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    protected Fmi2Builder.BoolVariable<PStm> store(Supplier<String> nameProvider, boolean value) {
        String name = nameProvider.get();
        ABoolLiteralExp initial = newABoolLiteralExp(value);
        PStm var = newVariable(name, newABoleanPrimitiveType(), initial);
        add(var);
        return new BooleanVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    protected Fmi2Builder.IntVariable<PStm> store(Supplier<String> nameProvider, int value) {
        String name = nameProvider.get();
        AIntLiteralExp initial = newAIntLiteralExp(value);
        PStm var = newVariable(name, newAIntNumericPrimitiveType(), initial);
        add(var);
        return new IntVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag) {

        if (!(tag instanceof ValueFmi2Api)) {
            throw new IllegalArgumentException();
        }

        ValueFmi2Api<V> v = (ValueFmi2Api<V>) tag;

        String name = builder.getNameGenerator().getName();

        PExp initial = null;
        Fmi2Builder.Variable<PStm, V> variable;

        if (v.getType() instanceof ARealNumericPrimitiveType) {
            if (v.get() != null) {
                initial = newARealLiteralExp((Double) v.get());
            }
            //TODO once changed to not use generic values we can return the actual variable type here

        } else if (v.getType() instanceof AIntNumericPrimitiveType) {
            if (v.get() != null) {
                initial = newAIntLiteralExp((Integer) v.get());
            }

        } else if (v.getType() instanceof ABooleanPrimitiveType) {
            if (v.get() != null) {
                initial = newABoolLiteralExp((Boolean) v.get());
            }
        } else if (v.getType() instanceof AStringPrimitiveType) {
            if (v.get() != null) {
                initial = newAStringLiteralExp((String) v.get());
            }
        }

        PStm var = newVariable(name, v.getType(), initial);
        add(var);
        variable = new VariableFmi2Api<>(var, v.getType(), this, builder.getDynamicScope(),
                newAIdentifierStateDesignator(MableAstFactory.newAIdentifier(name)), MableAstFactory.newAIdentifierExp(name));

        return variable;
    }


}
