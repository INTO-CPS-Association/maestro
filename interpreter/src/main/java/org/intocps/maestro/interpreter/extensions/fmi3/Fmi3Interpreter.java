package org.intocps.maestro.interpreter.extensions.fmi3;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.jnifmuapi.fmi3.*;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.node.AIntNumericPrimitiveType;
import org.intocps.maestro.ast.node.ANameType;
import org.intocps.maestro.ast.node.AReferenceType;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.interpreter.Fmi2Interpreter;
import org.intocps.maestro.interpreter.Interpreter;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.external.TP;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.Fmu3InstanceValue;
import org.intocps.maestro.interpreter.values.fmi.Fmu3StateValue;
import org.intocps.maestro.interpreter.values.fmi.Fmu3Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.intocps.maestro.interpreter.Fmi2Interpreter.*;
import static org.intocps.maestro.interpreter.extensions.fmi3.Fmi3StatusArgMapping.status2IntValue;

public class Fmi3Interpreter {
    static final ExternalReflectCallHelper.ArgMapping boolArrayOutArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Bool, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Output, null);
    static final ExternalReflectCallHelper.ArgMapping doubleArrayOutArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Real, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Output, null);

    static final ExternalReflectCallHelper.ArgMapping intInArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Int, 1,
            ExternalReflectCallHelper.ArgMapping.InOut.Input, null);
    static final ExternalReflectCallHelper.ArgMapping doubleInArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Real, 1,
            ExternalReflectCallHelper.ArgMapping.InOut.Input, null);

    static final ExternalReflectCallHelper.ArgMapping boolInArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Bool, 1,
            ExternalReflectCallHelper.ArgMapping.InOut.Input, null);

    static final ExternalReflectCallHelper.ArgMapping doubleArrayInArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Real, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Input, null);

    static final ExternalReflectCallHelper.ArgMapping longArrayInArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Long, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Input, null);

    static final ExternalReflectCallHelper.ArgMapping uintArrayOutArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Int, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Output, null);
    static final ExternalReflectCallHelper.ArgMapping longArrayOutArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Long, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Output, null);

    static final ExternalReflectCallHelper.ArgMapping intArrayOutArgMapper = new ExternalReflectCallHelper.ArgMapping(TP.Int, 2,
            ExternalReflectCallHelper.ArgMapping.InOut.Output, null);


    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);
    private final File workingDirectory;
    private final Function<String, AModuleDeclaration> resolver;

    public Fmi3Interpreter(File workingDirectory, Function<String, AModuleDeclaration> resolver) {

        this.workingDirectory = workingDirectory;
        this.resolver = resolver;
    }

    public static Double getDouble(Value value) {

        if (value == null || value instanceof NullValue) {
            return null;
        }

        value = value.deref();

        if (value == null || value instanceof NullValue) {
            return null;
        }
        if (value.isNumeric()) {
            return ((NumericValue) value).doubleValue();
        }
        throw new InterpreterException("Value is not double");
    }

    public static UpdatableValue getUpdatable(Value v) {
        if (!(v instanceof UpdatableValue)) {
            throw new InterpreterException("value not a reference value");
        }
        return (UpdatableValue) v;
    }

    public Map<String, Value> createFmuMembers(File workingDirectory, String guid, IFmi3Fmu fmu) {


        var fmi3Module = resolver.apply("FMI3");
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

            Fmi2Interpreter.checkArgLength(fargs, 6);

            String name = Fmi2Interpreter.getString(fargs.get(0));
            boolean visible = Fmi2Interpreter.getBool(fargs.get(1));
            boolean logginOn = Fmi2Interpreter.getBool(fargs.get(2));

            boolean eventModeUsed = Fmi2Interpreter.getBool(fargs.get(3));
            boolean earlyReturnAllowed = Fmi2Interpreter.getBool(fargs.get(4));
            long[] requiredIntermediateVariables = getArrayValue(fargs.get(5), Optional.empty(), NumericValue.class).stream()
                    .mapToLong(NumericValue::longValue).toArray();

            try {

                long startInstantiateTime = System.nanoTime();

                logger.debug(String.format("Loading native FMU. GUID: %s, NAME: %s", "" + guid, "" + name));

                BufferedOutputStream fmuLogOutputStream = workingDirectory == null ? null : new BufferedOutputStream(
                        new FileOutputStream(new File(workingDirectory, name + ".log")));

                final String formatter = "{} {} {} {}";
                String pattern = "%d{ISO8601} %-5p - %m%n";

                Layout layout = PatternLayout.newBuilder().withPattern(pattern).withCharset(StandardCharsets.UTF_8).build();//

                ILogMessageCallback logCallback = (instanceName, status, category, message) -> {
                    logger.info("NATIVE: instance: '{}', status: '{}', category: '{}', message: {}", instanceName, status, category, message);
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
                };
                IIntermediateUpdateCallback intermediateUpdateCallback = (instanceEnvironment, intermediateUpdateTime, clocksTicked, intermediateVariableSetRequested, intermediateVariableGetAllowed, intermediateStepFinished, canReturnEarly) -> new IIntermediateUpdateCallback.IntermediateUpdateResponse(
                        false, intermediateUpdateTime);
                IFmi3Instance instance = fmu.instantiateCoSimulation(name, guid, visible, logginOn, eventModeUsed, earlyReturnAllowed,
                        requiredIntermediateVariables, logCallback, null);


                if (instance == null) {
                    logger.debug("Instance instantiate failed");
                    return new NullValue();
                }
                long stopInstantiateTime = System.nanoTime();
                System.out.println("Interpretation instantiate took: " + (stopInstantiateTime - startInstantiateTime));

                return getFmuInstanceValue(fmuLogOutputStream, instance, resolver);


            } catch (IOException | NoSuchMethodException e) {
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

            if (!(fargs.get(0) instanceof Fmu3InstanceValue)) {
                throw new InterpreterException("Argument must be an external module reference");
            }

            Fmu3InstanceValue component = (Fmu3InstanceValue) fargs.get(0);

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

        checkRequiredFunctions(fmi3Module, functions);

        return functions;
    }

    static void checkRequiredFunctions(AModuleDeclaration module, Map<String, Value> functions) {
        var expectedFunctions = module.getFunctions().stream().map(f -> f.getName().getText()).collect(Collectors.toSet());

        if (expectedFunctions.size() != functions.size() || !functions.keySet().equals(expectedFunctions)) {
            logger.warn("Runtime type '{}' does not match declaration. Missing: '{}'", module.getName().getText(),
                    expectedFunctions.stream().filter(n -> !functions.containsKey(n)).sorted().collect(Collectors.joining(",\n\t", "\n\t", "")));
        }
    }

    public static Function<ExternalReflectCallHelper.ArgMappingContext, IArgMapping> getFmi3InstanceCustomArgMapper() {

        Set<String> statusRetuningFunctions = Arrays.stream(Fmi3Instance.class.getDeclaredMethods())
                .filter(m -> m.getReturnType().getSimpleName().equals("Fmi3Status")).map(Method::getName).collect(Collectors.toSet());


        Function<ExternalReflectCallHelper.ArgMappingContext, IArgMapping> costumeArgMapper = tCtxt -> {

            PType t = tCtxt.getArgType();
            boolean output = t instanceof AReferenceType;
            if (output) {
                t = ((AReferenceType) t).getType();
            }

            if (tCtxt.getArgType() instanceof AIntNumericPrimitiveType && tCtxt.getArgName() == null && statusRetuningFunctions.contains(
                    tCtxt.getFunctionName())) {
                //ok this is a return of status we need to make it an int
                return new Fmi3StatusArgMapping();
            } else if (t instanceof ANameType) {
                String typeName = ((ANameType) t).getName().getText();
                if (typeName.equals("FMI3Instance")) {
                    return new Fmi3InstanceArgMapping();
                } else if (typeName.equals("FMU3State")) {
                    return new Fmi3StateArgMapping(tCtxt);
                }
            }
            return null;
        };
        return costumeArgMapper;
    }


    private static Value getFmuInstanceValue(BufferedOutputStream fmuLogOutputStream, IFmi3Instance instance,
            Function<String, AModuleDeclaration> resolver) throws NoSuchMethodException {

        //populate component functions
        var module = resolver.apply("FMI3Instance");


        Map<String, Value> functions = new HashMap<>();//createGetSetMembers(instance));

        Predicate<AFunctionDeclaration> functionFilter = fun -> !fun.getName().getText().equals("enterInitializationMode") && !fun.getName().getText()
                .equals("setBinary");

        Set<String> resultRetuningFunctions = Arrays.stream(Fmi3Instance.class.getDeclaredMethods())
                .filter(m -> m.getReturnType().getName().equals(org.intocps.fmi.jnifmuapi.fmi3.FmuResult.class.getName())).map(Method::getName)
                .collect(Collectors.toSet());

        final var costumeArgMapper = getFmi3InstanceCustomArgMapper();


        for (AFunctionDeclaration function : module.getFunctions()) {
            if (functionFilter == null || functionFilter.test(function)) {

                functions.computeIfAbsent(function.getName().getText(), key -> {
                    try {
                        var builder = new ExternalReflectCallHelper(function, instance, costumeArgMapper);
                        if (resultRetuningFunctions.contains(function.getName().getText())) {
                            return handleResultReturns(builder).build();
                        } else {
                            return builder.build();
                        }
                    } catch (NoSuchMethodException | RuntimeException e) {
                        logger.warn("Auto binding faild for: " + e.getMessage());
                        return null;
                    }
                });

            }
        }


        functions.put("enterInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 5);

            boolean toleranceDefined = getBool(fcargs.get(0));
            double tolerance = getDouble(fcargs.get(1));
            double startTime = getDouble(fcargs.get(2));
            boolean stopTimeDefined = getBool(fcargs.get(3));
            Double stopTime = getDouble(fcargs.get(4));


            try {
                Fmi3Status res = instance.enterInitializationMode(toleranceDefined ? tolerance : null, startTime, stopTimeDefined ? stopTime : null);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("completedIntegratorStep", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int completedIntegratorStep(bool nosetFMUStatePriorToCurrentPoint, bool[] enterEventMode,bool[] terminateSimulation);

            checkArgLength(fcargs, 3);
            try {
                FmuResult<IFmi3Instance.CompletedIntegratorStepResponse> res = instance.completedIntegratorStep(
                        (Boolean) boolInArgMapper.map(fcargs.get(0)));
                boolArrayOutArgMapper.mapOut(fcargs.get(1), new boolean[]{res.result.isEnterEventMode()});
                boolArrayOutArgMapper.mapOut(fcargs.get(2), new boolean[]{res.result.isTerminateSimulation()});
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getAdjointDerivative", new FunctionValue.ExternalFunctionValue(fcargs -> {
            //   int getAdjointDerivative(uint unknowns[], int nUnknowns, uint knowns[], int nKnowns, real seed[], int nSeed,
            //        real sensitivity[], int nSensitivity);

            checkArgLength(fcargs, 8);
            try {
                FmuResult<double[]> res = instance.getGetAdjointDerivative((long[]) longArrayInArgMapper.map(fcargs.get(0)),
                        (long[]) longArrayInArgMapper.map(fcargs.get(2)), (double[]) doubleArrayInArgMapper.map(fcargs.get(4)),
                        (Integer) intInArgMapper.map(fcargs.get(5)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(6), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getClock", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getClock(uint valueReferences[], int nValueReferences, bool values[]);

            checkArgLength(fcargs, 3);
            try {
                FmuResult<boolean[]> res = instance.getClock((long[]) longArrayInArgMapper.map(fcargs.get(0)));
                boolArrayOutArgMapper.mapOut(fcargs.get(2), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        functions.put("getContinuousStateDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
            //int getContinuousStateDerivatives(real derivatives[], int nContinuousStates);

            checkArgLength(fcargs, 2);
            try {
                FmuResult<double[]> res = instance.getContinuousStateDerivatives((Integer) intInArgMapper.map(fcargs.get(1)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getContinuousStates", new FunctionValue.ExternalFunctionValue(fcargs -> {
            //int getContinuousStates(real continuousStates[], int nContinuousStates);

            checkArgLength(fcargs, 2);
            try {
                FmuResult<double[]> res = instance.getGetContinuousStates((Integer) intInArgMapper.map(fcargs.get(1)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getDirectionalDerivative", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getDirectionalDerivative(uint unknowns[], int nUnknowns, uint knowns[], int nKnowns, real seed[], int nSeed,
            //                real sensitivity[], int nSensitivity);

            checkArgLength(fcargs, 8);
            try {
                FmuResult<double[]> res = instance.getDirectionalDerivative((long[]) longArrayInArgMapper.map(fcargs.get(0)),
                        (long[]) longArrayInArgMapper.map(fcargs.get(2)), (double[]) doubleArrayInArgMapper.map(fcargs.get(4)));

                doubleArrayOutArgMapper.mapOut(fcargs.get(6), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        functions.put("getEventIndicators", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getEventIndicators(real eventIndicators[], int nEventIndicators);

            checkArgLength(fcargs, 2);
            try {
                FmuResult<double[]> res = instance.getGetEventIndicators((Integer) intInArgMapper.map(fcargs.get(1)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        functions.put("getIntervalDecimal", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getIntervalDecimal(uint valueReferences[], int nValueReferences, real intervals[],
            //                int qualifiers[]);

            checkArgLength(fcargs, 4);
            try {
                FmuResult<IFmi3Instance.GetIntervalDecimalResponse> res = instance.getIntervalDecimal(
                        (long[]) longArrayInArgMapper.map(fcargs.get(0)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(2), res.result.getIntervals());
                doubleArrayOutArgMapper.mapOut(fcargs.get(3), res.result.getQualifiers());
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getIntervalFraction", new FunctionValue.ExternalFunctionValue(fcargs -> {
            //    int getIntervalFraction(uint valueReferences[], int nValueReferences, uint intervalCounters[], uint resolutions[],
            //        int qualifiers[]);

            checkArgLength(fcargs, 5);
            try {
                FmuResult<IFmi3Instance.IntervalFractionResponse> res = instance.getIntervalFraction(
                        (long[]) longArrayInArgMapper.map(fcargs.get(0)));
                uintArrayOutArgMapper.mapOut(fcargs.get(2), res.result.getIntervalCounters());
                uintArrayOutArgMapper.mapOut(fcargs.get(3), res.result.getResolutions());
                intInArgMapper.mapOut(fcargs.get(3), Arrays.stream(res.result.getQualifiers()).map(q -> q.getValue()).toArray());
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getNominalsOfContinuousStates", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getNominalsOfContinuousStates(real nominals[], int nContinuousStates);

            checkArgLength(fcargs, 2);
            try {
                FmuResult<double[]> res = instance.getGetNominalsOfContinuousStates((Integer) intInArgMapper.map(fcargs.get(1)));
                doubleArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getNumberOfContinuousStates", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getNumberOfContinuousStates(int[] nContinuousStates);

            checkArgLength(fcargs, 1);
            try {
                FmuResult<Long> res = instance.getNumberOfContinuousStates();
                longArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));

        functions.put("getNumberOfEventIndicators", new FunctionValue.ExternalFunctionValue(fcargs -> {
            // int getNumberOfEventIndicators(int[] nEventIndicators);

            checkArgLength(fcargs, 1);
            try {
                FmuResult<Long> res = instance.getNumberOfEventIndicators();
                intArrayOutArgMapper.mapOut(fcargs.get(0), res.result);
                return status2IntValue(res.status);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        functions.put("doStep", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 7);

            //in
            double currentCommunicationPoint = getDouble(fcargs.get(0));
            double communicationStepSize = getDouble(fcargs.get(1));
            boolean noSetFMUStatePriorToCurrentPoint = getBool(fcargs.get(2));
            //out

            UpdatableValue eventHandlingNeeded = getUpdatable(fcargs.get(3));
            UpdatableValue terminateSimulation = getUpdatable(fcargs.get(4));
            UpdatableValue earlyReturn = getUpdatable(fcargs.get(5));
            UpdatableValue lastSuccessfulTime = getUpdatable(fcargs.get(6));
            /*int doStep(real currentCommunicationPoint, real communicationStepSize, bool nosetFMUStatePriorToCurrentPoint,
               out bool eventHandlingNeeded,out bool terminateSimulation,out bool earlyReturn,out real lastSuccessfulTime);*/

            try {
                FmuResult<IFmi3Instance.DoStepResult> res = instance.doStep(currentCommunicationPoint, communicationStepSize,
                        noSetFMUStatePriorToCurrentPoint);

                if (res.status == Fmi3Status.OK) {
                    eventHandlingNeeded.setValue(new BooleanValue(res.result.isEventHandlingNeeded()));
                    terminateSimulation.setValue(new BooleanValue(res.result.isTerminateSimulation()));
                    earlyReturn.setValue(new BooleanValue(res.result.isEarlyReturn()));
                    lastSuccessfulTime.setValue(new RealValue(res.result.getLastSuccessfulTime()));
                }
                return new IntegerValue(res.status.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));


        functions.put("setFMUState", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value v = fcargs.get(0).deref();

            if (v instanceof Fmu3StateValue) {
                try {
                    Fmu3StateValue stateValue = (Fmu3StateValue) v;
                    Fmi3Status res = instance.setState(stateValue.getModule());
                    return new IntegerValue(res.value);
                } catch (FmuInvocationException e) {
                    throw new InterpreterException(e);
                }
            }

            throw new InterpreterException("Invalid value");
        }));
        functions.put("getFMUState", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            if (!(fcargs.get(0) instanceof UpdatableValue)) {
                throw new InterpreterException("value not a reference value");
            }


            try {

                org.intocps.fmi.jnifmuapi.fmi3.FmuResult<Fmi3State> res = instance.getState();

                if (res.status == Fmi3Status.OK) {
                    UpdatableValue ref = (UpdatableValue) fcargs.get(0);
                    ref.setValue(new Fmu3StateValue(res.result));
                }


                return new IntegerValue(res.status.value);

            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }


        }));
        functions.put("freeFMUState", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            Value v = fcargs.get(0).deref();

            if (v instanceof Fmu3StateValue) {
                try {
                    Fmu3StateValue stateValue = (Fmu3StateValue) v;
                    Fmi3Status res = instance.freeState(stateValue.getModule());
                    return new IntegerValue(res.value);
                } catch (FmuInvocationException e) {
                    throw new InterpreterException(e);
                }
            }

            throw new InterpreterException("Invalid value");


        }));


        checkRequiredFunctions(module, functions);

        return new Fmu3InstanceValue(functions, instance, fmuLogOutputStream);
    }

    private static ExternalReflectCallHelper handleResultReturns(ExternalReflectCallHelper builder) {

        builder.addReturn(new ResultProxyArgMapper(new Fmi3StatusArgMapping(),
                builder.stream().filter(arg -> arg.getDirection() == ExternalReflectCallHelper.ArgMapping.InOut.Output)
                        .collect(Collectors.toList())));

        //turn off output mapping for these arguments
        builder.stream().filter(arg -> arg.getDirection() == ExternalReflectCallHelper.ArgMapping.InOut.Output)
                .forEach(arg -> arg.setDirection(ExternalReflectCallHelper.ArgMapping.InOut.OutputThroughReturn));

        return builder;
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

        } catch (Exception e) {
            e.printStackTrace();
            return new NullValue();
        }
    }


}
