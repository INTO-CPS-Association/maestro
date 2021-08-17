package org.intocps.maestro.plugin.initializer.spec;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.plugin.ExpandException;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.maestro.plugin.initializer.ModelParameter;
import org.intocps.maestro.plugin.initializer.conversionutilities.BooleanUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;

public class StatementGeneratorContainer {
    public static final BiFunction<PExp, String, PStm> errorReporter = (status, message) -> {
        return newExpressionStm(call("logger", "log", newAIntLiteralExp(4), newAStringLiteralExp(message + " %d"), status.clone()));
    };
    public final static Integer[] FMIWARNINGANDFATALERRORCODES = {3, 4};
    private static final Function<String, LexIdentifier> createLexIdentifier = s -> new LexIdentifier(s.replace("-", ""), null);
    private static StatementGeneratorContainer container = null;
    private final String statusVariable = "status";
    private final Map<Integer, LexIdentifier> realArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> boolArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> longArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> intArrays = new HashMap<>();
    private final Map<Integer, LexIdentifier> stringArrays = new HashMap<>();

    private final Map<Fmi2SimulationEnvironment.Variable, LexIdentifier> convergenceRefArray = new HashMap<>();
    private final Map<Fmi2SimulationEnvironment.Variable, LexIdentifier> loopValueArray = new HashMap<>();

    private final EnumMap<Fmi2ModelDescription.Types, String> typesStringMap = new EnumMap<>(Fmi2ModelDescription.Types.class) {
        {
            put(Fmi2ModelDescription.Types.Integer, "Integer");
            put(Fmi2ModelDescription.Types.String, "String");
            put(Fmi2ModelDescription.Types.Boolean, "Boolean");
            put(Fmi2ModelDescription.Types.Real, "Real");
        }
    };


    private final Map<String, Map<Long, VariableLocation>> instanceVariables = new HashMap<>();
    private final Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, Fmi2ModelDescription.ScalarVariable>>>
            inputToOutputMapping = new HashMap<>();
    private final IntFunction<String> booleanArrayVariableName = i -> "booleanValueSize" + i;
    private final IntFunction<String> realArrayVariableName = i -> "realValueSize" + i;
    private final IntFunction<String> intArrayVariableName = i -> "intValueSize" + i;
    private final IntFunction<String> stringArrayVariableName = i -> "stringValueSize" + i;

    public PExp startTime;
    public PExp endTime;
    public double absoluteTolerance;
    public double relativeTolerance;
    public List<ModelParameter> modelParameters;
    /**
     * <code>instancesLookupDependencies</code> is set to true the co-simulatino enters the stage where
     * dependencies are to be looked up. It is detected by the first "get".
     */
    private boolean instancesLookupDependencies = false;


