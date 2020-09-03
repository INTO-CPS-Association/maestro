package org.intocps.maestro.plugin.Initializer.Spec;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.ExpandException;
import org.intocps.maestro.plugin.Initializer.ConversionUtilities.BooleanUtils;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class StatementGeneratorContainer {
    private static final Function<String, LexIdentifier> createLexIdentifier = s -> new LexIdentifier(s.replace("-", ""), null);
    private static StatementGeneratorContainer container = null;
    private final LexIdentifier statusVariable = createLexIdentifier.apply("status");
    private final List<PStm> statements = new ArrayList<>();
    private final Map<Integer, LexIdentifier> realArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> boolArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> longArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> intArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> stringArrays = new HashMap<>();

    private final Map<ModelDescription.ScalarVariable, LexIdentifier> convergenceRefArray =
            new HashMap<ModelDescription.ScalarVariable, LexIdentifier>();
    private final Map<ModelDescription.ScalarVariable, LexIdentifier> loopValueArray = new HashMap<ModelDescription.ScalarVariable, LexIdentifier>();

    private final EnumMap<ModelDescription.Types, String> typesStringMap = new EnumMap<>(ModelDescription.Types.class) {
        {
            put(ModelDescription.Types.Integer, "Integer");
            put(ModelDescription.Types.String, "String");
            put(ModelDescription.Types.Boolean, "Boolean");
            put(ModelDescription.Types.Real, "Real");
        }
    };


    private final Map<String, Map<Long, VariableLocation>> instanceVariables = new HashMap<>();
    private final Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>>
            inputToOutputMapping = new HashMap<>();
    private final IntFunction<String> booleanArrayVariableName = i -> "booleanValueSize" + i;
    private final IntFunction<String> realArrayVariableName = i -> "realValueSize" + i;
    private final IntFunction<String> intArrayVariableName = i -> "intValueSize" + i;
    private final IntFunction<String> stringArrayVariableName = i -> "stringValueSize" + i;

    public PExp startTime;
    public PExp endTime;
    public double absoluteTolerance;
    public double relativeTolerance;

    /**
     * <code>instancesLookupDependencies</code> is set to true the co-simulatino enters the stage where
     * dependencies are to be looked up. It is detected by the first "get".
     */
    private boolean instancesLookupDependencies = false;
    public List<ModelParameter> modelParameters;


    private StatementGeneratorContainer() {
        AVariableDeclaration status =
                newAVariableDeclaration(statusVariable, newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)));
        statements.add(newALocalVariableStm(status));
    }

    public static StatementGeneratorContainer getInstance() {
        if (container == null) {
            container = new StatementGeneratorContainer();
        }
        return container;
    }

    private static PStm createGetSVsStatement(String instanceName, String functionName, long[] longs, LexIdentifier valueArray,
            LexIdentifier valRefArray, LexIdentifier statusVariable) {
        return newAAssignmentStm(newAIdentifierStateDesignator(statusVariable),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply(functionName).clone(), new ArrayList<PExp>(
                                Arrays.asList(newAIdentifierExp(valRefArray), newAUIntLiteralExp((long) longs.length),
                                        newAIdentifierExp(valueArray)))));
    }

    public static void reset() {
        container = null;
    }

    public SPrimitiveTypeBase FMITypeToMablType(ModelDescription.Types type) {
        switch (type) {
            case Boolean:
                return newABoleanPrimitiveType();
            case Real:
                return newARealNumericPrimitiveType();
            case Integer:
                return newAIntNumericPrimitiveType();
            case String:
                return newAStringPrimitiveType();
            default:
                throw new UnsupportedOperationException("Converting fmi type: " + type + " to mabl type is not supported.");
        }
    }

    public List<PStm> getStatements() {
        return statements;
    }

    public void createSetupExperimentStatement(String instanceName, boolean toleranceDefined, double tolerance, boolean stopTimeDefined) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("setupExperiment").clone(), new ArrayList<>(
                                Arrays.asList(newABoolLiteralExp(toleranceDefined), newARealLiteralExp(tolerance), this.startTime.clone(),
                                        newABoolLiteralExp(stopTimeDefined), this.endTime.clone())))

        );
        statements.add(statement);
    }

    public void exitInitializationMode(String instanceName) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("exitInitializationMode").clone(), null));
        statements.add(statement);
    }

    public void createFixedPointIteration(List<UnitRelationship.Variable> loopVariables, int iterationMax, int sccNumber,
            List<ModelDescription.ScalarVariable> outputs, ISimulationEnvironment env) throws ExpandException {
        LexIdentifier end = newAIdentifier(String.format("end%d", sccNumber));
        statements.add(newALocalVariableStm(
                newAVariableDeclaration(end, newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(iterationMax)))));

        LexIdentifier start = newAIdentifier(String.format("start%d", sccNumber));

        for (ModelDescription.ScalarVariable output : outputs) {
            var lexIdentifier = createReferenceArray(output);
            //Create map to test references against
            convergenceRefArray.put(output, lexIdentifier);
        }

        statements
                .add(newALocalVariableStm(newAVariableDeclaration(start, newARealNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)))));

        List<PStm> loopStmts = new Vector<>();

        LexIdentifier doesConverge = new LexIdentifier("DoesConverge", null);
        loopStmts.add(newALocalVariableStm(
                newAVariableDeclaration(doesConverge, newABoleanPrimitiveType(), newAExpInitializer(newABoolLiteralExp(true)))));


        PerformLoopActions(loopVariables, env);

        //Check for convergence
        CheckLoopConvergence(outputs, iterationMax, doesConverge);

        loopStmts.add(newIf(newAnd(newNot(newAIdentifierExp(doesConverge)), newALessEqualBinaryExp(newAIdentifierExp(start), newAIdentifierExp(end))),
                //Break loop - not converging and maximum number of iterations run
                //newAAssignmentStm(),
                null,
                newABlockStm(UpdateReferenceArray(outputs))));


        loopStmts.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) start.clone()),
                newPlusExp(newAIdentifierExp((LexIdentifier) start.clone()), newAIntLiteralExp(1))));

        //solve problem by updating values
        statements.add(newWhile(
                newAnd(newALessEqualBinaryExp(newAIdentifierExp(start), newAIdentifierExp(end)), newNot(newAIdentifierExp(doesConverge))),
                newABlockStm(loopStmts)));
    }

    private List<PStm> PerformLoopActions(List<UnitRelationship.Variable> loopVariables, ISimulationEnvironment env) {
        List<PStm> LoopStatements = new Vector<>();
        loopVariables.stream().map(variable -> variable.scalarVariable).forEach(variable -> {
            long[] scalarValueIndices = new long[]{variable.scalarVariable.valueReference};

            //All members of the same set has the same causality, type and comes from the same instance
            if (variable.scalarVariable.causality == ModelDescription.Causality.Output) {
                var array = createNewArrayAndAddToStm(variable.getScalarVariable().name, null,
                        newAArrayType(FMITypeToMablType(variable.scalarVariable.getType().type)), null);
                loopValueArray.put(variable.scalarVariable, array);
                try {
                    LoopStatements
                            .addAll(getValueStm(variable.instance.getText(), array, scalarValueIndices, variable.scalarVariable.getType().type));
                } catch (ExpandException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    LoopStatements.add(setValueOnPortStm(variable.instance, variable.scalarVariable.getType().type, variable.scalarVariable,
                            scalarValueIndices, env));
                } catch (ExpandException e) {
                    e.printStackTrace();
                }
            }
        });

        return LoopStatements;
    }

    private PStm setValueOnPortStm(LexIdentifier comp, ModelDescription.Types type, ModelDescription.ScalarVariable variable,
            long[] scalarValueIndices, ISimulationEnvironment env) throws ExpandException {
        ComponentInfo componentInfo = env.getUnitInfo(comp, Framework.FMI2);
        ModelConnection.ModelInstance modelInstances = new ModelConnection.ModelInstance(componentInfo.fmuIdentifier, comp.getText());

        if (type == ModelDescription.Types.Boolean) {
            return setBooleansStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(GetValues(Collections.singletonList(variable), modelInstances)).map(Boolean.class::cast)
                            .collect(BooleanUtils.TO_BOOLEAN_ARRAY));
        } else if (type == ModelDescription.Types.Real) {
            return setRealsStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(GetValues(Collections.singletonList(variable), modelInstances)).mapToDouble(o -> Double.parseDouble(o.toString()))
                            .toArray());
        } else if (type == ModelDescription.Types.Integer) {
            return setIntegersStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(GetValues(Collections.singletonList(variable), modelInstances)).mapToInt(o -> Integer.parseInt(o.toString()))
                            .toArray());
        } else if (type == ModelDescription.Types.String) {
            return setStringsStm(comp.getText(), scalarValueIndices,
                    (String[]) Arrays.stream(GetValues(Collections.singletonList(variable), modelInstances)).map(o -> o.toString()).toArray());
        } else {
            throw new ExpandException("Unrecognised type: " + type.name());
        }
    }

    private LexIdentifier createReferenceArray(ModelDescription.ScalarVariable variable) throws ExpandException {
        PExp[] args = new PExp[Math.toIntExact(variable.getValueReference())];
        Arrays.fill(args, getDefaultArrayValue(variable.getType().type));

        PInitializer initializer = MableAstFactory.newAArrayInitializer(Arrays.asList(args));
        LexIdentifier valueArray = createNewArrayAndAddToStm("Ref" + variable.getName() + "Size" + variable.getValueReference(), null,
                newAArrayType(FMITypeToMablType(variable.getType().type)), initializer);
        return valueArray;
    }

    private List<PStm> UpdateReferenceArray(List<ModelDescription.ScalarVariable> outputPorts) {
        List<PStm> updateStmts = new Vector<>();
        outputPorts.forEach(o -> {
            var referenceValue = convergenceRefArray.get(o);
            var currentValue = loopValueArray.get(o);
            updateStmts.add(newAAssignmentStm(newAIdentifierStateDesignator(referenceValue), newAIdentifierExp(currentValue)));
        });
        return updateStmts;
    }

    //This method should check if all output of the Fixed Point iteration have stabilized/converged
    private List<PStm> CheckLoopConvergence(List<ModelDescription.ScalarVariable> outputPorts, int sccNumber, LexIdentifier doesConverge) {
        List<PStm> result = new Vector<>();

        outputPorts.forEach(o -> {
            LexIdentifier index = newAIdentifier("index" + sccNumber);
            result.add(newALocalVariableStm(newAVariableDeclaration(index, newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)))));

            List<PStm> convergenceLoop = new Vector<>();

            var referenceValue = convergenceRefArray.get(o);
            var currentValue = loopValueArray.get(o);
            //find number of places in array
            var arraySize = newAIntLiteralExp(5);

            //convergenceLoop.add(newIf(isClose(), null, newAAssignmentStm(newAIdentifierStateDesignator(doesConverge), newABoolLiteralExp(false)));
            convergenceLoop.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) index.clone()),
                    newPlusExp(newAIdentifierExp((LexIdentifier) index.clone()), newAIntLiteralExp(1))));

            convergenceLoop.add(newIf(
                    newACallExp(newAIdentifierExp(createLexIdentifier.apply("Math")), (LexIdentifier) createLexIdentifier.apply("isClose").clone(),
                            new ArrayList<>(Arrays.asList(newAArrayIndexExp(newAIdentifierExp(currentValue),
                                    Collections.singletonList(newAUIntLiteralExp((long) index.clone()))),
                                    newAArrayIndexExp(newAIdentifierExp(referenceValue),
                                            Collections.singletonList(newAUIntLiteralExp((long) index.clone()))),
                                    newARealLiteralExp(this.absoluteTolerance), newARealLiteralExp(this.relativeTolerance)))), null,
                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) doesConverge.clone()), newABoolLiteralExp(false))));

            convergenceLoop.add(newIf(newAnd(newALessEqualBinaryExp(arraySize, newAIdentifierExp(index)), newNot(newAIdentifierExp(doesConverge))),
                    newABlockStm(
                            Arrays.asList(
                                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")),
                                            newABoolLiteralExp(false)),
                                    newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                            Arrays.asList(newAIntLiteralExp(4),
                                    newAStringLiteralExp("The initialization of the system was not possible since loop is not converging")))))),
                    null));


            result.add(newWhile(newAnd(newALessEqualBinaryExp(newAIdentifierExp(index), arraySize), newNot(newAIdentifierExp(doesConverge))),
                    newABlockStm(convergenceLoop)));

        });

        return result;
    }

    public void enterInitializationMode(String instanceName) {
        PStm statement = newAAssignmentStm(newAIdentifierStateDesignator(statusVariable),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("enterInitializationMode").clone(), null));
        statements.add(statement);
    }

    public void setReals(String instanceName, long[] longs, double[] doubles) {
        statements.add(setRealsStm(instanceName, longs, doubles));
    }

    public void setBooleans(String instanceName, long[] longs, boolean[] booleans) {
        statements.add(setBooleansStm(instanceName, longs, booleans));
    }

    public void setIntegers(String instanceName, long[] longs, int[] ints) {
        statements.add(setIntegersStm(instanceName, longs, ints));
    }

    public void setStrings(String instanceName, long[] longs, String[] strings) {
        statements.add(setStringsStm(instanceName, longs, strings));
    }

    private PStm generateAssignmentStm(String instanceName, long[] longs, LexIdentifier valueArray, LexIdentifier valRefs, String setCommand) {
        PStm statement = MableAstFactory.newAAssignmentStm(MableAstFactory.newAIdentifierStateDesignator(statusVariable),
                newACallExp(MableAstFactory.newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply(setCommand).clone(), new ArrayList<PExp>(
                                Arrays.asList(MableAstFactory.newAIdentifierExp(valRefs), MableAstFactory.newAUIntLiteralExp((long) longs.length),
                                        MableAstFactory.newAIdentifierExp(valueArray)))));
        return statement;
    }

    private LexIdentifier createArray(long[] longs, int valueLength, IntFunction<Pair<PExp, List<PStm>>> valueLocator, String arrayName,
            ModelDescription.Types type, Map<Integer, LexIdentifier> array) {
        List<PExp> args = new ArrayList<>();
        for (int i = 0; i < valueLength; i++) {
            Pair<PExp, List<PStm>> value = valueLocator.apply(i);
            args.add(value.getLeft());
            if (value.getRight() != null) {
                statements.addAll(value.getRight());
            }
        }

        PInitializer initializer = MableAstFactory.newAArrayInitializer(args);
        LexIdentifier valueArray =
                createNewArrayAndAddToStm(arrayName, array, MableAstFactory.newAArrayType(FMITypeToMablType(type), valueLength), initializer);
        return valueArray;
    }


    private void assignValueToArray(int valueLength, IntFunction<Pair<PExp, List<PStm>>> valueLocator, LexIdentifier valueArray) {
        for (int i = 0; i < valueLength; i++) {
            Pair<PExp, List<PStm>> value = valueLocator.apply(i);
            if (value.getRight() != null) {
                statements.addAll(value.getRight());
            }
            PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(valueArray), newAIntLiteralExp(i)), value.getLeft());
            statements.add(stm);
        }
    }


    public void setInputOutputMapping(
            Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> inputOutputMapping) {
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
                    newAArrayType(newAUIntNumericPrimitiveType(), valRefs.length),
                    newAArrayInitializer(Arrays.stream(valRefs).mapToObj(valRef -> newAUIntLiteralExp(valRef)).collect(Collectors.toList())));
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

    private List<PStm> updateInstanceVariables(String instanceName, long[] longs, LexIdentifier valueArray, ModelDescription.Types fmiType) {
        Map<Long, VariableLocation> instanceVariables = this.instanceVariables.computeIfAbsent(instanceName, k -> new HashMap<>());

        List<PStm> result = new Vector<>();
        // Move the retrieved values to the respective instance variables
        for (int i = 0; i < longs.length; i++) {
            VariableLocation svVar = instanceVariables.get(longs[i]);
            AArrayIndexExp assignmentExpression =
                    newAArrayIndexExp(newAIdentifierExp(valueArray), Collections.singletonList(newAUIntLiteralExp((long) i)));

            // Create the variable, initialize it, and add it to instanceVariables
            if (svVar == null) {

                String id = instanceName + "SvValRef" + longs[i];
                PStm stm = newALocalVariableStm(
                        newAVariableDeclaration(createLexIdentifier.apply(id), FMITypeToMablType(fmiType), newAExpInitializer(assignmentExpression)));
                result.add(stm);

                VariableLocation varLoc = new VariableLocation(id, fmiType);
                instanceVariables.put(longs[i], varLoc);
            }
            // Assign to the variable
            else {
                PStm stm = newAAssignmentStm(newAIdentifierStateDesignator(createLexIdentifier.apply(svVar.variableId)), assignmentExpression);
                result.add(stm);
            }
        }
        return result;
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
    public IntFunction<Pair<PExp, List<PStm>>> generateInstanceVariablesValueLocator(String instanceName, long[] valRefs,
            IntFunction<PExp> literalExp, ModelDescription.Types targetType) {
        IntFunction<Pair<PExp, List<PStm>>> valueLocator = null;

        if (this.instancesLookupDependencies) {
            Optional<ModelConnection.ModelInstance> key =
                    this.inputToOutputMapping.keySet().stream().filter(x -> x.instanceName.equals(instanceName)).findFirst();

            if (key.isPresent()) {
                Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>
                        svInputsToOutputs = this.inputToOutputMapping.get(key.get());
                valueLocator = i -> {
                    Optional<ModelDescription.ScalarVariable> svInputVarToOutput =
                            svInputsToOutputs.keySet().stream().filter(x -> x.valueReference == valRefs[i]).findFirst();

                    if (svInputVarToOutput.isPresent()) {
                        AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable> output =
                                svInputsToOutputs.get(svInputVarToOutput.get());
                        VariableLocation variable = this.instanceVariables.get(output.getKey().instanceName).get(output.getValue().valueReference);
                        LexIdentifier variableLexId = createLexIdentifier.apply(variable.variableId);
                        // The type of the located variable might not be the correct type.
                        List<PStm> statements = null;
                        if (output.getValue().type.type != targetType) {
                            statements = new ArrayList<>();
                            String name = null;
                            if (variable.typeMapping.containsKey(targetType)) {
                                // The variable exists. Assign a new value to it.
                                name = variable.typeMapping.get(targetType);
                            } else {
                                // The variable does not exist. Create it.
                                name = instanceName + "SvValRef" + valRefs[i] + targetType;
                                variable.typeMapping.put(targetType, name);
                                ALocalVariableStm stm =
                                        newALocalVariableStm(newAVariableDeclaration(new LexIdentifier(name, null), FMITypeToMablType(targetType)));
                                statements.add(stm);
                            }

                            // Convert the value
                            statements.add(newExpressionStm(newACallExp(newExpandToken(), newAIdentifier(
                                    "convert" + typesStringMap.get(output.getValue().type.type) + "2" + typesStringMap.get(targetType)),
                                    new ArrayList<PExp>(List.of(newAIdentifierExp(variable.variableId), newAIdentifierExp(name))))));


                            variableLexId = createLexIdentifier.apply(name);
                        }
                        return Pair.of(newAIdentifierExp(variableLexId), statements);
                    } else {
                        return Pair.of(newAIdentifierExp(createLexIdentifier
                                .apply("Failed to find the variable with valref " + valRefs[i] + " for instance: " + key.get().instanceName)), null);
                    }
                };
            }
        } else {
            valueLocator = i -> Pair.of(literalExp.apply(i), null);
        }
        return valueLocator;
    }

    public PStm setRealsStm(String instanceName, long[] longs, double[] doubles) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> MableAstFactory.newARealLiteralExp(doubles[i]),
                        ModelDescription.Types.Real);

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(realArrays, longs.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            valueArray = createArray(longs, doubles.length, valueLocator, realArrayVariableName.apply(longs.length), ModelDescription.Types.Real,
                    realArrays);
        } else {
            // The array exists. Assign its values.
            assignValueToArray(doubles.length, valueLocator, valueArray);
        }

        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        return generateAssignmentStm(instanceName, longs, valueArray, valRefs, "setReal");
    }

    public PStm setBooleansStm(String instanceName, long[] longs, boolean[] booleans) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> newABoolLiteralExp(booleans[i]), ModelDescription.Types.Boolean);

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(boolArrays, booleans.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            valueArray =
                    createArray(longs, booleans.length, valueLocator, booleanArrayVariableName.apply(longs.length), ModelDescription.Types.Boolean,
                            boolArrays);
        } else {
            // The array exists. Assign its values.
            assignValueToArray(booleans.length, valueLocator, valueArray);
        }

        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        return generateAssignmentStm(instanceName, longs, valueArray, valRefs, "setBoolean");
    }

    public PStm setIntegersStm(String instanceName, long[] longs, int[] ints) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> MableAstFactory.newAIntLiteralExp(ints[i]),
                        ModelDescription.Types.Integer);

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(intArrays, longs.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            valueArray = createArray(longs, ints.length, valueLocator, intArrayVariableName.apply(longs.length), ModelDescription.Types.Integer,
                    intArrays);
        } else {
            // The array exists. Assign its values.
            assignValueToArray(ints.length, valueLocator, valueArray);
        }

        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        return generateAssignmentStm(instanceName, longs, valueArray, valRefs, "setInteger");
    }

    public PStm setStringsStm(String instanceName, long[] longs, String[] strings) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> newAStringLiteralExp(strings[i]), ModelDescription.Types.String);

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(stringArrays, strings.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            valueArray = createArray(longs, strings.length, valueLocator, stringArrayVariableName.apply(longs.length), ModelDescription.Types.String,
                    stringArrays);
        } else {
            // The array exists. Assign its values.
            assignValueToArray(strings.length, valueLocator, valueArray);
        }

        LexIdentifier valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        return generateAssignmentStm(instanceName, longs, valueArray, valRefs, "setString");
    }

    public Object[] GetValues(List<ModelDescription.ScalarVariable> variables, ModelConnection.ModelInstance modelInstance) {
        Object[] values = new Object[variables.size()];
        var i = 0;
        for (ModelDescription.ScalarVariable v : variables) {
            values[i++] = getNewValue(v, modelInstance);
        }
        return values;
    }

    private Object getNewValue(ModelDescription.ScalarVariable sv, ModelConnection.ModelInstance comp) {
        Object newVal = null;
        if (sv.type.start != null) {
            newVal = sv.type.start;
        }

        for (ModelParameter par : this.modelParameters) {
            if (par.variable.toString().equals(comp + "." + sv.name)) {
                newVal = par.value;
                par.isSet = true;
            }
        }
        if (sv.type.type == ModelDescription.Types.Real) {
            if (newVal instanceof Integer) {
                newVal = (double) (int) newVal;
            }
        }

        return newVal;
    }

    public List<PStm> getValueStm(String instanceName, LexIdentifier valueArray, long[] longs, ModelDescription.Types type) throws ExpandException {
        if (!instancesLookupDependencies) {
            instancesLookupDependencies = true;
        }

        // Create the value array
        if (valueArray == null) {
            valueArray = findArrayOfSize(getArrayMapOfType(type), longs.length);
        }

        // The array does not exist. Create it
        if (valueArray == null) {
            valueArray = createNewArrayAndAddToStm(type.name() + "ValueSize" + longs.length, getArrayMapOfType(type),
                    newAArrayType(FMITypeToMablType(type), longs.length), null);
        }

        // Create the valRefArray
        LexIdentifier valRefArray = findOrCreateValueReferenceArrayAndAssign(longs);

        List<PStm> result = new Vector<>();

        result.add(createGetSVsStatement(instanceName, "get" + type.name(), longs, valueArray, valRefArray, statusVariable));
        // Update instanceVariables
        result.addAll(updateInstanceVariables(instanceName, longs, valueArray, type));
        return result;
    }

    private PExp getDefaultArrayValue(ModelDescription.Types type) throws ExpandException {
        if (type == ModelDescription.Types.Boolean) {
            return newABoolLiteralExp(false);
        } else if (type == ModelDescription.Types.Real) {
            return newARealLiteralExp(0.0);
        } else if (type == ModelDescription.Types.Integer) {
            return newAIntLiteralExp(0);
        } else {
            throw new ExpandException("Unknown type");
        }
    }

    private Map<Integer, LexIdentifier> getArrayMapOfType(ModelDescription.Types type) throws ExpandException {
        if (type == ModelDescription.Types.Boolean) {
            return boolArrays;
        } else if (type == ModelDescription.Types.Real) {
            return realArrays;
        } else if (type == ModelDescription.Types.Integer) {
            return intArrays;
        } else if (type == ModelDescription.Types.String) {
            return stringArrays;
        } else {
            throw new ExpandException("Unrecognised type: " + type.name());
        }
    }

    public void addStatements(List<PStm> statements) {
        this.statements.addAll(statements);
    }
}
