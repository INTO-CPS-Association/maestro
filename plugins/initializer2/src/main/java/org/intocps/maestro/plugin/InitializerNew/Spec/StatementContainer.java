package org.intocps.maestro.plugin.InitializerNew.Spec;

import org.intocps.maestro.ast.*;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class StatementContainer {
    private static final Function<String, LexIdentifier> createLexIdentifier = s -> new LexIdentifier(s.replace("-", ""), null);
    private static StatementContainer container = null;
    private final LexIdentifier statusVariable = createLexIdentifier.apply("status");
    private final List<PStm> statements = new ArrayList<>();
    // FMU Name to AST Variable
    private final Map<String, AVariableDeclaration> fmuVariables = new HashMap<>();
    private final Map<String, AVariableDeclaration> fmuInstances = new HashMap<>();
    private final Map<Integer, LexIdentifier> realArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> boolArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> longArrays = new HashMap<>();
    private final Map<String, Map<Long, VariableLocation>> instanceVariables = new HashMap<>();
    private final Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance,
                ModelDescription.ScalarVariable>>> inputToOutputMapping = new HashMap<>();
    private final IntFunction<String> booleanArrayVariableName = i -> "booleanValueSize" + i;
    private final IntFunction<String> realArrayVariableName = i -> "realValueSize" + i;
    public PExp startTime;
    public PExp endTime;
    /**
     * <code>instancesLookupDependencies</code> is set to true the co-simulatino enters the stage where
     * dependencies are to be looked up. It is detected by the first "get".
     */
    private boolean instancesLookupDependencies = false;


    private StatementContainer() {
        AVariableDeclaration status = newAVariableDeclaration(statusVariable, newAIntNumericPrimitiveType(),
                newAExpInitializer(newAIntLiteralExp(0)));
        statements.add(newALocalVariableStm(status));
    }

    public static StatementContainer getInstance() {
        if (container == null) {
            container = new StatementContainer();
        }
        return container;
    }

    private static PStm createGetSVsStatement(String instanceName, String functionName, long[] longs, LexIdentifier valueArray,
            LexIdentifier valRefArray, LexIdentifier statusVariable) {
        return newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)), newACallExp(
                newAIdentifierExp(createLexIdentifier.apply(functionName)), new ArrayList<PExp>(
                                Arrays.asList(newAIdentifierExp(valRefArray), newAUIntLiteralExp((long) longs.length),
                                        newAIdentifierExp(valueArray))))));
    }

    public static void reset() {
        container = null;
    }

    public List<PStm> getStatements() {
        return statements;
    }

    public void createSetupExperimentStatement(String instanceName, boolean toleranceDefined, double tolerance,
            boolean stopTimeDefined) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)), newACallExp(
                newAIdentifierExp(createLexIdentifier.apply("setupExperiment")), new ArrayList<PExp>(
                                Arrays.asList(newABoolLiteralExp(toleranceDefined), newARealLiteralExp(tolerance),
                                        this.startTime.clone(), newABoolLiteralExp(stopTimeDefined),
                                        this.endTime.clone())))

                ));
        statements.add(statement);
    }

    public void enterInitializationMode(String instanceName) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        newACallExp(newAIdentifierExp(createLexIdentifier.apply("enterInitializationMode")), null)));
        statements.add(statement);
    }

    public void setReals(String instanceName, long[] longs, double[] doubles) {
        // Create the array to contain the values
        LexIdentifier realArray = findArrayOfSize(realArrays, doubles.length);
        // The array does not exist. Create it and initialize it.
        if (realArray == null) {
            List<PExp> args = Arrays.stream(doubles).mapToObj(x -> newARealLiteralExp(x)).collect(Collectors.toList());
            PInitializer initializer = newAArrayInitializer(args);
            realArray = createNewArrayAndAddToStm(realArrayVariableName.apply(longs.length), realArrays,
                    newAArrayType(newARealNumericPrimitiveType(), doubles.length), initializer);
        } else {
            // The array exists. Assign its values.
            for (int i = 0; i < doubles.length; i++) {
                PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(realArray), newAIntLiteralExp(i)),
                        newARealLiteralExp(doubles[i]));
                statements.add(stm);
            }
        }

        // LongFunction valRefsFunctionMapper = l -> MableAstFactory.newAUIntLiteralExp(l);
        // LexIdentifier valRefs = genericFindOrCreateArrayAndAssign(longs, valRefsFunctionMapper)
        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)), newACallExp(
                newAIdentifierExp(createLexIdentifier.apply("setReal")), new ArrayList<PExp>(
                                Arrays.asList(newAIdentifierExp(valRefs), newAUIntLiteralExp((long) longs.length),
                                        newAIdentifierExp(realArray))))
                ));
        statements.add(statement);
    }

    public void setInputOutputMapping(
            Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance,
                    ModelDescription.ScalarVariable>>> inputOutputMapping) {
        inputOutputMapping.forEach(this.inputToOutputMapping::put);
    }

    private LexIdentifier findOrCreateValueReferenceArrayAndAssign(long[] valRefs) {
        LexIdentifier arrayName = findArrayOfSize(longArrays, valRefs.length);
        if (arrayName != null) {

            for (int i = 0; i < valRefs.length; i++) {
                PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(arrayName), newAIntLiteralExp(i)),
                        newAUIntLiteralExp(valRefs[i]));
                statements.add(stm);
            }
            return arrayName;
        } else {
            return createNewArrayAndAddToStm("valRefsSize" + valRefs.length, longArrays,
                    newAArrayType(newAUIntNumericPrimitiveType(), valRefs.length), newAArrayInitializer(Arrays.stream(valRefs).mapToObj(valRef -> newAUIntLiteralExp(valRef))
                                    .collect(Collectors.toList())));
        }
    }

    private LexIdentifier findArrayOfSize(Map<Integer, LexIdentifier> arrays, int i) {
        return arrays.getOrDefault(i, null);
    }

    private LexIdentifier createNewArrayAndAddToStm(String name, Map<Integer, LexIdentifier> array, AArrayType arType, PInitializer initializer) {
        LexIdentifier lexID = createLexIdentifier.apply(name);
        PStm stm = newALocalVariableStm(newAVariableDeclaration(lexID, arType, initializer));
        statements.add(stm);
        array.put(arType.getSize(), lexID);
        return lexID;
    }

    public void getBooleans(String instanceName, long[] longs) {
        if (!instancesLookupDependencies) {
            instancesLookupDependencies = true;
        }

        // Create the value array
        LexIdentifier valueArray = findArrayOfSize(boolArrays, longs.length);
        // The array does not exist. Create it
        if (valueArray == null) {
            valueArray = createNewArrayAndAddToStm(booleanArrayVariableName.apply(longs.length), boolArrays,
                    newAArrayType(newABoleanPrimitiveType(), longs.length), null);
        }

        // Create the valRefArray
        LexIdentifier valRefArray = findOrCreateValueReferenceArrayAndAssign(longs);

        // Create the getBoolean statement
        PStm statement = createGetSVsStatement(instanceName, "getBoolean", longs, valueArray, valRefArray, statusVariable);
        statements.add(statement);

        // Update instanceVariables
        updateInstanceVariables(instanceName, longs, valueArray);
    }

    public void getReals(String instanceName, long[] longs) {
        if (!instancesLookupDependencies) {
            instancesLookupDependencies = true;
        }

        // Create the value array
        LexIdentifier valueArray = findArrayOfSize(realArrays, longs.length);
        // The array does not exist. Create it
        if (valueArray == null) {
            valueArray = createNewArrayAndAddToStm("realValueSize" + longs.length, realArrays,
                    newAArrayType(newARealNumericPrimitiveType(), longs.length), null);
        }

        // Create the valRefArray
        LexIdentifier valRefArray = findOrCreateValueReferenceArrayAndAssign(longs);

        // Create the statement
        PStm statement = createGetSVsStatement(instanceName, "getReal", longs, valueArray, valRefArray, statusVariable);
        statements.add(statement);

        // Update instanceVariables
        updateInstanceVariables(instanceName, longs, valueArray);
    }

    private void updateInstanceVariables(String instanceName, long[] longs, LexIdentifier valueArray) {
        Map<Long, VariableLocation> instanceVariables = this.instanceVariables.computeIfAbsent(instanceName, k -> new HashMap<>());

        // Move the retrieved values to the respective instance variables
        for (int i = 0; i < longs.length; i++) {
            VariableLocation svVar = instanceVariables.get(longs[i]);
            AArrayIndexExp assignmentExpression = newAArrayIndexExp(newAIdentifierExp(valueArray),
                            Collections.singletonList(newAUIntLiteralExp((long) i)));

            // Create the variable, initialize it, and add it to instanceVariables
            if (svVar == null) {
                String id = instanceName + "SvValRef" + longs[i];
                PStm stm = newALocalVariableStm(newAVariableDeclaration(createLexIdentifier.apply(id), newABoleanPrimitiveType(),
                                newAExpInitializer(assignmentExpression)));
                statements.add(stm);

                VariableLocation varLoc = new VariableLocation(id, ModelDescription.Types.Boolean);
                instanceVariables.put(longs[i], varLoc);
            }
            // Assign to the variable
            else {
                PStm stm = newAAssignmentStm(newAIdentifierStateDesignator(createLexIdentifier.apply(svVar.variableId)),
                                assignmentExpression);
                statements.add(stm);
            }
        }
    }

    /**
     * This creates a function (<code>valueLocator</code>) that is used to locate an output corresponding to the given input that is to be set.
     * If the co-simulation has entered <code>instancesLookupDependencies</code> then it will determine the variable using the inputs to outputs map (<code>inputOutputMapping2</code>)
     * and locate it using <code>instanceVariables</code>.
     * Otherwise it uses the argument <code>literalExp</code> to return a <code>valueLocator</code>.
     * It uses  to
     *
     * @param instanceName name of the instance on which the FMI-function is invoked.
     * @param valRefs      value reference argument to the FMI function
     * @param literalExp   Function to apply the int to, if the co-simulation has not entered <code>instancesLookupDependencies</code> mode.
     * @return a function (valueLocator) that, given an int representing the index of <code>valRefs</code>, will locate the corresponding output
     */
    public IntFunction<PExp> generateInstanceVariablesValueLocator(String instanceName, long[] valRefs, IntFunction<PExp> literalExp) {
        IntFunction<PExp> valueLocator = null;

        if (this.instancesLookupDependencies) {
            Optional<ModelConnection.ModelInstance> key = this.inputToOutputMapping.keySet().stream().filter(x -> x.instanceName.equals(instanceName))
                    .findFirst();

            if (key.isPresent()) {
                Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>> svInputsToOutputs =
                        this.inputToOutputMapping
                        .get(key.get());
                valueLocator = i -> {
                    Optional<ModelDescription.ScalarVariable> svInputVarToOutput = svInputsToOutputs.keySet().stream()
                            .filter(x -> x.valueReference == valRefs[i]).findFirst();

                    if (svInputVarToOutput.isPresent()) {
                        AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable> output = svInputsToOutputs
                                .get(svInputVarToOutput.get());
                        VariableLocation variable = this.instanceVariables.get(output.getKey().instanceName).get(output.getValue().valueReference);
                        return newAIdentifierExp(createLexIdentifier.apply(variable.variableId));
                    } else {
                        return newAIdentifierExp(createLexIdentifier.apply("FAILED TO FIND VARIABLE"));
                    }
                };
            }
        } else {
            valueLocator = literalExp::apply;
        }
        return valueLocator;
    }

    public void setBooleans(String instanceName, long[] longs, boolean[] booleans) {

        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<PExp> valueLocator = generateInstanceVariablesValueLocator(instanceName, longs,
                i -> newABoolLiteralExp(booleans[i]));

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(boolArrays, booleans.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            List<PExp> args = new ArrayList<>();
            for (int i = 0; i < booleans.length; i++) {
                args.add(valueLocator.apply(i));
            }

            PInitializer initializer = newAArrayInitializer(args);
            valueArray = createNewArrayAndAddToStm(booleanArrayVariableName.apply(longs.length), realArrays,
                    newAArrayType(newABoleanPrimitiveType(), booleans.length), initializer);
        } else {
            // The array exists. Assign its values.
            for (int i = 0; i < booleans.length; i++) {
                PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(valueArray), newAIntLiteralExp(i)),
                        valueLocator.apply(i));
                statements.add(stm);
            }
        }

        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)), newACallExp(
                newAIdentifierExp(createLexIdentifier.apply("setBoolean")), new ArrayList<PExp>(
                                Arrays.asList(newAIdentifierExp(valRefs), newAUIntLiteralExp((long) longs.length),
                                        newAIdentifierExp(valueArray))))

                ));
        statements.add(statement);
    }


    public void exitInitializationMode(String instanceName) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable), newADotExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        newACallExp(newAIdentifierExp(createLexIdentifier.apply("exitInitializationMode")), null)));
        statements.add(statement);
    }
}
