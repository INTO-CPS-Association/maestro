package org.intocps.maestro.interpreter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.intocps.fmi.*;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentStateValue;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Fmi2Interpreter {
    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);
    private final File workingDirectory;

    public Fmi2Interpreter(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public static boolean getBool(Value value) {

        value = value.deref();

        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).getValue();
        }
        throw new InterpreterException("Value is not boolean");
    }

    public static double getDouble(Value value) {

        value = value.deref();

        if (value instanceof RealValue) {
            return ((RealValue) value).getValue();
        }
        throw new InterpreterException("Value is not double");
    }

    public static long getUint(Value value) {

        value = value.deref();

        if (value instanceof UnsignedIntegerValue) {
            return ((UnsignedIntegerValue) value).getValue();
        }
        if (value instanceof IntegerValue) {
            return ((IntegerValue) value).getValue();
        }
        throw new InterpreterException("Value is not unsigned integer");
    }

    public static String getString(Value value) {

        value = value.deref();

        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        throw new InterpreterException("Value is not string");
    }

    static void checkArgLength(List<Value> values, int size) {
        if (values == null) {
            throw new InterpreterException("No values passed");
        }

        if (values.stream().anyMatch(Objects::isNull)) {
            throw new InterpreterException("Argument list contains null values");
        }

        if (values.size() != size) {
            if (values.size() < size) {
                throw new InterpreterException("Too few arguments");
            } else {
                throw new InterpreterException("Too many arguments");
            }
        }
    }

    static <T extends Value> List<T> getArrayValue(Value value, Class<T> clz) {
        return getArrayValue(value, Optional.empty(), clz);
    }

    static <T extends Value> List<T> getArrayValue(Value value, Optional<Long> limit, Class<T> clz) {

        value = value.deref();

        if (value instanceof ArrayValue) {

            ArrayValue<? extends Value> array = (ArrayValue<Value>) value;
            if (((ArrayValue) value).getValues().isEmpty()) {
                return Collections.emptyList();
            }

            if (!clz.isAssignableFrom(array.getValues().get(0).deref().getClass())) {
                throw new InterpreterException("Array not containing the right type. Expected: " + clz.getSimpleName() + " Actual: " +
                        array.getValues().get(0).getClass().getSimpleName());
            }
            if (limit.isPresent()) {
                return array.getValues().stream().limit(limit.get()).map(Value::deref).map(clz::cast).collect(Collectors.toList());
            } else {
                return array.getValues().stream().map(Value::deref).map(clz::cast).collect(Collectors.toList());
            }
        }
        throw new InterpreterException("Value is not an array");
    }

    public static FmuComponentValue getFmuComponentValue(BufferedOutputStream fmuLogOutputStream, final IFmiComponent component) {

        //populate component functions
        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("setDebugLogging", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 3);

            boolean debugLogginOn = getBool(fcargs.get(0));
            //                        int arraySize = getInteger(fcargs.get(1));
            List<StringValue> categories = getArrayValue(fcargs.get(2), StringValue.class);

            try {
                Fmi2Status res = component.setDebugLogging(debugLogginOn, categories.stream().map(StringValue::getValue).toArray(String[]::new));
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
                Fmi2Status res = component.setupExperiment(toleranceDefined, tolerance, startTime, stopTimeDefined, stopTime);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));
        componentMembers.put("enterInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = component.enterInitializationMode();
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));
        componentMembers.put("exitInitializationMode", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = component.exitInitializationMode();
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
                Fmi2Status res = component.setReals(scalarValueIndices, values);
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
                FmuResult<double[]> res = component.getReal(scalarValueIndices);

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
                Fmi2Status res = component.setIntegers(scalarValueIndices, values);
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
                FmuResult<int[]> res = component.getInteger(scalarValueIndices);

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
                Fmi2Status res = component.setBooleans(scalarValueIndices, values);
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
                FmuResult<boolean[]> res = component.getBooleans(scalarValueIndices);

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
                Fmi2Status res = component.setStrings(scalarValueIndices, values);
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
                FmuResult<String[]> res = component.getStrings(scalarValueIndices);

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
                Fmi2Status res = component.doStep(currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }
        }));

        componentMembers.put("terminate", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 0);
            try {
                Fmi2Status res = component.terminate();
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
                    Fmi2Status res = component.setState(stateValue.getModule());
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

                FmuResult<IFmiComponentState> res = component.getState();

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
                    Fmi2Status res = component.freeState(stateValue.getModule());
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
                FmuResult<Double> res = component.getRealStatus(kindEnum);

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
                FmuResult<double[]> res = component.getRealOutputDerivatives(scalarValueIndices, orders);

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
                Fmi2Status res = component.setRealInputDerivatives(scalarValueIndices, orders, values);
                return new IntegerValue(res.value);
            } catch (FmuInvocationException e) {
                throw new InterpreterException(e);
            }

        }));


        return new FmuComponentValue(componentMembers, component, fmuLogOutputStream);
    }

    public static Map<String, Value> createFmuMembers(File workingDirectory, String guid, IFmu fmu) {
        Map<String, Value> functions = new HashMap<>();

        functions.put("instantiate", new FunctionValue.ExternalFunctionValue(fargs -> {

            checkArgLength(fargs, 3);

            String name = getString(fargs.get(0));
            boolean visible = getBool(fargs.get(1));
            boolean logginOn = getBool(fargs.get(2));

            try {

                long startInstantiateTime = System.nanoTime();

                logger.debug(String.format("Loading native FMU. GUID: %s, NAME: %s", "" + guid, "" + name));

                BufferedOutputStream fmuLogOutputStream =
                        workingDirectory == null ? null : new BufferedOutputStream(new FileOutputStream(new File(workingDirectory, name + ".log")));

                final String formatter = "{} {} {} {}";
                String pattern = "%d{ISO8601} %-5p - %m%n";

                Layout layout = PatternLayout.newBuilder().withPattern(pattern).withCharset(StandardCharsets.UTF_8).build();//
                IFmiComponent component = fmu.instantiate(guid, name, visible, logginOn, new IFmuCallback() {
                    @Override
                    public void log(String instanceName, Fmi2Status status, String category, String message) {
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
                                case Pending:
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

                    }

                    @Override
                    public void stepFinished(Fmi2Status status) {

                    }
                });

                if (component == null) {
                    logger.debug("Component instantiate failed");
                    return new NullValue();
                }
                long stopInstantiateTime = System.nanoTime();
                System.out.println("Interpretation instantiate took: " + (stopInstantiateTime - startInstantiateTime));

                return getFmuComponentValue(fmuLogOutputStream, component);


            } catch (XPathExpressionException | FmiInvalidNativeStateException | IOException e) {
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

    public Value createFmiValue(String path, String guid) throws InterpreterException {

        try {
            long startExecTime = System.nanoTime();
            final IFmu fmu = FmuFactory.create(null, URI.create(path));

            fmu.load();

            Map<String, Value> functions = createFmuMembers(workingDirectory, guid, fmu);

            long stopTime = System.nanoTime();

            System.out.println("Interpretation load took: " + (stopTime - startExecTime));
            return new FmuValue(functions, fmu);

        } catch (IOException | FmuInvocationException | FmuMissingLibraryException e) {

            e.printStackTrace();
            return new NullValue();
        } catch (Exception e) {
            e.printStackTrace();
            return new NullValue();
        }
    }

    public Value createFmiValue(Class<?> clz) {
        try {
            long startExecTime = System.nanoTime();
            final IFmu fmu = (IFmu) clz.getDeclaredConstructor().newInstance();

            fmu.load();
            Fmi2ModelDescription md = new Fmi2ModelDescription(fmu.getModelDescription());
            Map<String, Value> functions = createFmuMembers(workingDirectory, md.getGuid(), fmu);

            long stopTime = System.nanoTime();

            System.out.println("Interpretation load took: " + (stopTime - startExecTime));
            return new FmuValue(functions, fmu);

        } catch (IOException | FmuInvocationException | FmuMissingLibraryException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ParserConfigurationException | SAXException | XPathExpressionException e) {

            e.printStackTrace();
            return new NullValue();
        }
    }
}
