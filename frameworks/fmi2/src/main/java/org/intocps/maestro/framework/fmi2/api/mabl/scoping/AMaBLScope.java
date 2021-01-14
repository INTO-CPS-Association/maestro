package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.AMaBLVariableCreator;
import org.intocps.maestro.framework.fmi2.api.mabl.AMablValue;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.statements.AMaBLStatement;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablDoubleVariable;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;
import static org.intocps.maestro.framework.fmi2.api.mabl.statements.AMaBLStatementFactory.createSingleStatement;

public class AMaBLScope implements IMablScope {
    private static final Function<String, LexIdentifier> createLexIdentifier = s -> new LexIdentifier(s.replace("-", ""), null);
    final IMablScope parent;
    private final Map<Integer, LexIdentifier> longArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> booleanArrays = new HashMap<>();
    //private final Consumer<AMaBLScope> currentScopeSetter;
    //    private final Supplier<AMaBLScope> currentScopeGetter;
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final MablApiBuilder builder;
    // public final LinkedList<AMaBLStatement> statements = new LinkedList<>();
    //public final LinkedList<PStm> statements;
    private final ABlockStm block;
    // ScopeVariables variables = new ScopeVariables();
    AMaBLVariableCreator variableCreator;

    public AMaBLScope(MablApiBuilder builder, Fmi2SimulationEnvironment simulationEnvironment) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABlockStm();
        this.simulationEnvironment = simulationEnvironment;
        this.variableCreator = new AMaBLVariableCreator(this, builder);

    }

    public AMaBLScope(MablApiBuilder builder, IMablScope parent, ABlockStm block, Fmi2SimulationEnvironment simulationEnvironment) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
        this.simulationEnvironment = simulationEnvironment;
        this.variableCreator = new AMaBLVariableCreator(this, builder);
    }

    public static void addStatementBefore(BiFunction<Integer, LinkedList<AMaBLStatement>, Boolean> predicate, LinkedList<AMaBLStatement> statements,
            PStm... stm) {
        OptionalInt index = IntStream.range(0, statements.size()).filter(x -> predicate.apply(x, statements)).findFirst();
        if (index.isPresent()) {
            statements.addAll(index.getAsInt(), Arrays.stream(stm).map(x -> createSingleStatement(x)).collect(Collectors.toList()));
        }
    }

    public ABlockStm getBlock() {
        return block;
    }

    @Override
    public AMaBLVariableCreator getVariableCreator() {
        return variableCreator;
    }

    @Override
    public Fmi2Builder.WhileScope<PStm> enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return null;
    }

    @Override
    public Fmi2Builder.IfScope<PStm> enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
        //todo
        ABlockStm thenStm = newABlockStm();
        ABlockStm elseStm = newABlockStm();

        AIfStm ifStm = newIf(newABoolLiteralExp(true), thenStm, elseStm);
        add(ifStm);
        AMaBLScope thenScope = new AMaBLScope(builder, this, thenStm, this.simulationEnvironment);
        AMaBLScope elseScope = new AMaBLScope(builder, this, elseStm, this.simulationEnvironment);
        return new IfMaBlScope(builder, ifStm, this, thenScope, elseScope);
    }

    @Override
    public Fmi2Builder.Scope<PStm> leave() {
        return null;
    }

    @Override
    public Fmi2Builder.LiteralCreator literalCreator() {
        return null;
    }


    @Override
    public void add(PStm... commands) {
        block.getBody().addAll(Arrays.asList(commands));
    }

    /*
    int findStatement(PStm stm) {
        //since we wrap this is an expensive search
        int index = -1;
        for (int i = 0; i < this.statements.size(); i++) {
            if (this.statements.get(i).getStatement().equals(stm)) {
                //we found it
                index = i;
            }
        }
        return index;
    }
    */


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
    public AMablDoubleVariable store(double value) {
        String name = builder.getNameGenerator().getName();
        ARealLiteralExp initial = newARealLiteralExp(value);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), initial);
        add(var);
        return new AMablDoubleVariable(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));

    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag) {

        if (!(tag instanceof AMablValue)) {
            throw new IllegalArgumentException();
        }

        AMablValue<V> v = (AMablValue<V>) tag;

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
        variable = new AMablVariable<>(var, v.getType(), this, builder.getDynamicScope(),
                newAIdentifierStateDesignator(MableAstFactory.newAIdentifier(name)), MableAstFactory.newAIdentifierExp(name));

        return variable;
    }

 /*   public LexIdentifier getIdentifier() {
        return MableAstFactory.newAIdentifier(this.lexName);
    }

    public AIdentifierStateDesignator getStateDesignator() {
        return new MableAstFactory().newAIdentifierStateDesignator(this.getIdentifier());
    }

    public AIdentifierStateDesignator getNestedStateDesignator() {
        return null;
    }*/


    @Override
    public Fmi2Builder.MDouble doubleFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

    @Override
    public Fmi2Builder.MInt intFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

    @Override
    public Fmi2Builder.MBoolean booleanFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

    /*
    public <T> AMablVariable<T> getVariable(T obj) {
        return this.variables.getVariable(obj);
    }*/
