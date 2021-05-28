package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AParallelBlockStm;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class ScopeFmi2Api implements IMablScope, Fmi2Builder.WhileScope<PStm> {
    final ScopeFmi2Api parent;
    private final MablApiBuilder builder;
    private final SBlockStm block;
    IntVariableFmi2Api fmiStatusVariable = null;

    public ScopeFmi2Api(MablApiBuilder builder) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABasicBlockStm();

    }

    public ScopeFmi2Api(MablApiBuilder builder, ScopeFmi2Api parent, SBlockStm block) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
    }

    public SBlockStm getBlock() {
        return block;
    }

    @Override
    public WhileMaBLScope enterWhile(Fmi2Builder.Predicate predicate) {
        if (predicate instanceof PredicateFmi2Api) {
            PredicateFmi2Api predicate_ = (PredicateFmi2Api) predicate;
            SBlockStm whileBlock = new ABasicBlockStm();
            AWhileStm whileStm = newWhile(predicate_.getExp(), whileBlock);
            add(whileStm);
            WhileMaBLScope scope = new WhileMaBLScope(builder, whileStm, this, whileBlock);
            scope.activate();
            return scope;
        }
        throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + predicate.getClass());
    }

    @Override
    public IfMaBlScope enterIf(Fmi2Builder.Predicate predicate) {

        if (predicate instanceof PredicateFmi2Api) {
            PredicateFmi2Api predicate_ = (PredicateFmi2Api) predicate;

            SBlockStm thenStm = newABlockStm();
            SBlockStm elseStm = newABlockStm();

            AIfStm ifStm = newIf(predicate_.getExp(), thenStm, elseStm);
            add(ifStm);
            ScopeFmi2Api thenScope = new ScopeFmi2Api(builder, this, thenStm);
            ScopeFmi2Api elseScope = new ScopeFmi2Api(builder, this, elseStm);
            return new IfMaBlScope(builder, ifStm, this, thenScope, elseScope);
        }

        throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + predicate.getClass());

    }

    @Override
    public IMablScope parallel() {
        AParallelBlockStm blockStm = new AParallelBlockStm();
        add(blockStm);
        return new ScopeFmi2Api(this.builder, this, blockStm).activate();
    }

    @Override
    public ScopeFmi2Api leave() {
        return parent.activate();
    }

    @Override
    public void add(PStm... commands) {
        addAll(Arrays.asList(commands));
    }

    @Override
    public void addAll(Collection<PStm> commands) {
        //DO NOT USE ADDALL. IT FAILS IN THE ASTCREATOR.
        commands.forEach(x -> block.getBody().add(x));
    }

    @Override
    public void addBefore(PStm item, PStm... commands) {

        int index = block.getBody().indexOf(item);

        if (index == -1) {
            add(commands);
        } else {
            int insertAt = index - 1;
            if (insertAt < 0) {
                addAll(0, commands);
            } else {
                addAll(insertAt, commands);
            }
        }

    }

    private void addAll(int index, PStm... commands) {
        // ASTCreator addAll is broken. Therefor add individually
        int index_ = index;
        for (int i = 0; i < commands.length; i++, index_++) {
            block.getBody().add(index_, commands[i]);
        }
    }

    @Override
    public void addAfter(PStm item, PStm... commands) {
        int index = block.getBody().indexOf(item);
        int insertAt = index + 1;
        if (index == -1 || insertAt > block.getBody().size()) {
            add(commands);
        } else {
            addAll(insertAt, commands);
        }
    }

    @Override
    public ScopeFmi2Api activate() {
        if (builder.getDynamicScope() != null) {
            return (ScopeFmi2Api) builder.getDynamicScope().activate(this);
        }
        return this;
    }

    @Override
    public DoubleVariableFmi2Api store(double value) {
        return store(() -> builder.getNameGenerator().getName(), value);
    }

    @Override
    public StringVariableFmi2Api store(String value) {
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
    public StringVariableFmi2Api store(String prefix, String value) {
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
    public <V> ArrayVariableFmi2Api<V> store(String name, V[] value) {
        return store(() -> builder.getNameGenerator().getName(name), value);
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

    protected StringVariableFmi2Api store(Supplier<String> nameProvider, String value) {
        String name = nameProvider.get();
        AStringLiteralExp initial = newAStringLiteralExp(value);
        PStm var = newVariable(name, newAStringPrimitiveType(), initial);
        add(var);
        return new StringVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    protected <V> ArrayVariableFmi2Api<V> store(Supplier<String> nameProvider, V[] value) {
        String name = nameProvider.get();
        int length = value.length;
        PType type = new ANullType();
        PInitializer initializer = null;

        if (value instanceof Double[]) {
            type = new ARealNumericPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer =
                        newAArrayInitializer(Arrays.asList(value).stream().map(v -> newARealLiteralExp((Double) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof Integer[]) {
            type = new AIntNumericPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer =
                        newAArrayInitializer(Arrays.asList(value).stream().map(v -> newAIntLiteralExp((Integer) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof Boolean[]) {
            type = new ABooleanPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer =
                        newAArrayInitializer(Arrays.asList(value).stream().map(v -> newABoolLiteralExp((Boolean) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof String[]) {
            type = new AStringPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer =
                        newAArrayInitializer(Arrays.asList(value).stream().map(v -> newAStringLiteralExp((String) v)).collect(Collectors.toList()));
            }
        }

        PStm localVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), type, length, initializer));

        final PType finalType = type;

        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(
                i -> new VariableFmi2Api<>(localVarStm, finalType, null, builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp(name), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

        add(localVarStm);
        return new ArrayVariableFmi2Api(localVarStm, type, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name), items);
    }

    @Override
    public String getName(String prefix) {
        return builder.getNameGenerator().getName(prefix);
    }

    @Override
    public IMablScope enterScope() {
        ABasicBlockStm blockStm = new ABasicBlockStm();
        add(blockStm);
        return new ScopeFmi2Api(this.builder, this, blockStm).activate();
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
    public <Var extends VariableFmi2Api> Var copy(String name, Var variable) {
        if (variable instanceof BooleanVariableFmi2Api || variable instanceof DoubleVariableFmi2Api || variable instanceof IntVariableFmi2Api ||
                variable instanceof StringVariableFmi2Api) {
            String varName = builder.getNameGenerator().getName(name);
            PStm variableDeclaration = newVariable(varName, variable.getType(), variable.getReferenceExp().clone());
            add(variableDeclaration);
            return (Var) variable
                    .clone(variableDeclaration, builder.getDynamicScope(), newAIdentifierStateDesignator(varName), newAIdentifierExp(varName));
        }
        throw new RuntimeException("Copy is not implemented for the type: " + variable.getClass().getName());
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, ModelDescription modelDescription, URI path) throws Exception {
        return VariableCreatorFmi2Api.createFMU(builder, builder.getNameGenerator(), builder.getDynamicScope(), name, modelDescription, path, this);
    }

    @Override
    public IntVariableFmi2Api getFmiStatusVariable() {

        //if this is a parallel block then we just use the global variable as there is no way to control the concurrency anyway. But if this is a
        // child block of a concurrent block then make sure we have a fresh local status variable

        if (this.parent == null) {
            return builder.getGlobalFmiStatus();
        } else if (this.parent.block instanceof AParallelBlockStm) {
            if (this.fmiStatusVariable == null) {
                this.fmiStatusVariable = this.store("status", MablApiBuilder.FmiStatus.FMI_OK.getValue());
            }
            return this.fmiStatusVariable;
        } else {
            return this.parent.getFmiStatusVariable();
        }
    }
}
