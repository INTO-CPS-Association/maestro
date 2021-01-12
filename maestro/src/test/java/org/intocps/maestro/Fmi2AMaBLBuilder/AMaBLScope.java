package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class AMaBLScope implements Fmi2Builder.Scope {
    private static final Function<String, LexIdentifier> createLexIdentifier = s -> new LexIdentifier(s.replace("-", ""), null);
    private final Map<Integer, LexIdentifier> longArrays = new HashMap<>();
    private final List<PStm> statements = new ArrayList<>();
    private final Consumer<AMaBLScope> currentScopeSetter;
    private final Supplier<AMaBLScope> currentScopeGetter;
    private final Fmi2SimulationEnvironment simulationEnvironment;
    ScopeVariables variables = new ScopeVariables();
    List<AMaBLScope> childrenScopes = new ArrayList<>();
    AMaBLVariableCreator variableCreator;

    public AMaBLScope(Consumer<AMaBLScope> scopeSetter, Supplier<AMaBLScope> currentScopeGetter, Fmi2SimulationEnvironment simulationEnvironment) {
        this.currentScopeSetter = scopeSetter;
        this.currentScopeGetter = currentScopeGetter;
        this.simulationEnvironment = simulationEnvironment;
        this.variableCreator = new AMaBLSpecificVariableCreator(this.simulationEnvironment, this);
    }

    @Override
    public Fmi2Builder.WhileScope enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return null;
    }

    @Override
    public Fmi2Builder.IfScope enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return null;
    }

    @Override
    public Fmi2Builder.Scope leave() {
        return null;
    }

    @Override
    public Fmi2Builder.LiteralCreator literalCreator() {
        return null;
    }

    @Override
    public AMaBLVariableCreator variableCreator() {
        return this.variableCreator;
    }

    @Override
    public Fmi2Builder.Value store(double value) {
        return null;
    }

    @Override
    public Fmi2Builder.Value store(Fmi2Builder.Value tag) {
        return null;
    }

    @Override
    public Fmi2Builder.Value store(Fmi2Builder.Value tag, Fmi2Builder.Value value) {
        return null;
    }

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

    public <T> AMablVariable<T> getVariable(T obj) {
        return this.variables.getVariable(obj);
    }

    public void addStatement(PStm pStm) {
        this.statements.add(pStm);
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

    public void addStatements(List<PStm> stms) {
        this.statements.addAll(stms);
    }

    public List<PStm> getStatements() {
        return this.statements;
    }

    public List<PStm> getStatementsRecursive() {
        List<PStm> allStatments = new ArrayList<>();
        allStatments.addAll(this.statements);
        return allStatments;
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
