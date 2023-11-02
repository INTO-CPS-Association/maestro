package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AParallelBlockStm;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.*;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class ScopeFmi2Api implements IMablScope, FmiBuilder.WhileScope<PStm> {
    private final MablApiBuilder builder;
    private final SBlockStm block;
    private final List<ComponentVariableFmi2Api> fmi2ComponentVariables = new ArrayList<>();
    FmiBuilder.ScopeElement<PStm> parent;
    IntVariableFmi2Api fmiStatusVariable = null;

    public ScopeFmi2Api(MablApiBuilder builder) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABasicBlockStm();

    }

    public ScopeFmi2Api(MablApiBuilder builder, FmiBuilder.ScopeElement<PStm> parent, SBlockStm block) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
    }

    public SBlockStm getBlock() {
        return block;
    }

    @Override
    public WhileMaBLScope enterWhile(FmiBuilder.Predicate predicate) {
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
    public TryMaBlScope enterTry() {
        var bodyStm = newABlockStm();
        var finallyStm = newABlockStm();

        ATryStm tryStm = newTry(bodyStm, finallyStm);
        add(tryStm);
        ScopeFmi2Api bodyScope = new ScopeFmi2Api(builder, null, bodyStm);
        ScopeFmi2Api finallyScope = new ScopeFmi2Api(builder, null, finallyStm);
        var tryScope = new TryMaBlScope(builder, tryStm, this, bodyScope, finallyScope);
        tryScope.enter().activate();
        return tryScope;
    }


    @Override
    public IfMaBlScope enterIf(FmiBuilder.Predicate predicate) {

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
        return ((ScopeFmi2Api) parent).activate();
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
            addAll(index, commands);
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

    /**
     * Attempts to find {@code item} and places {@code commands} after it.
     * If {@code item} is not found, then {@code commands} is added at the top of the scope.
     *
     * @param item
     * @param commands
     */
    @Override
    public void addAfterOrTop(PStm item, PStm... commands) {
        int index = block.getBody().indexOf(item);
        int insertAt = index + 1;
        if (insertAt > block.getBody().size()) {
            add(commands);
        } else {
            addAll(insertAt, commands);
        }

    }

    @Override
    public int indexOf(PStm stm) {
        return this.block.getBody().indexOf(stm);
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


    public DoubleVariableFmi2Api store(Supplier<String> nameProvider, double value) {
        String name = nameProvider.get();
        ARealLiteralExp initial = newARealLiteralExp(value);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), initial);
        add(var);
        return new DoubleVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    public BooleanVariableFmi2Api store(Supplier<String> nameProvider, boolean value) {
        String name = nameProvider.get();
        ABoolLiteralExp initial = newABoolLiteralExp(value);
        PStm var = newVariable(name, newABoleanPrimitiveType(), initial);
        add(var);
        return new BooleanVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    public IntVariableFmi2Api store(Supplier<String> nameProvider, int value) {
        String name = nameProvider.get();
        AIntLiteralExp initial = newAIntLiteralExp(value);
        PStm var = newVariable(name, newAIntNumericPrimitiveType(), initial);
        add(var);
        return new IntVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    public StringVariableFmi2Api store(Supplier<String> nameProvider, String value) {
        String name = nameProvider.get();
        AStringLiteralExp initial = newAStringLiteralExp(value);
        PStm var = newVariable(name, newAStringPrimitiveType(), initial);
        add(var);
        return new StringVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    /**
     * @param identifyingName the name of the MaBL array
     * @param mdArray         non-jagged multidimensional Java array.
     * @param <V>             data type
     * @return an ArrayVariable representing the multidimensional array
     */
    private <V> ArrayVariableFmi2Api<V> storeMDArray(String identifyingName, V[] mdArray) {
        List<Integer> arrayShape = new ArrayList<>();
        PType type;
        V[] subArr = mdArray;
        while (subArr.getClass().getComponentType().isArray()) {
            arrayShape.add(subArr.length);
            subArr = (V[]) subArr[0];
        }
        arrayShape.add(subArr.length);

        if (subArr instanceof Double[]) {
            type = newARealNumericPrimitiveType();
        } else if (subArr instanceof Integer[]) {
            type = newAIntNumericPrimitiveType();
        } else if (subArr instanceof Boolean[]) {
            type = newABoleanPrimitiveType();
        } else if (subArr instanceof String[]) {
            type = newAStringPrimitiveType();
        } else if (subArr instanceof Long[]) {
            type = newAUIntNumericPrimitiveType();
        } else {
            throw new IllegalArgumentException();
        }

        PStm arrayVariableStm = newALocalVariableStm(newAVariableDeclarationMultiDimensionalArray(newAIdentifier(identifyingName), type, arrayShape));

        add(arrayVariableStm);

        return instantiateMDArrayRecursively(mdArray, arrayVariableStm, newAIdentifierStateDesignator(newAIdentifier(identifyingName)),
                newAIdentifierExp(identifyingName));
    }

    /**
     * @param array        multi dimensional array
     * @param declaringStm declaring statement of the root array
     * @param <V>          data type
     * @return an ArrayVariable representing the multidimensional array
     */
    private <V> ArrayVariableFmi2Api<V> instantiateMDArrayRecursively(V[] array, PStm declaringStm, PStateDesignatorBase stateDesignator,
            PExpBase indexExp) {

        if (array.getClass().getComponentType().isArray()) {
            List<VariableFmi2Api> arrays = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                arrays.add(instantiateMDArrayRecursively((V[]) array[i], declaringStm, newAArayStateDesignator(stateDesignator, newAIntLiteralExp(i)),
                        newAArrayIndexExp(indexExp, List.of(newAIntLiteralExp(i)))));
            }
            return new ArrayVariableFmi2Api(declaringStm, arrays.get(0).getType(), this, builder.getDynamicScope(), stateDesignator, indexExp.clone(),
                    arrays);
        }

        List<VariableFmi2Api<V>> variables = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            PType type;
            FmiBuilder.ExpressionValue value;

            if (array instanceof Double[]) {
                type = newARealNumericPrimitiveType();
                value = new DoubleExpressionValue((Double) array[i]);
            } else if (array instanceof Integer[]) {
                type = newAIntNumericPrimitiveType();
                value = new IntExpressionValue((Integer) array[i]);
            } else if (array instanceof Boolean[]) {
                type = newABoleanPrimitiveType();
                value = new BooleanExpressionValue((Boolean) array[i]);
            } else if (array instanceof String[]) {
                type = newAStringPrimitiveType();
                value = new StringExpressionValue((String) array[i]);
            } else if (array instanceof Long[]) {
                type = newAUIntNumericPrimitiveType();
                value = new IntExpressionValue(((Long) array[i]).intValue());
            } else {
                throw new IllegalArgumentException();
            }

            VariableFmi2Api<V> variableToAdd = new VariableFmi2Api<>(declaringStm, type, this, builder.getDynamicScope(),
                    newAArayStateDesignator(stateDesignator.clone(), newAIntLiteralExp(i)),
                    newAArrayIndexExp(indexExp.clone(), List.of(newAIntLiteralExp(i))));

            variableToAdd.setValue(value);
            variables.add(variableToAdd);
        }

        return new ArrayVariableFmi2Api<>(declaringStm, variables.get(0).getType(), this, builder.getDynamicScope(),
                ((AArrayStateDesignator) variables.get(0).getDesignatorClone()).getTarget(), indexExp.clone(), variables);
    }

    protected <V> ArrayVariableFmi2Api<V> store(Supplier<String> nameProvider, V[] value) {
        String name = nameProvider.get();
        int length = value.length;
        PType type = new ANullType();
        PInitializer initializer = null;

        if (value instanceof Double[]) {
            type = new ARealNumericPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer = newAArrayInitializer(Arrays.stream(value).map(v -> newARealLiteralExp((Double) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof Integer[]) {
            type = new AIntNumericPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer = newAArrayInitializer(Arrays.stream(value).map(v -> newAIntLiteralExp((Integer) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof Long[]) {
            type = new AUIntNumericPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer = newAArrayInitializer(Arrays.stream(value).map(v -> newAUIntLiteralExp((Long) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof Boolean[]) {
            type = new ABooleanPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer = newAArrayInitializer(Arrays.stream(value).map(v -> newABoolLiteralExp((Boolean) v)).collect(Collectors.toList()));
            }
        } else if (value instanceof String[]) {
            type = new AStringPrimitiveType();
            if (length > 1 && value[0] != null) {
                initializer = newAArrayInitializer(Arrays.stream(value).map(v -> newAStringLiteralExp((String) v)).collect(Collectors.toList()));
            }
        } else if (value.getClass().getComponentType().isArray()) {
            return storeMDArray(name, value);
        }

        PStm localVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), type, length, initializer));

        final PType finalType = type;

        add(localVarStm);
        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(
                i -> new VariableFmi2Api<>(localVarStm, finalType, this, builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp(name), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

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
    public <V> FmiBuilder.Variable<PStm, V> store(FmiBuilder.Value<V> tag) {

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

    @Override
    public DoubleVariableFmi2Api store(String namePrefix, DoubleVariableFmi2Api variable) {
        String name = getName(namePrefix);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), variable.getReferenceExp());
        add(var);
        return new DoubleVariableFmi2Api(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));
    }

    @Override
    public ArrayVariableFmi2Api storeInArray(String namePrefix, VariableFmi2Api[] variables) {
        String name = getName(namePrefix);
        PType type = variables[0].getType();
        PInitializer initializer = newAArrayInitializer(Arrays.stream(variables).map(VariableFmi2Api::getExp).collect(Collectors.toList()));
        PStm arrayVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), variables[0].getType(), variables.length, initializer));


        add(arrayVarStm);
        return new ArrayVariableFmi2Api(arrayVarStm, type, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name), Arrays.asList(variables));
    }

    private <V> FmiBuilder.Variable<PStm, V> storePrivate(String name, FmiBuilder.Value<V> tag) {

        if (!(tag instanceof ValueFmi2Api)) {
            throw new IllegalArgumentException();
        }

        ValueFmi2Api<V> v = (ValueFmi2Api<V>) tag;

        PExp initial = null;
        FmiBuilder.Variable<PStm, V> variable;

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
    public FmuVariableFmi3Api createFMU(String name, Fmi3ModelDescription modelDescription, URI path) throws Exception {
        return VariableCreatorFmi3Api.createFMU(builder, builder.getNameGenerator(), builder.getDynamicScope(), name, modelDescription, path, this);
    }

    @Override
    public FmuVariableFmi3Api createFMU3(String name, String loaderName, String... args) throws Exception {
        return null;
    }


    // TODO stuff goes here.
    @Override
    public void registerInstanceVariableFmi3Api(InstanceVariableFmi3Api instanceVariableFmi3Api) {

    }


    @Override
    public void markTransferPoint(String... names) {

        List<AStringLiteralExp> strings = new ArrayList<>();
        if (names != null) {
            strings = Arrays.stream(names).map(s -> new AStringLiteralExp(s)).collect(Collectors.toList());
        }

        add(new ATransferStm(strings));
    }

    @Override
    public void addTransferAs(String... names) {

        List<AStringLiteralExp> strings = new ArrayList<>();
        if (names != null) {
            strings = Arrays.stream(names).map(s -> new AStringLiteralExp(s)).collect(Collectors.toList());
        }

        add(new ATransferAsStm(strings));
    }

    @Override
    public <Var extends VariableFmi2Api> Var copy(String name, Var variable) {
        if (variable instanceof BooleanVariableFmi2Api || variable instanceof DoubleVariableFmi2Api || variable instanceof IntVariableFmi2Api ||
                variable instanceof StringVariableFmi2Api) {
            String varName = builder.getNameGenerator().getName(name);
            PStm variableDeclaration = newVariable(varName, variable.getType(), variable.getReferenceExp().clone());
            add(variableDeclaration);
            return (Var) variable.clone(variableDeclaration, builder.getDynamicScope(), newAIdentifierStateDesignator(varName),
                    newAIdentifierExp(varName));
        }
        throw new RuntimeException("Copy is not implemented for the type: " + variable.getClass().getName());
    }

    @Override
    public FmuVariableFmi2Api createFMU(String name, Fmi2ModelDescription modelDescription, URI path) throws Exception {
        return VariableCreatorFmi2Api.createFMU(builder, builder.getNameGenerator(), builder.getDynamicScope(), name, modelDescription, path, this);
    }

    @Override
    public FmiBuilder.ScopeElement<PStm> parent() {
        return this.parent;
    }

    @Override
    public PStm getDeclaration() {
        return this.block;
    }

    @Override
    public <P extends FmiBuilder.ScopeElement<PStm>> P findParent(Class<P> clz) {
        return this.findParentScope(clz);
    }

    @Override
    public IntVariableFmi2Api getFmiStatusVariable() {

        //if this is a parallel block then we just use the global variable as there is no way to control the concurrency anyway. But if this is a
        // child block of a concurrent block then make sure we have a fresh local status variable

        if (this.parent == null) {
            return builder.getGlobalFmiStatus();
        } else {

            if (parent instanceof ScopeFmi2Api) {

                if (((ScopeFmi2Api) this.parent).block instanceof AParallelBlockStm) {
                    if (this.fmiStatusVariable == null) {
                        this.fmiStatusVariable = this.store("status", MablApiBuilder.FmiStatus.FMI_OK.getValue());
                    }
                    return this.fmiStatusVariable;
                } else {
                    return ((ScopeFmi2Api) parent).getFmiStatusVariable();
                }
            } else {
                //ignore try and if
                FmiBuilder.ScopeElement<PStm> p = parent;
                while ((p = p.parent()) != null) {
                    if (p instanceof ScopeFmi2Api) {
                        return ((ScopeFmi2Api) p).getFmiStatusVariable();
                    }
                }
            }


        }
        return null;
    }

    @Override
    public void registerComponentVariableFmi2Api(ComponentVariableFmi2Api componentVariableFmi2Api) {
        this.fmi2ComponentVariables.add(componentVariableFmi2Api);
    }


    @Override
    public <S> S findParentScope(Class<S> type) {
        FmiBuilder.ScopeElement<PStm> p = this;
        while ((p = p.parent()) != null) {
            if (type.isAssignableFrom(p.getClass())) {
                return type.cast(p);
            }
        }
        return null;
    }

    @Override
    public Set<ComponentVariableFmi2Api> getAllComponentFmi2Variables() {
        HashSet<ComponentVariableFmi2Api> compFmi2Variables = new HashSet<>();
        compFmi2Variables.addAll(this.fmi2ComponentVariables);
        var parentScope = this.parent;
        while (parentScope != null && parentScope instanceof ScopeFmi2Api) {
            compFmi2Variables.addAll(((ScopeFmi2Api) parentScope).getAllComponentFmi2Variables());
            parentScope = ((ScopeFmi2Api) parentScope).parent;
        }


        return compFmi2Variables;
    }
}