    private StatementGeneratorContainer() {
        AVariableDeclaration status =
                newAVariableDeclaration(newAIdentifier(statusVariable), newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)));
        //statements.add(newALocalVariableStm(status));
    }

    public static StatementGeneratorContainer getInstance() {
        if (container == null) {
            container = new StatementGeneratorContainer();
        }
        return container;
    }

    private static PExp statusErrorExpressions(PExp statusExpression, Integer[] errorCodes) {

        List<PExp> orExpressions = new ArrayList<>();
        for (Integer i : errorCodes) {
            orExpressions.add(newEqual(statusExpression.clone(), newAIntLiteralExp(i)));
        }
        PExp orExpression = MableBuilder.nestedOr(orExpressions);
        return orExpression;
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

    public static PStm statusCheck(PExp status, Integer[] statusCodes, String message, boolean breakOut, boolean setGlobalExecution) {
        List<PStm> thenStm = new ArrayList<>();
        thenStm.add(errorReporter.apply(status, message));
        if (setGlobalExecution) {
            thenStm.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)),
                    newABoolLiteralExp(false)));
        }
        if (breakOut) {
            thenStm.add(newBreak());
        }


        return newIf(statusErrorExpressions(status.clone(), statusCodes), newABlockStm(thenStm), null);
    }

    public SPrimitiveTypeBase fmiTypeToMablType(Fmi2ModelDescription.Types type) {
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

    public PStm createSetupExperimentStatement(String instanceName, boolean toleranceDefined, double tolerance, boolean stopTimeDefined) {
        return newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(statusVariable)),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("setupExperiment").clone(), new ArrayList<>(
                                Arrays.asList(newABoolLiteralExp(toleranceDefined), newARealLiteralExp(tolerance), this.startTime.clone(),
                                        newABoolLiteralExp(stopTimeDefined), this.endTime.clone())))

        );
    }

    public PStm exitInitializationMode(String instanceName) {
        return newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(statusVariable)),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("exitInitializationMode").clone(), null));
    }

    public List<PStm> createFixedPointIteration(List<Fmi2SimulationEnvironment.Variable> loopVariables, int iterationMax, int sccNumber,
            Fmi2SimulationEnvironment env) throws ExpandException {
        LexIdentifier end = newAIdentifier(String.format("end%d", sccNumber));
        LexIdentifier start = newAIdentifier(String.format("start%d", sccNumber));
        List<PStm> statements = new Vector<>();

        statements
                .add(newALocalVariableStm(newAVariableDeclaration(start, newARealNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)))));
        statements.add(newALocalVariableStm(
                newAVariableDeclaration(end, newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(iterationMax)))));
        var outputs = loopVariables.stream().filter(o -> o.scalarVariable.getScalarVariable().causality == Fmi2ModelDescription.Causality.Output &&
                o.scalarVariable.scalarVariable.getType().type == Fmi2ModelDescription.Types.Real).collect(Collectors.toList());

        for (Fmi2SimulationEnvironment.Variable output : outputs) {
            var lexIdentifier = createLexIdentifier
                    .apply("Ref" + output.scalarVariable.instance.getText() + output.scalarVariable.scalarVariable.getName() + "ValueRef" +
                            output.scalarVariable.scalarVariable.getValueReference());
            statements.add(createReferenceArray(output, lexIdentifier));
            //Create map to test references against
            convergenceRefArray.put(output, lexIdentifier);
        }

        LexIdentifier doesConverge = new LexIdentifier("DoesConverge", null);
        statements.add(newALocalVariableStm(
                newAVariableDeclaration(doesConverge, newABoleanPrimitiveType(), newAExpInitializer(newABoolLiteralExp(true)))));
        List<PStm> loopStmts = new Vector<>();

        loopStmts.addAll(performLoopActions(loopVariables, env));

        //Check for convergence
        loopStmts.addAll(checkLoopConvergence(outputs, doesConverge));

        loopStmts.add(newIf(newAnd(newNot(newAIdentifierExp(doesConverge)), newEqual(newAIdentifierExp(start), newAIdentifierExp(end))), newABlockStm(
                Arrays.asList(
                        newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")), newABoolLiteralExp(false)),
                        newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"), Arrays.asList(newAIntLiteralExp(4),
                                newAStringLiteralExp("The initialization of the system was not possible since loop is not converging")))))), null));

        loopStmts.addAll(updateReferenceArray(outputs));

        //Perform next iteration
        loopStmts.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) start.clone()),
                newPlusExp(newAIdentifierExp((LexIdentifier) start.clone()), newAIntLiteralExp(1))));

        //solve problem by updating values
        statements.add(newWhile(newALessEqualBinaryExp(newAIdentifierExp(start), newAIdentifierExp(end)), newABlockStm(loopStmts)));

        return statements;
    }

    private List<PStm> performLoopActions(List<Fmi2SimulationEnvironment.Variable> loopVariables, Fmi2SimulationEnvironment env) {
        List<PStm> LoopStatements = new Vector<>();
        loopVariables.stream().forEach(variable -> {
            long[] scalarValueIndices = new long[]{variable.scalarVariable.scalarVariable.valueReference};

            //All members of the same set has the same causality, type and comes from the same instance
            if (variable.scalarVariable.scalarVariable.causality == Fmi2ModelDescription.Causality.Output) {
                var lexId = createLexIdentifier.apply(variable.scalarVariable.instance + variable.scalarVariable.getScalarVariable().name);
                LoopStatements.add(newALocalVariableStm(
                        newAVariableDeclaration(lexId, newAArrayType(fmiTypeToMablType(variable.scalarVariable.scalarVariable.getType().type)), 1,
                                null)));
                loopValueArray.put(variable, lexId);
                try {
                    LoopStatements.addAll(getValueStm(variable.scalarVariable.instance.getText(), lexId, scalarValueIndices,
                            variable.scalarVariable.scalarVariable.getType().type));
                } catch (ExpandException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    LoopStatements.addAll(setValueOnPortStm(variable.scalarVariable.instance, variable.scalarVariable.scalarVariable.getType().type,
                            Collections.singletonList(variable.scalarVariable.scalarVariable), scalarValueIndices, env));
                } catch (ExpandException e) {
                    e.printStackTrace();
                }
            }
        });

        return LoopStatements;
    }

    public List<PStm> setValueOnPortStm(LexIdentifier comp, Fmi2ModelDescription.Types type, List<Fmi2ModelDescription.ScalarVariable> variables,
            long[] scalarValueIndices, Fmi2SimulationEnvironment env) throws ExpandException {
        ComponentInfo componentInfo = env.getUnitInfo(comp, Framework.FMI2);
        ModelConnection.ModelInstance modelInstances = new ModelConnection.ModelInstance(componentInfo.fmuIdentifier, comp.getText());

        if (type == Fmi2ModelDescription.Types.Boolean) {
            return setBooleansStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(getValues(variables, modelInstances)).map(Boolean.class::cast).collect(BooleanUtils.TO_BOOLEAN_ARRAY));
        } else if (type == Fmi2ModelDescription.Types.Real) {
            return setRealsStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(getValues(variables, modelInstances)).mapToDouble(o -> Double.parseDouble(o.toString())).toArray());
        } else if (type == Fmi2ModelDescription.Types.Integer) {
            return setIntegersStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(getValues(variables, modelInstances)).mapToInt(o -> Integer.parseInt(o.toString())).toArray());
        } else if (type == Fmi2ModelDescription.Types.String) {
            return setStringsStm(comp.getText(), scalarValueIndices,
                    Arrays.stream(getValues(variables, modelInstances)).map(o -> o.toString()).toArray(String[]::new));
        } else {
            throw new ExpandException("Unrecognised type: " + type.name());
        }
    }

    private PStm createReferenceArray(Fmi2SimulationEnvironment.Variable variable, LexIdentifier lexID) throws ExpandException {
        List<PExp> args = new ArrayList<>();
        args.add(getDefaultArrayValue(variable.scalarVariable.scalarVariable.getType().type));

        PInitializer initializer = MableAstFactory.newAArrayInitializer(args);
        return newALocalVariableStm(
                newAVariableDeclaration(lexID, newAArrayType(fmiTypeToMablType(variable.scalarVariable.scalarVariable.getType().type)), 1,
                        initializer));
    }

    private List<PStm> updateReferenceArray(List<Fmi2SimulationEnvironment.Variable> outputPorts) {
        List<PStm> updateStmts = new Vector<>();
        outputPorts.forEach(o -> {
            var referenceValue = convergenceRefArray.get(o);
            var currentValue = loopValueArray.get(o);
            //TODO
            //updateStmts.add(newAAssignmentStm((referenceValue), newAIdentifierExp(currentValue));
        });
        return updateStmts;
    }

    //This method should check if all output of the Fixed Point iteration have stabilized/converged
    private List<PStm> checkLoopConvergence(List<Fmi2SimulationEnvironment.Variable> outputPorts, LexIdentifier doesConverge) {
        LexIdentifier index = newAIdentifier("index");
        List<PStm> result = new Vector<>();
        result.add(newALocalVariableStm(newAVariableDeclaration(index, newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)))));
        outputPorts.forEach(o -> {
            List<PStm> convergenceLoop = new Vector<>();
            var referenceValue = convergenceRefArray.get(o);
            var currentValue = loopValueArray.get(o);
            //find number of places in array
            var arraySize = newAIntLiteralExp(0);

            convergenceLoop.add(newIf(
                    newACallExp(newAIdentifierExp(createLexIdentifier.apply("Math")), (LexIdentifier) createLexIdentifier.apply("isClose").clone(),
                            new ArrayList<>(Arrays.asList(
                                    newAArrayIndexExp(newAIdentifierExp(currentValue), Collections.singletonList(newAIdentifierExp(index))),
                                    newAArrayIndexExp(newAIdentifierExp(referenceValue), Collections.singletonList(newAIdentifierExp(index))),
                                    newARealLiteralExp(this.absoluteTolerance), newARealLiteralExp(this.relativeTolerance)))), null,
                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) doesConverge.clone()), newABoolLiteralExp(false))));

            convergenceLoop.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) index.clone()),
                    newPlusExp(newAIdentifierExp((LexIdentifier) index.clone()), newAIntLiteralExp(1))));

            result.add(newWhile(newAnd(newALessEqualBinaryExp(newAIdentifierExp(index), arraySize), newNot(newAIdentifierExp(doesConverge))),
                    newABlockStm(convergenceLoop)));

        });

        result.add(newIf(newAIdentifierExp(doesConverge), newBreak(), null));

        return result;
    }

    public PStm enterInitializationMode(String instanceName) {
        return newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(statusVariable)),
                newACallExp(newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply("enterInitializationMode").clone(), null));
    }

    private PStm generateAssignmentStm(String instanceName, long[] longs, LexIdentifier valueArray, LexIdentifier valRefs, String setCommand) {
        return MableAstFactory.newAAssignmentStm(MableAstFactory.newAIdentifierStateDesignator(newAIdentifier(statusVariable)),
                newACallExp(MableAstFactory.newAIdentifierExp(createLexIdentifier.apply(instanceName)),
                        (LexIdentifier) createLexIdentifier.apply(setCommand).clone(), new ArrayList<PExp>(
                                Arrays.asList(MableAstFactory.newAIdentifierExp(valRefs), MableAstFactory.newAUIntLiteralExp((long) longs.length),
                                        MableAstFactory.newAIdentifierExp(valueArray)))));
    }

    private Pair<LexIdentifier, List<PStm>> createArray(int valueLength, IntFunction<Pair<PExp, List<PStm>>> valueLocator, String arrayName,
            Fmi2ModelDescription.Types type, Map<Integer, LexIdentifier> array) {
        List<PStm> statements = new Vector<>();

        List<PExp> args = new ArrayList<>();
        for (int i = 0; i < valueLength; i++) {
            Pair<PExp, List<PStm>> value = valueLocator.apply(i);
            args.add(value.getLeft());
            if (value.getRight() != null) {
                statements.addAll(value.getRight());
            }
        }

        PInitializer initializer = MableAstFactory.newAArrayInitializer(args);
        var arType = MableAstFactory.newAArrayType(fmiTypeToMablType(type));
        LexIdentifier lexID = createLexIdentifier.apply(arrayName);
        PStm stm = newALocalVariableStm(newAVariableDeclaration(lexID, arType, args.size(), initializer));
        statements.add(stm);
        array.put(args.size(), lexID);

        return Pair.of(lexID, statements);
    }

    private List<PStm> assignValueToArray(int valueLength, IntFunction<Pair<PExp, List<PStm>>> valueLocator, LexIdentifier valueArray) {
        List<PStm> statements = new Vector<>();
        for (int i = 0; i < valueLength; i++) {
            Pair<PExp, List<PStm>> value = valueLocator.apply(i);
            if (value.getRight() != null) {
                statements.addAll(value.getRight());
            }
            PStm stm = newAAssignmentStm(newAArayStateDesignator(newAIdentifierStateDesignator(valueArray), newAIntLiteralExp(i)), value.getLeft());
            statements.add(stm);
        }
        return statements;
    }

    public void setInputOutputMapping(
            Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, Fmi2ModelDescription.ScalarVariable>>> inputOutputMapping) {
        inputOutputMapping.forEach(this.inputToOutputMapping::put);
    }

    private Pair<LexIdentifier, List<PStm>> findOrCreateValueReferenceArrayAndAssign(long[] valRefs) {
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

    private List<PStm> updateInstanceVariables(String instanceName, long[] longs, LexIdentifier valueArray, Fmi2ModelDescription.Types fmiType) {
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
                        newAVariableDeclaration(createLexIdentifier.apply(id), fmiTypeToMablType(fmiType), newAExpInitializer(assignmentExpression)));
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
            IntFunction<PExp> literalExp, Fmi2ModelDescription.Types targetType) {
        IntFunction<Pair<PExp, List<PStm>>> valueLocator = null;
        if (this.instancesLookupDependencies) {
            Optional<ModelConnection.ModelInstance> key =
                    this.inputToOutputMapping.keySet().stream().filter(x -> x.instanceName.equals(instanceName)).findFirst();

            if (key.isPresent()) {
                Map<Fmi2ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, Fmi2ModelDescription.ScalarVariable>>
                        svInputsToOutputs = this.inputToOutputMapping.get(key.get());
                valueLocator = i -> {
                    Optional<Fmi2ModelDescription.ScalarVariable> svInputVarToOutput =
                            svInputsToOutputs.keySet().stream().filter(x -> x.valueReference == valRefs[i]).findFirst();

                    if (svInputVarToOutput.isPresent()) {
                        AbstractMap.SimpleEntry<ModelConnection.ModelInstance, Fmi2ModelDescription.ScalarVariable> output =
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
                                        newALocalVariableStm(newAVariableDeclaration(new LexIdentifier(name, null), fmiTypeToMablType(targetType)));
                                statements.add(stm);
                            }

                            // Convert the value
                            statements.add(newExpressionStm(newACallExp(newExpandToken(), newAIdentifierExp("TypeConverter"), newAIdentifier(
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

    public List<PStm> setRealsStm(String instanceName, long[] longs, double[] doubles) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> MableAstFactory.newARealLiteralExp(doubles[i]),
                        Fmi2ModelDescription.Types.Real);
        List<PStm> statements = new Vector<>();
        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(realArrays, longs.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            var value = createArray(doubles.length, valueLocator, realArrayVariableName.apply(longs.length), Fmi2ModelDescription.Types.Real, realArrays);
            valueArray = value.getLeft();
            statements.addAll(value.getRight());
        } else {
            // The array exists. Assign its values.
            statements.addAll(assignValueToArray(doubles.length, valueLocator, valueArray));
        }

        var valRefs = findOrCreateValueReferenceArrayAndAssign(longs);
        statements.addAll(valRefs.getRight());

        statements.addAll(generateAssignmentStmForSet(instanceName, longs, valueArray, valRefs.getLeft(), "setReal"));
        return statements;
    }

    public List<PStm> setBooleansStm(String instanceName, long[] longs, boolean[] booleans) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> newABoolLiteralExp(booleans[i]), Fmi2ModelDescription.Types.Boolean);
        List<PStm> statements = new Vector<>();

        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(boolArrays, booleans.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            var value = createArray(booleans.length, valueLocator, booleanArrayVariableName.apply(longs.length), Fmi2ModelDescription.Types.Boolean,
                    boolArrays);
            valueArray = value.getLeft();
            statements.addAll(value.getRight());
        } else {
            // The array exists. Assign its values.
            statements.addAll(assignValueToArray(booleans.length, valueLocator, valueArray));
        }

        var valRefs = findOrCreateValueReferenceArrayAndAssign(longs);
        statements.addAll(valRefs.getRight());

        statements.addAll(generateAssignmentStmForSet(instanceName, longs, valueArray, valRefs.getLeft(), "setBoolean"));
        return statements;
    }

    public List<PStm> setIntegersStm(String instanceName, long[] longs, int[] ints) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> MableAstFactory.newAIntLiteralExp(ints[i]),
                        Fmi2ModelDescription.Types.Integer);
        List<PStm> statements = new Vector<>();
        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(intArrays, longs.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            var value = createArray(ints.length, valueLocator, intArrayVariableName.apply(longs.length), Fmi2ModelDescription.Types.Integer, intArrays);
            valueArray = value.getLeft();
            statements.addAll(value.getRight());
        } else {
            // The array exists. Assign its values.
            statements.addAll(assignValueToArray(ints.length, valueLocator, valueArray));
        }

        var valRefs = findOrCreateValueReferenceArrayAndAssign(longs);
        statements.addAll(valRefs.getRight());
        statements.addAll(generateAssignmentStmForSet(instanceName, longs, valueArray, valRefs.getLeft(), "setInteger"));
        return statements;
    }

    public List<PStm> setStringsStm(String instanceName, long[] longs, String[] strings) {
        // Create a valueLocator to locate the value corresponding to a given valuereference.
        IntFunction<Pair<PExp, List<PStm>>> valueLocator =
                generateInstanceVariablesValueLocator(instanceName, longs, i -> newAStringLiteralExp(strings[i]), Fmi2ModelDescription.Types.String);
        List<PStm> statements = new Vector<>();
        // Create the array to contain the values
        LexIdentifier valueArray = findArrayOfSize(stringArrays, strings.length);
        // The array does not exist. Create it and initialize it.
        if (valueArray == null) {
            var value = createArray(strings.length, valueLocator, stringArrayVariableName.apply(longs.length), Fmi2ModelDescription.Types.String,
                    stringArrays);
            valueArray = value.getLeft();
            statements.addAll(value.getRight());
        } else {
            // The array exists. Assign its values.
            statements.addAll(assignValueToArray(strings.length, valueLocator, valueArray));
        }

        var valRefs = findOrCreateValueReferenceArrayAndAssign(longs);
        statements.addAll(valRefs.getRight());

        statements.addAll(generateAssignmentStmForSet(instanceName, longs, valueArray, valRefs.getLeft(), "setString"));

        return statements;
    }

    public List<PStm> generateAssignmentStmForSet(String instanceName, long[] longs, LexIdentifier valueArray, LexIdentifier valRefs,
            String setCommand) {
        List<PStm> pstms = new ArrayList<>();
        pstms.add(generateAssignmentStm(instanceName, longs, valueArray, valRefs, setCommand));
        pstms.add(statusCheck(newAIdentifierExp(newAIdentifier(statusVariable)), FMIWARNINGANDFATALERRORCODES, "set failed", true, true));
        return pstms;

    }

    // Input:
    // A variable that is compared to a test


    //    public void test() {
    //        BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus = (inLoopAndMessage, list) -> {
    //            List<PStm> body = new Vector<>(Arrays.asList(newExpressionStm(
    //                    call("logger", "log", newAIntLiteralExp(4), newAStringLiteralExp(inLoopAndMessage.getValue() + " %d "),
    //                            arrayGet(fixedStepStatus, newAIdentifierExp((LexIdentifier) compIndexVar.clone())))),
    //                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)),
    //                            newABoolLiteralExp(false))));
    //
    //            if (inLoopAndMessage.getKey()) {
    //                body.add(newBreak());
    //            }
    //
    //            list.getValue().add(newIf(newOr((newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_ERROR))),
    //                    (newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_FATAL)))), newABlockStm(body), null));
    //        };
    //    }

    public PStm generateIfConditionForSetGet() {
        PExp orExp = statusErrorExpressions(newAIdentifierExp(newAIdentifier(statusVariable)), FMIWARNINGANDFATALERRORCODES);
        return newIf(orExp, newABlockStm(
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)), newABoolLiteralExp(false)),
                newBreak()), null);

    }

    public Object[] getValues(List<Fmi2ModelDescription.ScalarVariable> variables, ModelConnection.ModelInstance modelInstance) {
        Object[] values = new Object[variables.size()];
        var i = 0;
        for (Fmi2ModelDescription.ScalarVariable v : variables) {
            values[i++] = getNewValue(v, modelInstance);
        }
        return values;
    }

    private Object getNewValue(Fmi2ModelDescription.ScalarVariable sv, ModelConnection.ModelInstance comp) {
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
        if (sv.type.type == Fmi2ModelDescription.Types.Real) {
            if (newVal instanceof Integer) {
                newVal = (double) (int) newVal;
            }
        }

        return newVal;
    }

    public List<PStm> getValueStm(String instanceName, LexIdentifier valueArray, long[] longs, Fmi2ModelDescription.Types type) throws ExpandException {
        if (!instancesLookupDependencies) {
            instancesLookupDependencies = true;
        }
        List<PStm> result = new Vector<>();

        // Create the value array
        if (valueArray == null) {
            valueArray = findArrayOfSize(getArrayMapOfType(type), longs.length);
        }

        // The array does not exist. Create it
        if (valueArray == null) {
            var arType = newAArrayType(fmiTypeToMablType(type));
            valueArray = createLexIdentifier.apply(type.name() + "ValueSize" + longs.length);
            PStm stm = newALocalVariableStm(newAVariableDeclaration(valueArray, arType, longs.length, null));
            result.add(stm);
            getArrayMapOfType(type).put(longs.length, valueArray);
        }

        // Create the valRefArray
        var valRefs = findOrCreateValueReferenceArrayAndAssign(longs);

        result.addAll(valRefs.getRight());

        result.addAll(Arrays.asList(
                createGetSVsStatement(instanceName, "get" + type.name(), longs, valueArray, valRefs.getLeft(), newAIdentifier(statusVariable)),
                statusCheck(newAIdentifierExp(newAIdentifier(statusVariable)), FMIWARNINGANDFATALERRORCODES, "get failed", true, true)));


        // Update instanceVariables
        result.addAll(updateInstanceVariables(instanceName, longs, (LexIdentifier) valueArray.clone(), type));
        return result;
    }

    //status expression
    // besked

    public Object test() {


        return null;
    }

    private PExp getDefaultArrayValue(Fmi2ModelDescription.Types type) throws ExpandException {
        if (type == Fmi2ModelDescription.Types.Boolean) {
            return newABoolLiteralExp(false);
        } else if (type == Fmi2ModelDescription.Types.Real) {
            return newARealLiteralExp(0.0);
        } else if (type == Fmi2ModelDescription.Types.Integer) {
            return newAIntLiteralExp(0);
        } else {
            throw new ExpandException("Unknown type");
        }
    }

    private Map<Integer, LexIdentifier> getArrayMapOfType(Fmi2ModelDescription.Types type) throws ExpandException {
        if (type == Fmi2ModelDescription.Types.Boolean) {
            return boolArrays;
        } else if (type == Fmi2ModelDescription.Types.Real) {
            return realArrays;
        } else if (type == Fmi2ModelDescription.Types.Integer) {
            return intArrays;
        } else if (type == Fmi2ModelDescription.Types.String) {
            return stringArrays;
        } else {
            throw new ExpandException("Unrecognised type: " + type.name());
        }
    }
}