/*
    public void addStatement(PStm... pStm) {
        this.statements.addAll(Arrays.stream(pStm).map(x -> createSingleStatement(x)).collect(Collectors.toList()));
    }

    public void addStatement(int index, PStm... pStm) {
        this.statements.addAll(index, Arrays.stream(pStm).map(x -> createSingleStatement(x)).collect(Collectors.toList()));
    }
    */


   /* public void addStatementBeforeScope(AMaBLScope scope, PStm... stm) {
        addStatementBefore((x, stmList) -> {
            AMaBLStatement curStm = stmList.get(x);
            return curStm instanceof IMablScope && ((ScopeStatement) curStm).scope == scope;
        }, this.statements, stm);
    }

    public void addStatementBeforeLabel(String label, PStm... stm) {
        addStatementBefore((x, stmList) -> {
            AMaBLStatement curStm = stmList.get(x);
            return curStm instanceof LabelStatement && ((LabelStatement) curStm).labelName == label;
        }, this.statements, stm);

    }

    public void addVariable(Object value, AMablVariable fmu) {
        this.variables.addVariable(value, fmu);
    }

    public void addVariable(AMablVariable variable) {
        this.variables.add(variable);
    }

    // Creates variables for the individual ports
    public void getOrCreateVariables(AMaBLScope scope, List<Fmi2Builder.Port> ports) {
        ports.forEach(x -> {
            AMablPort port = (AMablPort) x;
            // See if a variable exists that is allocated to the port
            this.getOrCreateVariableForPort(scope, port);
        });
    }

    // Should created specific variables for each port
    private void getOrCreateVariableForPort(AMaBLScope scope, AMablPort port) {
        AMablVariable<AMablPort> variable = this.variables.getVariable(port);

        if (variable == null) {
            // Create a variable dedicated to the port
            variable = this.variableCreator.createVariableForPort(port);
        }
    }

    private AMablVariable portToVariable(AMablPort port) {
        return this.variables.getVariable(port);
    }
*/

    public Pair<LexIdentifier, List<PStm>> findOrCreateValueReferenceArrayAndAssign(long[] valRefs) {
        LexIdentifier arrayName = findArrayOfSize(longArrays, valRefs.length);
        List<PStm> statement = new Vector<>();
        if (arrayName != null) {
            for (int i = 0; i < valRefs.length; i++) {
                PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(arrayName), newAIntLiteralExp(i)),
                        newAUIntLiteralExp(valRefs[i]));
                statement.add(stm);
            }
        } else {
            arrayName = createLexIdentifier.apply("valRefsSize" + valRefs.length);
            var arType = newAArrayType(newAUIntNumericPrimitiveType());
            PStm stm = newALocalVariableStm(newAVariableDeclaration(arrayName, arType, valRefs.length,
                    newAArrayInitializer(Arrays.stream(valRefs).mapToObj(valRef -> newAUIntLiteralExp(valRef)).collect(Collectors.toList()))));
            longArrays.put(valRefs.length, arrayName);
            statement.add(stm);
        }
        return Pair.of(arrayName, statement);
    }

    private LexIdentifier findArrayOfSize(Map<Integer, LexIdentifier> arrays, int i) {
        return arrays.getOrDefault(i, null);
    }

    /*
    public void addStatements(List<PStm> stms) {
        this.statements.addAll(createSingleStatements(stms));


    }

    public PStm getStatement() {
        List<PStm> statements = new ArrayList<>();
        this.statements.forEach(x -> {
            if (!x.isMeta()) {
                statements.add(x.getStatement());
            }
        });
        return newABlockStm(statements);
    }
*/

    /**
     * Find an array in the given scope or create an array in the given scope of type and size.
     * TODO Work in progress. Look in StatementGenerator for similar functionality.
     * TODO See the function findOrCreateValueReferenceArrayAndAssign above as well.
     *
     * @param scope
     * @param type
     * @param size
     * @return TODO: The LexIdentifier of the Array OR AMablVariable?
     */
    public Object findOrCreateArrayOfSize(AMaBLScope scope, ModelDescription.Types type, int size) {
        LexIdentifier arrayName;
        switch (type) {
            case Boolean:
                break;
            case Real:
                break;
            case Integer:
                break;
            case String:
                break;
            case Enumeration:
                break;
        }

        return null;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return this.simulationEnvironment;
    }


    public static class ScopeVariables {
        public final Map<String, Object> variableNameToObject = new HashMap<>();
        public final Map<AMablVariable, String> fmusToNames = new HashMap<>();
        public final Map<Object, AMablVariable<?>> objectToVariable = new HashMap<>();
        private final List<AMablVariable> variables = new ArrayList<>();

        public <T> AMablVariable<T> getVariable(T object) {
            return (AMablVariable<T>) this.objectToVariable.get(object);
        }

        public void addVariable(Object value, AMablVariable fmu) {
            this.objectToVariable.put(value, fmu);
        }

        public void add(AMablVariable variable) {
            this.variables.add(variable);
        }
    }
}
