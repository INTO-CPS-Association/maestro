package org.intocps.maestro.interpreter;

import org.apache.commons.lang.ArrayUtils;
import org.intocps.fmi.*;
import org.intocps.fmi.jnifmuapi.Factory;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FmiInterpreter {
    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);

    static boolean getBool(Value value) {

        value = value.deref();

        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).getValue();
        }
        throw new InterpreterException("Value is not boolean");
    }

    static double getDouble(Value value) {

        value = value.deref();

        if (value instanceof RealValue) {
            return ((RealValue) value).getValue();
        }
        throw new InterpreterException("Value is not double");
    }

    static String getString(Value value) {

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

        value = value.deref();

        if (value instanceof ArrayValue) {

            ArrayValue array = (ArrayValue) value;
            if (((ArrayValue) value).getValues().isEmpty()) {
                return Collections.emptyList();
            }

            if (!clz.isAssignableFrom(array.getValues().get(0).getClass())) {
                throw new InterpreterException("Array not containing the right type");
            }

            return array.getValues();
        }
        throw new InterpreterException("Value is not an array");


    }


    public ModuleValue createFmiValue(String path, String guid) throws InterpreterException {

        try {
            long startExecTime = System.nanoTime();
            final IFmu fmu = Factory.create(new File(path));

            fmu.load();


            Map<String, Value> functions = new HashMap<>();

            functions.put("instantiate", new FunctionValue.ExternalFunctionValue(fargs -> {

                checkArgLength(fargs, 3);

                String name = getString(fargs.get(0));
                boolean visible = getBool(fargs.get(1));
                boolean logginOn = getBool(fargs.get(2));

                try {

                    long startInstantiateTime = System.nanoTime();
                    IFmiComponent component = fmu.instantiate(guid, name, visible, logginOn, new IFmuCallback() {
                        @Override
                        public void log(String instanceName, Fmi2Status status, String category, String message) {
                            logger.info("NATIVE: instance: '{}', status: '{}', category: '{}', message: {}", instanceName, status, category, message);
                        }

                        @Override
                        public void stepFinished(Fmi2Status status) {

                        }
                    });

                    //populate component functions
                    Map<String, Value> componentMembers = new HashMap<>();

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
                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();
                        double[] values = getArrayValue(fcargs.get(2), RealValue.class).stream().mapToDouble(RealValue::getValue).toArray();

                        try {
                            Fmi2Status res = component.setReals(scalarValueIndices, values);
                            return new IntegerValue(res.value);
                        } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                            throw new InterpreterException(e);
                        }

                    }));
                    componentMembers.put("getReal", new FunctionValue.ExternalFunctionValue(fcargs -> {

                        checkArgLength(fcargs, 3);

                        if (!(fcargs.get(2) instanceof ReferenceValue)) {
                            throw new InterpreterException("value not a reference value");
                        }

                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();


                        try {
                            FmuResult<double[]> res = component.getReal(scalarValueIndices);

                            if (res.status == Fmi2Status.OK) {
                                ReferenceValue ref = (ReferenceValue) fcargs.get(2);

                                List<RealValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).map(d -> new RealValue(d))
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
                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();
                        int[] values = getArrayValue(fcargs.get(2), IntegerValue.class).stream().mapToInt(IntegerValue::getValue).toArray();

                        try {
                            Fmi2Status res = component.setIntegers(scalarValueIndices, values);
                            return new IntegerValue(res.value);
                        } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                            throw new InterpreterException(e);
                        }
                    }));
                    componentMembers.put("getInteger", new FunctionValue.ExternalFunctionValue(fcargs -> {

                        checkArgLength(fcargs, 3);

                        if (!(fcargs.get(2) instanceof ReferenceValue)) {
                            throw new InterpreterException("value not a reference value");
                        }

                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();


                        try {
                            FmuResult<int[]> res = component.getInteger(scalarValueIndices);

                            if (res.status == Fmi2Status.OK) {
                                ReferenceValue ref = (ReferenceValue) fcargs.get(2);

                                List<IntegerValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).map(i -> new IntegerValue(i))
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

                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();
                        boolean[] values = ArrayUtils.toPrimitive(
                                getArrayValue(fcargs.get(2), BooleanValue.class).stream().map(BooleanValue::getValue).collect(Collectors.toList())
                                        .toArray(new Boolean[]{}));

                        try {
                            Fmi2Status res = component.setBooleans(scalarValueIndices, values);
                            return new IntegerValue(res.value);
                        } catch (InvalidParameterException | FmiInvalidNativeStateException e) {
                            throw new InterpreterException(e);
                        }
                    }));
                    componentMembers.put("getBoolean", new FunctionValue.ExternalFunctionValue(fcargs -> {

                        checkArgLength(fcargs, 3);

                        if (!(fcargs.get(2) instanceof ReferenceValue)) {
                            throw new InterpreterException("value not a reference value");
                        }

                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();


                        try {
                            FmuResult<boolean[]> res = component.getBooleans(scalarValueIndices);

                            if (res.status == Fmi2Status.OK) {
                                ReferenceValue ref = (ReferenceValue) fcargs.get(2);

                                List<BooleanValue> values = Arrays.stream(ArrayUtils.toObject(res.result)).map(BooleanValue::new)
                                        .collect(Collectors.toList());

                                ref.setValue(new ArrayValue<>(values));
                            }


                            return new IntegerValue(res.status.value);

                        } catch (FmuInvocationException e) {
                            throw new InterpreterException(e);
                        }

                    }));
                    componentMembers.put("setString", new FunctionValue.ExternalFunctionValue(fcargs -> {

                        checkArgLength(fcargs, 3);
                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();
                        String[] values = getArrayValue(fcargs.get(2), StringValue.class).stream().map(StringValue::getValue)
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

                        if (!(fcargs.get(2) instanceof ReferenceValue)) {
                            throw new InterpreterException("value not a reference value");
                        }

                        long[] scalarValueIndices = getArrayValue(fcargs.get(0), NumericValue.class).stream().mapToLong(NumericValue::longValue)
                                .toArray();


                        try {
                            FmuResult<String[]> res = component.getStrings(scalarValueIndices);

                            if (res.status == Fmi2Status.OK) {
                                ReferenceValue ref = (ReferenceValue) fcargs.get(2);

                                List<StringValue> values = Arrays.stream(res.result).map(StringValue::new).collect(Collectors.toList());

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


                    long stopInstantiateTime = System.nanoTime();
                    System.out.println("Interpretation instantiate took: " + (stopInstantiateTime - startInstantiateTime));
                    return new FmuComponentValue(componentMembers, component);


                } catch (XPathExpressionException | FmiInvalidNativeStateException e) {
                    e.printStackTrace();
                }

                return null;
            }));


            functions.put("freeInstance", new FunctionValue.ExternalFunctionValue(fargs -> {

                logger.debug("freeInstance");

                if (fargs.size() != 1) {
                    throw new InterpreterException("Too few arguments");
                }

                if (!(fargs.get(0) instanceof FmuComponentValue)) {
                    throw new InterpreterException("Argument must be an external module reference");
                }

                FmuComponentValue component = (FmuComponentValue) fargs.get(0);

                try {
                    component.getModule().freeInstance();
                } catch (FmuInvocationException e) {
                    e.printStackTrace();
                }

                return new VoidValue();
            }));

            functions.put("unload", new FunctionValue.ExternalFunctionValue(fargs -> {

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

            long stopTime = System.nanoTime();

            System.out.println("Interpretation load took: " + (stopTime - startExecTime));
            return new FmuValue(functions, fmu);

        } catch (IOException | FmuInvocationException | FmuMissingLibraryException e) {

            e.printStackTrace();
            throw new InterpreterException(e);
        }
    }
}
