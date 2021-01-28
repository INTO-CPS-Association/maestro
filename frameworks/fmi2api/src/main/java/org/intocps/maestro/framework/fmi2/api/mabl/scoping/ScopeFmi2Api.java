package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class ScopeFmi2Api implements IMablScope, Fmi2Builder.WhileScope<PStm> {
    final ScopeFmi2Api parent;
    private final MablApiBuilder builder;
    private final ABlockStm block;

    public ScopeFmi2Api(MablApiBuilder builder) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABlockStm();

    }

    public ScopeFmi2Api(MablApiBuilder builder, ScopeFmi2Api parent, ABlockStm block) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
    }

    public ABlockStm getBlock() {
        return block;
    }


    @Override
    public WhileMaBLScope enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
        if (predicate instanceof PredicateFmi2Api) {
            PredicateFmi2Api predicate_ = (PredicateFmi2Api) predicate;
            ABlockStm whileBlock = new ABlockStm();
            AWhileStm whileStm = newWhile(predicate_.getExp(), whileBlock);
            add(whileStm);
            WhileMaBLScope scope = new WhileMaBLScope(builder, whileStm, this, whileBlock);
            scope.activate();
            return scope;
        }
        throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + predicate.getClass());
    }

    @Override
    public IfMaBlScope enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {

        if (predicate instanceof PredicateFmi2Api) {
            PredicateFmi2Api predicate_ = (PredicateFmi2Api) predicate;

            ABlockStm thenStm = newABlockStm();
            ABlockStm elseStm = newABlockStm();

            AIfStm ifStm = newIf(predicate_.getExp(), thenStm, elseStm);
            add(ifStm);
            ScopeFmi2Api thenScope = new ScopeFmi2Api(builder, this, thenStm);
            ScopeFmi2Api elseScope = new ScopeFmi2Api(builder, this, elseStm);
            return new IfMaBlScope(builder, ifStm, this, thenScope, elseScope);
        }

        throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + predicate.getClass());

    }

    @Override
    public ScopeFmi2Api leave() {
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
    public ScopeFmi2Api activate() {
        return (ScopeFmi2Api) builder.getDynamicScope().activate(this);
    }

    @Override
    public DoubleVariableFmi2Api store(double value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public BooleanVariableFmi2Api store(boolean value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public IntVariableFmi2Api store(int value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public DoubleVariableFmi2Api store(String prefix, double value) {
        return store(() -> builder.getNameGenerator().getName(prefix), value);
    }

    @Override
    public BooleanVariableFmi2Api store(String name, boolean value) {
        return store(() -> builder.getNameGenerator().getName(name), value);
    }

    @Override
    public IntVariableFmi2Api store(String name, int value) {
        return store(() -> builder.getNameGenerator().getName(name), value);
    }

    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var store(String name, Var value) {
        return copy(name, value);
    }

    @Override
    public <ValType, Val extends Fmi2Builder.Value<ValType>, Var extends Fmi2Builder.Variable<PStm, Val>> Var copy(String name, Var value) {

        String varName = builder.getNameGenerator().getName(name);
        PStm var = newVariable(varName, ((VariableFmi2Api) value).getType(), ((VariableFmi2Api) value).getReferenceExp().clone());
        add(var);

        return (Var) ((VariableFmi2Api<?>) value)
                .clone(var, builder.getDynamicScope(), newAIdentifierStateDesignator(varName), newAIdentifierExp(varName));
    }

    protected DoubleVariableFmi2Api store(Supplier<String> nameProvider, double value) {
        String name = nameProvider.get();
        ARealLiteralExp initial = newARealLiteralExp(value);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), initial);
        add(var);
        return new DoubleVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    protected BooleanVariableFmi2Api store(Supplier<String> nameProvider, boolean value) {
        String name = nameProvider.get();
        ABoolLiteralExp initial = newABoolLiteralExp(value);
        PStm var = newVariable(name, newABoleanPrimitiveType(), initial);
        add(var);
        return new BooleanVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    protected IntVariableFmi2Api store(Supplier<String> nameProvider, int value) {
        String name = nameProvider.get();
        AIntLiteralExp initial = newAIntLiteralExp(value);
        PStm var = newVariable(name, newAIntNumericPrimitiveType(), initial);
        add(var);
        return new IntVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    @Override
    public String getName(String prefix) {
        return builder.getNameGenerator().getName(prefix);
    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag) {

        return storePrivate(builder.getNameGenerator().getName(), tag);
    }

    @Override
    public IntVariableFmi2Api store(String namePrefix, IntVariableFmi2Api variable) {
        String name = getName(namePrefix);
        PStm var = newVariable(name, newAIntNumericPrimitiveType(), variable.getReferenceExp());
        add(var);
        return new IntVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    private <V> Fmi2Builder.Variable<PStm, V> storePrivate(String name, Fmi2Builder.Value<V> tag) {

        if (!(tag instanceof ValueFmi2Api)) {
            throw new IllegalArgumentException();
        }

        ValueFmi2Api<V> v = (ValueFmi2Api<V>) tag;

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

    @Override
    public FmuVariableFmi2Api createFMU(String name, String loaderName, String... args) throws Exception {
        return VariableCreatorFmi2Api.createFMU(builder, builder.getNameGenerator(), builder.getDynamicScope(), name, loaderName, args, this);
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, ModelDescription modelDescription, URI path) throws Exception {
        return VariableCreatorFmi2Api.createFMU(builder, builder.getNameGenerator(), builder.getDynamicScope(), name, modelDescription, path, this);
    }
}
