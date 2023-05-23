package org.intocps.maestro.interpreter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.FmuMissingLibraryException;
import org.intocps.fmi.jnifmuapi.fmi3.*;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.node.ANameType;
import org.intocps.maestro.ast.node.AReferenceType;
import org.intocps.maestro.ast.node.PType;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.intocps.maestro.interpreter.Fmi2Interpreter.*;

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

        if (value instanceof RealValue) {
            return ((RealValue) value).getValue();
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

    private static Value getFmuInstanceValue(BufferedOutputStream fmuLogOutputStream, IFmi3Instance instance,
            Function<String, AModuleDeclaration> resolver) throws NoSuchMethodException {

        //populate component functions
        var module = resolver.apply("FMI3Instance");


        Map<String, Value> functions = new HashMap<>(createGetSetMembers(instance));

        Predicate<AFunctionDeclaration> functionFilter = fun -> !fun.getName().getText().equals("enterInitializationMode") && !fun.getName().getText()
                .startsWith("get") && !fun.getName().getText().equals("setBinary");

        Function<PType, IArgMapping> costumeArgMapper = t -> {

            boolean output = t instanceof AReferenceType;
            if (output) {
                t = ((AReferenceType) t).getType();
            }


            if (t instanceof ANameType) {
                String typeName = ((ANameType) t).getName().getText();
                if (typeName.equals("FMI3Instance")) {
                    return new IArgMapping() {
                        @Override
                        public int getDimension() {
                            return 1;
                        }

                        @Override
                        public long[] getLimits() {
                            return null;
                        }

                        @Override
                        public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
                            return ExternalReflectCallHelper.ArgMapping.InOut.Input;
                        }

                        @Override
                        public Object map(Value v) {
                            if (v instanceof ExternalModuleValue) {
                                return ((ExternalModuleValue<?>) v).getModule();
                            }
                            return null;
                        }

                        @Override
                        public void mapOut(Value original, Object value) {
                            throw new RuntimeException("This is only for input so should not be called");
                        }

                        @Override
                        public Value mapOut(Object value) {
                            return new ExternalModuleValue<>(null, value) {};
                        }

                        @Override
                        public Class getType() {
                            return Object.class;
                        }

                        @Override
                        public String getDescriptiveName() {
                            return "FMI3Instance";
                        }

                        @Override
                        public String getDefaultTestValue() {
                            return "null";
                        }
                    };
                } else if (typeName.equals("FMU3State")) {
                    return new IArgMapping() {
                        @Override
                        public int getDimension() {
                            return 1;
                        }

                        @Override
                        public long[] getLimits() {
                            return null;
                        }

                        @Override
                        public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
                            return output ? ExternalReflectCallHelper.ArgMapping.InOut.Output : ExternalReflectCallHelper.ArgMapping.InOut.Input;
                        }

                        @Override
                        public Object map(Value v) {
                            if (v instanceof Fmu3StateValue) {
                                return ((Fmu3StateValue) v).getModule();
                            }
                            return null;
                        }

                        @Override
                        public void mapOut(Value original, Object value) {
                            if (original instanceof UpdatableValue) {
                                UpdatableValue up = (UpdatableValue) original;
                                up.setValue(new Fmu3StateValue((Fmi3State) value));
                            }

                        }

                        @Override
                        public Value mapOut(Object value) {
                            return null;
                        }

                        @Override
                        public Class getType() {
                            return Fmi3State.class;
                        }

                        @Override
                        public String getDescriptiveName() {
                            return "Fmi3State";
                        }

                        @Override
                        public String getDefaultTestValue() {
                            return null;
                        }
                    };
                }
            }
            return null;
        };

        for (AFunctionDeclaration function : module.getFunctions()) {
            if (functionFilter == null || functionFilter.test(function)) {

                functions.computeIfAbsent(function.getName().getText(), key -> {
                    try {
                        return new ExternalReflectCallHelper(function, instance, costumeArgMapper).build();
                    } catch (NoSuchMethodException e) {
                        System.err.println(e.getMessage());
                        return null;
                    } catch (RuntimeException e) {
                        System.err.println(e.getMessage());
                        return null;
                    }
                });

            }
        }


        for (AFunctionDeclaration function : module.getFunctions()) {
            if (functionFilter == null || functionFilter.test(function)) {

                functions.computeIfAbsent(function.getName().getText(), key -> {
                    try {
                        return new ExternalReflectCallHelper(function, instance, costumeArgMapper) {
                            @Override
                            public FunctionValue.ExternalFunctionValue build() throws NoSuchMethodException {
                                final List<IArgMapping> args = Collections.unmodifiableList(this);
                                final IArgMapping rArg = returnArg;
                                final Method method = object.getClass().getMethod(functionName,
                                        args.stream().filter(arg -> arg.getDirection() == ArgMapping.InOut.Input).map(IArgMapping::getType)
                                                .toArray(Class[]::new));

                                if (method.getReturnType().isAssignableFrom(FmuResult.class)) {
                                    //ok so we are using the result return type
                                    long outputs = args.stream().filter(arg -> arg.getDirection() == ArgMapping.InOut.Output).count();
                                    System.out.println(outputs);
                                }


                                return new FunctionValue.ExternalFunctionValue(fcargs -> {

                                    checkArgLength(fcargs, args.size());

                                    //map inputs
                                    var vitr = fcargs.iterator();
                                    var mitr = args.iterator();
                                    List argValues = new ArrayList(args.size());
                                    while (vitr.hasNext() && mitr.hasNext()) {
                                        var v = vitr.next();
                                        IArgMapping mapper = mitr.next();
                                        switch (mapper.getDirection()) {
                                            case Input:
                                            case Output:
                                                argValues.add(mapper.map(v));
                                                break;
                                        }
                                    }

                                    try {
                                        var ret = method.invoke(object, argValues.toArray());

                                        //map outputs
                                        vitr = fcargs.iterator();
                                        var voitr = argValues.iterator();
                                        mitr = args.iterator();
                                        while (vitr.hasNext() && voitr.hasNext() && mitr.hasNext()) {
                                            var original = vitr.next();
                                            IArgMapping mapper = mitr.next();

                                            Object output = null;

                                            switch (mapper.getDirection()) {
                                                case Input:
                                                case Output:
                                                    output = voitr.next();
                                                    break;
                                            }

                                            if (mapper.getDirection() == ArgMapping.InOut.Output) {
                                                mapper.mapOut(original, output);
                                            }
                                        }


                                        if (rArg == null) {
                                            return new VoidValue();
                                        }
                                        return rArg.mapOut(ret);


                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }
                        }.build();
                    } catch (NoSuchMethodException e) {
                        System.err.println(e.getMessage());
                        return null;
                    } catch (RuntimeException e) {
                        System.err.println(e.getMessage());
                        return null;
                    }
                });

            }
        }


        //void methods
        //        functions.put("freeInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 0);
        //            try {
        //                instance.freeInstance();
        //                return new VoidValue();
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //        }));
        //
        //        String[] voidStatusMethods = new String[]{"exitInitializationMode", "enterEventMode", "terminate", "reset", "enterConfigurationMode",
        //                "exitConfigurationMode", "evaluateDiscreteStates", "enterContinuousTimeMode", "enterStepMode"};
        //
        //        for (String methodName : voidStatusMethods) {
        //            Method method = instance.getClass().getMethod(methodName);
        //
        //            functions.put(methodName, new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //                checkArgLength(fcargs, 0);
        //
        //
        //                try {
        //                    Fmi3Status res = (Fmi3Status) method.invoke(instance);
        //                    return status2IntValue(res);
        //                } catch (IllegalAccessException | InvocationTargetException e) {
        //                    throw new InterpreterException(e);
        //                }
        //            }));
        //
        //        }



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


        //        functions.put("setDebugLogging", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //
        //            boolean debugLogginOn = getBool(fcargs.get(0));
        //            //                        int arraySize = getInteger(fcargs.get(1));
        //            List<StringValue> categories = getArrayValue(fcargs.get(2), StringValue.class);
        //
        //            try {
        //                Fmi3Status res = instance.setDebugLogging(debugLogginOn, categories.stream().map(StringValue::getValue).toArray(String[]::new));
        //                return new IntegerValue(res.value);
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));

        //        functions.put("setupExperiment", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 5);
        //
        //            boolean toleranceDefined = getBool(fcargs.get(0));
        //            double tolerance = getDouble(fcargs.get(1));
        //            double startTime = getDouble(fcargs.get(2));
        //            boolean stopTimeDefined = getBool(fcargs.get(3));
        //            double stopTime = getDouble(fcargs.get(4));
        //            try {
        //                Fmi2Status res = instance.setupExperiment(toleranceDefined, tolerance, startTime, stopTimeDefined, stopTime);
        //                return new IntegerValue(res.value);
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));
        functions.put("enterInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            Double tolerance = getDouble(fcargs.get(0));
            double startTime = getDouble(fcargs.get(1));
            Double stopTime = getDouble(fcargs.get(2));


            try {
                Fmi3Status res = instance.enterInitializationMode(tolerance, startTime, stopTime);
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


        //        functions.put("exitInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //            checkArgLength(fcargs, 0);
        //            try {
        //                Fmi3Status res = instance.exitInitializationMode();
        //                return new IntegerValue(res.value);
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));
        //        functions.put("setReal", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //            checkArgLength(fcargs, 3);
        //            long elementsToUse = getUint(fcargs.get(1));
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //            double[] values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().limit(elementsToUse)
        //                    .mapToDouble(NumericValue::realValue).toArray();
        //
        //            try {
        //                Fmi3Status res = instance.setReals(scalarValueIndices, values);
        //                return new IntegerValue(res.value);
        //            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //        }));
        //        functions.put("getReal", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //
        //            if (!(fcargs.get(2) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //
        //            try {
        //                org.intocps.fmi.FmuResult<double[]> res = instance.getReal(scalarValueIndices);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);
        //
        //                    List<RealValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(d -> new RealValue(d))
        //                            .collect(Collectors.toList());
        //
        //                    ref.setValue(new ArrayValue<>(values));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //
        //        }));
        //        functions.put("setInteger", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //            checkArgLength(fcargs, 3);
        //            long elementsToUse = getUint(fcargs.get(1));
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //            int[] values =
        //                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), IntegerValue.class).stream().mapToInt(IntegerValue::getValue).toArray();
        //
        //            try {
        //                Fmi2Status res = instance.setIntegers(scalarValueIndices, values);
        //                return new IntegerValue(res.value);
        //            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));
        //        functions.put("getInteger", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //            if (!(fcargs.get(2) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //
        //            try {
        //                org.intocps.fmi.FmuResult<int[]> res = instance.getInteger(scalarValueIndices);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);
        //
        //                    List<IntegerValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(i -> new IntegerValue(i))
        //                            .collect(Collectors.toList());
        //
        //                    ref.setValue(new ArrayValue<>(values));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //
        //        }));
        //        functions.put("setBoolean", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //            long elementsToUse = getUint(fcargs.get(1));
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //            boolean[] values = ArrayUtils.toPrimitive(
        //                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), BooleanValue.class).stream().map(BooleanValue::getValue)
        //                            .collect(Collectors.toList()).toArray(new Boolean[]{}));
        //
        //            try {
        //                Fmi2Status res = instance.setBooleans(scalarValueIndices, values);
        //                return new IntegerValue(res.value);
        //            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
        //                throw new InterpreterException(e);
        //            }
        //        }));
        //        functions.put("getBoolean", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //
        //            if (!(fcargs.get(2) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //
        //            try {
        //                org.intocps.fmi.FmuResult<boolean[]> res = instance.getBooleans(scalarValueIndices);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);
        //
        //                    List<BooleanValue> values =
        //                            Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(BooleanValue::new).collect(Collectors.toList());
        //
        //                    ref.setValue(new ArrayValue<>(values));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //        }));
        //        functions.put("setString", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //            String[] values = getArrayValue(fcargs.get(2), Optional.of(elementsToUse), StringValue.class).stream().map(StringValue::getValue)
        //                    .collect(Collectors.toList()).toArray(new String[]{});
        //
        //            try {
        //                Fmi2Status res = instance.setStrings(scalarValueIndices, values);
        //                return new IntegerValue(res.value);
        //            } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //        }));
        //        functions.put("getString", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 3);
        //
        //            if (!(fcargs.get(2) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //
        //
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //
        //            try {
        //                org.intocps.fmi.FmuResult<String[]> res = instance.getStrings(scalarValueIndices);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(2);
        //
        //                    List<StringValue> values = Arrays.stream(res.result).limit(elementsToUse).map(StringValue::new).collect(Collectors.toList());
        //
        //                    ref.setValue(new ArrayValue<>(values));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //
        //        }));
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

        functions.put("terminate", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 0);
            try {
                Fmi3Status res = instance.terminate();
                return new IntegerValue(res.value);
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

        //        functions.put("getRealStatus", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //
        //            checkArgLength(fcargs, 2);
        //
        //            if (!(fcargs.get(1) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //
        //            Value kindValue = fcargs.get(0).deref();
        //
        //            if (!(kindValue instanceof IntegerValue)) {
        //                throw new InterpreterException("Invalid kind value: " + kindValue);
        //            }
        //
        //            int kind = ((IntegerValue) kindValue).getValue();
        //
        //            Fmi2StatusKind kindEnum = Arrays.stream(Fmi2StatusKind.values()).filter(v -> v.value == kind).findFirst().orElse(null);
        //
        //            try {
        //                org.intocps.fmi.FmuResult<Double> res = instance.getRealStatus(kindEnum);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(1);
        //
        //                    ref.setValue(new RealValue(res.result));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //
        //        }));
        //
        //        functions.put("getRealOutputDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //            //   int getRealOutputDerivatives(long[] scalarValueIndices, UInt nvr, int[] order, ref double[] derivatives);
        //            checkArgLength(fcargs, 4);
        //
        //            if (!(fcargs.get(3) instanceof UpdatableValue)) {
        //                throw new InterpreterException("value not a reference value");
        //            }
        //
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //            int[] orders =
        //                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().mapToInt(NumericValue::intValue).toArray();
        //
        //            try {
        //                FmuResult<double[]> res = instance.getRealOutputDerivatives(scalarValueIndices, orders);
        //
        //                if (res.status == Fmi2Status.OK) {
        //                    UpdatableValue ref = (UpdatableValue) fcargs.get(3);
        //
        //                    List<RealValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).limit(elementsToUse).map(d -> new RealValue(d))
        //                            .collect(Collectors.toList());
        //
        //                    ref.setValue(new ArrayValue<>(values));
        //                }
        //
        //
        //                return new IntegerValue(res.status.value);
        //
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //
        //        }));
        //
        //        functions.put("setRealInputDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
        //            // int setRealInputDerivatives(UInt[] scalarValueIndices, UInt nvr, int[] order, ref real[] derivatives);
        //            checkArgLength(fcargs, 4);
        //            long elementsToUse = getUint(fcargs.get(1));
        //
        //
        //            long[] scalarValueIndices =
        //                    getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
        //                            .toArray();
        //
        //            int[] orders =
        //                    getArrayValue(fcargs.get(2), Optional.of(elementsToUse), NumericValue.class).stream().mapToInt(NumericValue::intValue).toArray();
        //
        //            double[] values =
        //                    getArrayValue(fcargs.get(3), Optional.of(elementsToUse), RealValue.class).stream().mapToDouble(RealValue::getValue).toArray();
        //
        //            try {
        //                Fmi2Status res = instance.setRealInputDerivatives(scalarValueIndices, orders, values);
        //                return new IntegerValue(res.value);
        //            } catch (FmuInvocationException e) {
        //                throw new InterpreterException(e);
        //            }
        //
        //        })
        //            )
        checkRequiredFunctions(module, functions);

        return new Fmu3InstanceValue(functions, instance, fmuLogOutputStream);
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

                    checkArgLength(fcargs, 2);
                    Value scalarValue = fcargs.get(0);
                    Value valuesValue = fcargs.get(1);

                    if (!(valuesValue instanceof UpdatableValue)) {
                        throw new InterpreterException("value not a reference value");
                    }
                    //                    long elementsToUse = getUint(fcargs.get(1));

                    //                    long[] scalarValueIndices =
                    //                            getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                    //                                    .toArray();

                    long[] scalarValueIndices = getArrayValue(scalarValue, Optional.empty(), NumericValue.class).stream()
                            .mapToLong(NumericValue::longValue).toArray();

                    long elementsToUse = scalarValueIndices.length;

                    try {
                        @SuppressWarnings("rawtypes")
                        FmuResult res = (FmuResult) m.invoke(instance, scalarValueIndices);// instance.getFloat64(scalarValueIndices);

                        if (res.status == Fmi3Status.OK) {
                            UpdatableValue ref = (UpdatableValue) valuesValue;

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

                Method m = instance.getClass().getMethod(method.getKey(), long[].class, argClass);

                componentMembers.put(method.getKey(), new FunctionValue.ExternalFunctionValue(fcargs -> {
                    checkArgLength(fcargs, 2);
                    //                    long elementsToUse = getUint(fcargs.get(1));
                    //                    long[] scalarValueIndices =
                    //                            getArrayValue(fcargs.get(0), Optional.of(elementsToUse), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                    //                                    .toArray();
                    Value scalarValue = fcargs.get(0);
                    Value valuesValue = fcargs.get(1);

                    long[] scalarValueIndices = getArrayValue(scalarValue, Optional.empty(), NumericValue.class).stream()
                            .mapToLong(NumericValue::longValue).toArray();

                    long elementsToUse = scalarValueIndices.length;

                    //extract values
                    Object values = null;

                    switch (method.getValue()) {

                        case Bool:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), BooleanValue.class).stream().map(BooleanValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Boolean[]{}));
                            break;
                        case Byte:
                            //todo change native type to use int as byte us unsigned
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), ByteValue.class).stream().map(ByteValue::getValue)
                                            .map(v -> v & 0x00ff).map(Integer::byteValue).collect(Collectors.toList()).toArray(new Byte[]{}));
                            break;
                        case Float:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), FloatValue.class).stream().map(FloatValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Float[]{}));
                            break;
                        case Int:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), IntegerValue.class).stream().map(IntegerValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Integer[]{}));
                            break;
                        case Long:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), LongValue.class).stream().map(LongValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Long[]{}));
                            break;
                        case Real:
                            values = getArrayValue(valuesValue, Optional.of(elementsToUse), NumericValue.class).stream().limit(elementsToUse)
                                    .mapToDouble(NumericValue::realValue).toArray();
                            break;
                        case Short:
                            values = ArrayUtils.toPrimitive(
                                    getArrayValue(valuesValue, Optional.of(elementsToUse), ShortValue.class).stream().map(ShortValue::getValue)
                                            .collect(Collectors.toList()).toArray(new Short[]{}));
                            break;
                        case String:
                            values = getArrayValue(valuesValue, Optional.of(elementsToUse), StringValue.class).stream().map(StringValue::getValue)
                                    .collect(Collectors.toList()).toArray(new String[]{});
                            break;
                    }

                    try {
                        Fmi3Status res = (Fmi3Status) m.invoke(instance, scalarValueIndices, values);// instance.setBoolean().setReals
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


}
