package org.intocps.maestro.interpreter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.intocps.fmi.*;
import org.intocps.fmi.jnifmuapi.fmi3.FmuResult;
import org.intocps.fmi.jnifmuapi.fmi3.*;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.Fmu3Value;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentStateValue;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.interpreter.Fmi2Interpreter.*;

public class Fmi3Interpreter {
    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);
    private final File workingDirectory;

    public Fmi3Interpreter(File workingDirectory) {

        this.workingDirectory = workingDirectory;
    }

    public static Map<String, Value> createFmuMembers(File workingDirectory, String guid, IFmi3Fmu fmu) {
        Map<String, Value> functions = new HashMap<>();

        /*
        *
        * FMI3Instance instantiateCoSimulation(string instanceName, string instantiationToken, string resourceLocation, bool visible, bool loggingOn,
            bool eventModeUsed, bool earlyReturnAllowed, uint[] requiredIntermediateVariables); //, ILogMessageCallback logMessage,IIntermediateUpdateCallback intermediateUpdate

FMI2Component instantiateCoSimulationWrapAsFmi2(string instanceName, string instantiationToken, string resourceLocation, bool visible, bool loggingOn,
            bool eventModeUsed, bool earlyReturnAllowed, uint[] requiredIntermediateVariables);//, ILogMessageCallback logMessage,IIntermediateUpdateCallback intermediateUpdate

        *
        * */


        functions.put("instantiateCoSimulation", new FunctionValue.ExternalFunctionValue(fargs -> {

            Fmi2Interpreter.checkArgLength(fargs, 3);

            String name = Fmi2Interpreter.getString(fargs.get(0));
            boolean visible = Fmi2Interpreter.getBool(fargs.get(1));
            boolean logginOn = Fmi2Interpreter.getBool(fargs.get(2));

            try {

                long startInstantiateTime = System.nanoTime();

                logger.debug(String.format("Loading native FMU. GUID: %s, NAME: %s", "" + guid, "" + name));

                BufferedOutputStream fmuLogOutputStream =
                        workingDirectory == null ? null : new BufferedOutputStream(new FileOutputStream(new File(workingDirectory, name + ".log")));

                final String formatter = "{} {} {} {}";
                String pattern = "%d{ISO8601} %-5p - %m%n";

                Layout layout = PatternLayout.newBuilder().withPattern(pattern).withCharset(StandardCharsets.UTF_8).build();//
                boolean eventModeUsed = false;
                boolean earlyReturnAllowed = false;
                long[] requiredIntermediateVariables = new long[0];
                IFmi3Instance instance =
                        fmu.instantiateCoSimulation(name, guid, visible, logginOn, eventModeUsed, earlyReturnAllowed, requiredIntermediateVariables,
                                (instanceName, status, category, message) -> {
                                    logger.info("NATIVE: instance: '{}', status: '{}', category: '{}', message: {}", instanceName, status, category,
                                            message);
                                    {

                                        if (fmuLogOutputStream == null) {
                                            return;
                                        }

                                        Log4jLogEvent.Builder builder = Log4jLogEvent.newBuilder()
                                                .setMessage(new ParameterizedMessage(formatter, category, status, instanceName, message));


                                        switch (status) {
                                            case OK:
                                            case Discard:
                                                builder.setLevel(Level.INFO);
                                                break;
                                            case Error:
                                            case Fatal:
                                                builder.setLevel(Level.ERROR);
                                            case Warning:
                                                builder.setLevel(Level.WARN);
                                                break;
                                            default:
                                                builder.setLevel(Level.TRACE);
                                                break;
                                        }

                                        try {
                                            Log4jLogEvent event = builder.build();
                                            fmuLogOutputStream.write(layout.toByteArray(event));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                (instanceEnvironment, intermediateUpdateTime, clocksTicked, intermediateVariableSetRequested, intermediateVariableGetAllowed, intermediateStepFinished, canReturnEarly) -> new IIntermediateUpdateCallback.IntermediateUpdateResponse(
                                        false, intermediateUpdateTime));


                if (instance == null) {
                    logger.debug("Instance instantiate failed");
                    return new NullValue();
                }
                long stopInstantiateTime = System.nanoTime();
                System.out.println("Interpretation instantiate took: " + (stopInstantiateTime - startInstantiateTime));

                return getFmuInstanceValue(fmuLogOutputStream, instance);


            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }));


        functions.put("freeInstance", new FunctionValue.ExternalFunctionValue(fargs -> {

            fargs = fargs.stream().map(Value::deref).collect(Collectors.toList());

            logger.debug("freeInstance");

            if (fargs.size() != 1) {
                throw new InterpreterException("Too few arguments");
            }

            if (!(fargs.get(0) instanceof FmuComponentValue)) {
                throw new InterpreterException("Argument must be an external module reference");
            }

            FmuComponentValue component = (FmuComponentValue) fargs.get(0);

            try {
                OutputStream loggerOutputStream = component.getFmuLoggerOutputStream();
                if (loggerOutputStream != null) {
                    loggerOutputStream.close();
                }
                component.getModule().freeInstance();
            } catch (IOException | FmuInvocationException e) {
                e.printStackTrace();
            }


            return new VoidValue();
        }));

        functions.put("unload", new FunctionValue.ExternalFunctionValue(fargs -> {
            fargs = fargs.stream().map(Value::deref).collect(Collectors.toList());

            logger.debug("unload");

            if (fargs.size() != 0) {
                throw new InterpreterException("Too many arguments");
            }

            try {
                fmu.unLoad();
            } catch (FmuInvocationException e) {
                e.printStackTrace();
            }

            return new VoidValue();
        }));
        return functions;
    }

    private static Value getFmuInstanceValue(BufferedOutputStream fmuLogOutputStream, IFmi3Instance instance) throws NoSuchMethodException {

        //populate component functions


        Map<String, Value> componentMembers = new HashMap<>(createGetSetMembers(instance));

        //void methods
        componentMembers.put("freeInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 0);
            try {
                instance.freeInstance();
                return new VoidValue();
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        String[] voidStatusMethods =
                new String[]{"exitInitializationMode", "enterEventMode", "terminate", "reset", "enterConfigurationMode", "exitConfigurationMode",
                        "evaluateDiscreteStates", "enterContinuousTimeMode", "enterStepMode"};

        for (String methodName : voidStatusMethods) {
            Method method = componentMembers.getClass().getMethod(methodName);

            componentMembers.put(methodName, new FunctionValue.ExternalFunctionValue(fcargs -> {

                checkArgLength(fcargs, 0);


                try {
                    Fmi3Status res = (Fmi3Status) method.invoke(instance);
                    return status2IntValue(res);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new InterpreterException(e);
                }
            }));

        }

        //        componentMembers.put("setDebugLogging", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //
        //            boolean debugLogginOn = getBool(fcargs.get(0));
        //            List<StringValue> categories = getArrayValue(fcargs.get(2), StringValue.class);
        //
        //            try {
        //                Fmi3Status res = instance.setDebugLogging(debugLogginOn, categories.stream().map(StringValue::getValue).toArray(String[]::new));
        //                return status2IntValue(res);
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));


        /*

int setDebugLogging(bool loggingOn, string categories[]);
int enterInitializationMode(bool toleranceDefined, real tolerance, real startTime, bool stopTimeDefined,

int getBinary(uint valueReferences[], int nValueReferences, byte values[][], int nValues);
int setBinary(uint valueReferences[], int nValueReferences, long sizes[], byte array_name[][], int nValues);
int getNumberOfVariableDependencies(uint valueReference, out int nDependencies);
int getVariableDependencies(uint dependent, int elementIndicesOfDependent[], uint independents[],
int getFMUState(out FMUS3tate FMUState);
int setFMUState(FMUS3tate FMUState);
int freeFMUState(out FMUS3tate  FMUState);
int getDirectionalDerivative(uint unknowns[], int nUnknowns, uint knowns[], int nKnowns, real seed[], int nSeed,
int getAdjointDerivative(uint unknowns[], int nUnknowns, uint knowns[], int nKnowns, real seed[], int nSeed,
int getClock(uint valueReferences[], int nValueReferences, bool values[]);
int setClock(uint valueReferences[], int nValueReferences, bool values[]);
int getIntervalDecimal(uint valueReferences[], int nValueReferences, real intervals[],
int getIntervalFraction(uint valueReferences[], int nValueReferences, uint intervalCounters[], uint resolutions[],
int getShiftDecimal(uint valueReferences[], int nValueReferences, real shifts[]);
int getShiftFraction(uint valueReferences[], int nValueReferences, uint shiftCounters[], uint resolutions[]);
int setIntervalDecimal(uint valueReferences[], int nValueReferences, real interval[]);
int setIntervalFraction(uint valueReferences[], int nValueReferences, uint intervalCounter[], uint resolution[]);
int setShiftDecimal(uint valueReferences[], int nValueReferences, real shifts[]);
int setShiftFraction(uint valueReferences[], int nValueReferences, uint counters[], uint resolutions[]);
int updateDiscreteStates(bool[] discreteStatesNeedUpdate, bool[] terminateSimulation,
int completedIntegratorStep(bool nosetFMUStatePriorToCurrentPoint, bool[] enterEventMode,
int setTime(real time);
int setContinuousStates(real continuousStates[], int nContinuousStates);
int getContinuousStateDerivatives(real derivatives[], int nContinuousStates);
int getEventIndicators(real eventIndicators[], int nEventIndicators);
int getContinuousStates(real continuousStates[], int nContinuousStates);
int getNominalsOfContinuousStates(real nominals[], int nContinuousStates);
int getNumberOfEventIndicators(int[] nEventIndicators);
int getNumberOfContinuousStates(int[] nContinuousStates);
int getOutputDerivatives(uint valueReferences[], int nValueReferences, int orders[], real values[], int nValues);
int doStep(real currentCommunicationPoint, real communicationStepSize, bool nosetFMUStatePriorToCurrentPoint,
int activateModelPartition(uint clockReference, real activationTime);

        */


        componentMembers.put("setDebugLogging", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            boolean debugLogginOn = getBool(fcargs.get(0));
            //                        int arraySize = getInteger(fcargs.get(1));
            List<StringValue> categories = getArrayValue(fcargs.get(2), StringValue.class);

            try {
                Fmi3Status res = instance.setDebugLogging(debugLogginOn, categories.stream().map(StringValue::getValue).toArray(String[]::new));
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));

        componentMembers.put("setupExperiment", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 5);

            boolean toleranceDefined = getBool(fcargs.get(0));
            double tolerance = getDouble(fcargs.get(1));
            double startTime = getDouble(fcargs.get(2));
            boolean stopTimeDefined = getBool(fcargs.get(3));
            double stopTime = getDouble(fcargs.get(4));
            try {
                Fmi2Status res = instance.setupExperiment(toleranceDefined, tolerance, startTime, stopTimeDefined, stopTime);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));
        componentMembers.put("enterInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = instance.enterInitializationMode();
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));
        componentMembers.put("exitInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = instance.exitInitializationMode();
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));
        componentMembers.put("setReal", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 3);
            long elementsToUse = getUint(fcargs.get(1));
            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();
            double[] values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().limit(elementsToUse)
                    .mapToDouble(NumericValue::realValue).toArray();

            try {
                Fmi2Status res = instance.setReals(scalarValueIndices, values);
                return new IntegerValue(res.value);
            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                throw new InterpreterException(e);
            }

        }));
        componentMembers.put("getReal", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            if (!(fcargs.get(2) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }
            long elementsToUse = getUint(fcargs.get(1));

            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();


            try {
                org.intocps.fmi.FmuResult<double[]> res = instance.getReal(scalarValueIndices);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);

                    List<RealValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(d -> new RealValue(d))
                            .collect(Collectors.toList());

                    ref.setValue(new ArrayValue<>(values));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));
        componentMembers.put("setInteger", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 3);
            long elementsToUse = getUint(fcargs.get(1));
            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();
            int[] values =
                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), IntegerValue.class).stream().mapToInt(IntegerValue::getValue).toArray();

            try {
                Fmi2Status res = instance.setIntegers(scalarValueIndices, values);
                return new IntegerValue(res.value);
            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                throw new InterpreterException(e);
            }
        }));
        componentMembers.put("getInteger", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);
            long elementsToUse = getUint(fcargs.get(1));

            if (!(fcargs.get(2) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }

            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();


            try {
                org.intocps.fmi.FmuResult<int[]> res = instance.getInteger(scalarValueIndices);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);

                    List<IntegerValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(i -> new IntegerValue(i))
                            .collect(Collectors.toList());

                    ref.setValue(new ArrayValue<>(values));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));
        componentMembers.put("setBoolean", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);
            long elementsToUse = getUint(fcargs.get(1));
            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();
            boolean[] values = ArrayUtils.toPrimitive(
                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), BooleanValue.class).stream().map(BooleanValue::getValue)
                            .collect(Collectors.toList()).toArray(new Boolean[]{}));

            try {
                Fmi2Status res = instance.setBooleans(scalarValueIndices, values);
                return new IntegerValue(res.value);
            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                throw new InterpreterException(e);
            }
        }));
        componentMembers.put("getBoolean", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            if (!(fcargs.get(2) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }

            long elementsToUse = getUint(fcargs.get(1));


            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();


            try {
                org.intocps.fmi.FmuResult<boolean[]> res = instance.getBooleans(scalarValueIndices);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);

                    List<BooleanValue> values =
                            Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(BooleanValue::new).collect(Collectors.toList());

                    ref.setValue(new ArrayValue<>(values));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));
        componentMembers.put("setString", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);
            long elementsToUse = getUint(fcargs.get(1));

            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();
            String[] values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), StringValue.class).stream().map(StringValue::getValue)
                    .collect(Collectors.toList()).toArray(new String[]{});

            try {
                Fmi2Status res = instance.setStrings(scalarValueIndices, values);
                return new IntegerValue(res.value);
            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                throw new InterpreterException(e);
            }

        }));
        componentMembers.put("getString", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            if (!(fcargs.get(2) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }


            long elementsToUse = getUint(fcargs.get(1));


            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();


            try {
                org.intocps.fmi.FmuResult<String[]> res = instance.getStrings(scalarValueIndices);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);

                    List<StringValue> values = Arrays.stream(res.result).limit(elementsToUse).map(StringValue::new).collect(Collectors.toList());

                    ref.setValue(new ArrayValue<>(values));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));
        componentMembers.put("doStep", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            double currentCommunicationPoint = getDouble(fcargs.get(0));
            double communicationStepSize = getDouble(fcargs.get(1));
            boolean noSetFMUStatePriorToCurrentPoint = getBool(fcargs.get(2));

            try {
                Fmi2Status res = instance.doStep(currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));

        componentMembers.put("terminate", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = instance.terminate();
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));

        componentMembers.put("setState", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value v = fcargs.get(0).deref();

            if (v instanceof FmuComponentStateValue) {
                try {
                    FmuComponentStateValue stateValue = (FmuComponentStateValue) v;
                    Fmi2Status res = instance.setState(stateValue.getModule());
                    return new IntegerValue(res.value);
                } catch (FmuInvocationException e) {
                    throw new InterpreterException(e);
                }
            }

            throw new InterpreterException("Invalid value");
        }));
        componentMembers.put("getState", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            if (!(fcargs.get(0) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }


            try {

                org.intocps.fmi.FmuResult<IFmiComponentState> res = instance.getState();

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(0);
                    ref.setValue(new FmuComponentStateValue(res.result));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));
        componentMembers.put("freeState", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            Value v = fcargs.get(0).deref();

            if (v instanceof FmuComponentStateValue) {
                try {
                    FmuComponentStateValue stateValue = (FmuComponentStateValue) v;
                    Fmi2Status res = instance.freeState(stateValue.getModule());
                    return new IntegerValue(res.value);
                } catch (FmuInvocationException e) {
                    throw new InterpreterException(e);
                }
            }

            throw new InterpreterException("Invalid value");


        }));

        componentMembers.put("getRealStatus", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 2);

            if (!(fcargs.get(1) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }

            Value kindValue = fcargs.get(0).deref();

            if (!(kindValue instanceof IntegerValue)) {
                throw new InterpreterException("Invalid kind value: " + kindValue);
            }

            int kind = ((IntegerValue) kindValue).getValue();

            Fmi2StatusKind kindEnum = Arrays.stream(Fmi2StatusKind.values()).filter(v -> v.value == kind).findFirst().orElse(null);

            try {
                org.intocps.fmi.FmuResult<Double> res = instance.getRealStatus(kindEnum);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(1);

                    ref.setValue(new RealValue(res.result));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));

        componentMembers.put("getRealOutputDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
            //   int getRealOutputDerivatives(long[] scalarValueIndices, UInt nvr, int[] order, ref double[] derivatives);
            checkArgLength(fcargs, 4);

            if (!(fcargs.get(3) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }

            long elementsToUse = getUint(fcargs.get(1));


            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();

            int[] orders =
                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().mapToInt(NumericValue::intValue).toArray();

            try {
                FmuResult<double[]> res = instance.getRealOutputDerivatives(scalarValueIndices, orders);

                if (res.status == Fmi2Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(3);

                    List<RealValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(d -> new RealValue(d))
                            .collect(Collectors.toList());

                    ref.setValue(new ArrayValue<>(values));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));

        componentMembers.put("setRealInputDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int setRealInputDerivatives(UInt[] scalarValueIndices, UInt nvr, int[] order, ref real[] derivatives);
            checkArgLength(fcargs, 4);
            long elementsToUse = getUint(fcargs.get(1));


            long[] scalarValueIndices =
                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                            .toArray();

            int[] orders =
                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().mapToInt(NumericValue::intValue).toArray();

            double[] values =
                    getArrayValue(fcargs.get(3), Optional.of(elementsToUse), RealValue.class).stream().mapToDouble(RealValue::getValue).toArray();

            try {
                Fmi2Status res = instance.setRealInputDerivatives(scalarValueIndices, orders, values);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        return new FmuComponentValue(componentMembers, component, fmuLogOutputStream);
    }

    private static Value status2IntValue(Fmi3Status res) {
        switch (res) {

            case OK:
                return new IntegerValue(0);
            case Warning:
                return new IntegerValue(1);
            case Discard:
                return new IntegerValue(2);
            case Error:
                return new IntegerValue(3);
            case Fatal:
                return new IntegerValue(4);
        }
        return null;
    }

    private static Map<String, ? extends Value> createGetSetMembers(IFmi3Instance instance) throws NoSuchMethodException {
        Map<String, Value> componentMembers = new HashMap<>();

        Map<String, TP> methodTypeMapping = new HashMap<>() {{
            put("getFloat32", TP.Float);
            put("getFloat64", TP.Real);
            put("getInt8", TP.Byte);
            put("getUInt8", TP.Byte);
            put("getInt16", TP.Short);
            put("getUInt16", TP.Short);
            put("getInt32", TP.Int);
            put("getUInt32", TP.Int);
            put("getInt64", TP.Long);
            put("getUInt64", TP.Long);
            put("getBoolean", TP.Bool);
            put("getString", TP.String);
            put("setFloat32", TP.Float);
            put("setFloat64", TP.Real);
            put("setInt8", TP.Byte);
            put("setUInt8", TP.Byte);
            put("setInt16", TP.Short);
            put("setUInt16", TP.Short);
            put("setInt32", TP.Int);
            put("setUInt32", TP.Int);
            put("setInt64", TP.Long);
            put("setUInt64", TP.Long);
            put("setBoolean", TP.Bool);
            put("setString", TP.String);


        }};


        for (Map.Entry<String, TP> method : methodTypeMapping.entrySet()) {
            if (method.getKey().startsWith("get")) {

                Method m = instance.getClass().getMethod(method.getKey(), long[].class);

                componentMembers.put(method.getKey(), new FunctionValue.ExternalFunctionValue(fcargs -> {

                    checkArgLength(fcargs, 3);

                    if (!(fcargs.get(2) instanceof UpdatableValue)) {
                        throw new InterpreterException("value not a reference value");
                    }
                    long elementsToUse = getUint(fcargs.get(1));

                    long[] scalarValueIndices =
                            getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                    .toArray();


                    try {
                        @SuppressWarnings("rawtypes")
                        FmuResult res = (FmuResult) m.invoke(instance, scalarValueIndices);// instance.getFloat64(scalarValueIndices);

                        if (res.status == Fmi3Status.OK) {
                            UpdatableValue ref = (UpdatableValue) fcargs.get(2);

                            List<Value> values = null;
                            switch (method.getValue()) {

                                case Bool:
                                    values = Arrays.stream(ArrayUtils.toObject((boolean[]) res.result)).limit(elementsToUse).map(BooleanValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Byte:
                                    values = Arrays.stream(ArrayUtils.toObject((byte[]) res.result)).limit(elementsToUse).map(ByteValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Float:
                                    values = Arrays.stream(ArrayUtils.toObject((float[]) res.result)).limit(elementsToUse).map(FloatValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Int:
                                    values = Arrays.stream(ArrayUtils.toObject((int[]) res.result)).limit(elementsToUse).map(IntegerValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Long:
                                    values = Arrays.stream(ArrayUtils.toObject((long[]) res.result)).limit(elementsToUse).map(LongValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Real:
                                    values = Arrays.stream(ArrayUtils.toObject((double[]) res.result)).limit(elementsToUse).map(RealValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case Short:
                                    values = Arrays.stream(ArrayUtils.toObject((short[]) res.result)).limit(elementsToUse).map(ShortValue::new)
                                            .collect(Collectors.toList());
                                    break;
                                case String:
                                    values = Arrays.stream((String[]) res.result).limit(elementsToUse).map(StringValue::new)
                                            .collect(Collectors.toList());
                                    break;
                            }


                            ref.setValue(new ArrayValue<>(values));
                        }

                        return status2IntValue(res.status);

                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new InterpreterException(e);
                    }


                }));

            } else {

                Class<?> argClass = null;

                switch (method.getValue()) {

                    case Bool:
                        argClass = boolean[].class;
                        break;
                    case Byte:
                        argClass = byte[].class;
                        break;
                    case Float:
                        argClass = float[].class;
                        break;
                    case Int:
                        argClass = int[].class;
                        break;
                    case Long:
                        argClass = long[].class;
                        break;
                    case Real:
                        argClass = double[].class;
                        break;
                    case Short:
                        argClass = short[].class;
                        break;
                    case String:
                        argClass = String[].class;
                        break;
                }

                Method m = instance.getClass().getMethod(method.getKey(), argClass);

                componentMembers.put(method.getKey(), new FunctionValue.ExternalFunctionValue(fcargs -> {
                    checkArgLength(fcargs, 3);
                    long elementsToUse = getUint(fcargs.get(1));
                    long[] scalarValueIndices =
                            getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                    .toArray();

                    //extract values
                    Object values = null;

                    switch (method.getValue()) {

                        case Bool:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), BooleanValue.class).stream().map(BooleanValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Boolean[]{}));
                            break;
                        case Byte:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), ByteValue.class).stream().map(ByteValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Byte[]{}));
                            break;
                        case Float:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), FloatValue.class).stream().map(FloatValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Float[]{}));
                            break;
                        case Int:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), IntegerValue.class).stream().map(IntegerValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Integer[]{}));
                            break;
                        case Long:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), LongValue.class).stream().map(LongValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Long[]{}));
                            break;
                        case Real:
                            values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().limit(elementsToUse)
                                    .mapToDouble(NumericValue::realValue).toArray();
                            break;
                        case Short:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), ShortValue.class).stream().map(ShortValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Short[]{}));
                            break;
                        case String:
                            values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), StringValue.class).stream().map(StringValue::getValue)
                                    .collect(Collectors.toList()).toArray(new String[]{});
                            break;
                    }

                    try {
                        Fmi2Status res = (Fmi2Status) m.invoke(instance, scalarValueIndices, values);// instance.setBoolean().setReals
                        // (scalarValueIndices, values);
                        return new IntegerValue(res.value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new InterpreterException(e);
                    }

                }));
            }
        }

        /*
int getFloat32(uint valueReferences[], int nValueReferences, float values[], int nValues);
int getFloat64(uint valueReferences[], int nValueReferences, real values[], int nValues);
int getInt8(uint valueReferences[], int nValueReferences, byte values[], int nValues);
int getUInt8(uint valueReferences[], int nValueReferences, byte values[], int nValues);
int getInt16(uint valueReferences[], int nValueReferences, short values[], int nValues);
int getUInt16(uint valueReferences[], int nValueReferences, short values[], int nValues);
int getInt32(uint valueReferences[], int nValueReferences, int values[], int nValues);
int getUInt32(uint valueReferences[], int nValueReferences, int values[], int nValues);
int getInt64(uint valueReferences[], int nValueReferences, long values[], int nValues);
int getUInt64(uint valueReferences[], int nValueReferences, long values[], int nValues);
int getBoolean(uint valueReferences[], int nValueReferences, bool values[], int nValues);
int getString(uint valueReferences[], int nValueReferences, string values[], int nValues);
int setFloat32(uint valueReferences[], int nValueReferences, float values[], int nValues);
int setFloat64(uint valueReferences[], int nValueReferences, real values[], int nValues);
int setInt8(uint valueReferences[], int nValueReferences, byte values[], int nValues);
int setUInt8(uint valueReferences[], int nValueReferences, byte values[], int nValues);
int setInt16(uint valueReferences[], int nValueReferences, short values[], int nValues);
int setUInt16(uint valueReferences[], int nValueReferences, short values[], int nValues);
int setInt32(uint valueReferences[], int nValueReferences, int values[], int nValues);
int setUInt32(uint valueReferences[], int nValueReferences, int values[], int nValues);
int setInt64(uint valueReferences[], int nValueReferences, long values[], int nValues);
int setUInt64(uint valueReferences[], int nValueReferences, long values[], int nValues);
int setBoolean(uint valueReferences[], int nValueReferences, bool values[], int nValues);
int setString(uint valueReferences[], int nValueReferences, string values[], int nValues);
        */


        return componentMembers;

    }

    public Value createFmiValue(String path, String guid) {
        try {
            long startExecTime = System.nanoTime();

            URI uri = URI.create(path);
            if (!uri.isAbsolute()) {
                uri = new File(".").toURI().resolve(uri);
            }
            File file = new File(uri);

            final IFmi3Fmu fmu = new Fmu3(file);

            fmu.load();

            Map<String, Value> functions = createFmuMembers(workingDirectory, guid, fmu);

            long stopTime = System.nanoTime();

            System.out.println("Interpretation load took: " + (stopTime - startExecTime));
            return new Fmu3Value(functions, fmu);

        } catch (IOException | FmuInvocationException | FmuMissingLibraryException e) {

            e.printStackTrace();
            return new NullValue();
        } catch (Exception e) {
            e.printStackTrace();
            return new NullValue();
        }
    }

    private enum TP {
        Bool,
        Byte,
        Float,
        Int,
        Long,
        Real,
        Short,
        String,
    }
}
