package org.intocps.maestro.interpreter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.csv.CSVValue;
import org.intocps.maestro.interpreter.values.csv.CsvDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;
import org.intocps.maestro.interpreter.values.derivativeestimator.DerivativeEstimatorValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.interpreter.values.modeltransition.ModelTransitionValue;
import org.intocps.maestro.interpreter.values.utilities.ArrayUtilValue;
import org.intocps.maestro.interpreter.values.variablestep.VariableStepValue;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Default interpreter factory with framework support and other basic features.
 * This class provides run-time support only. It creates and destroys certain types based on load and unload
 */
public class DefaultExternalValueFactory implements IExternalValueFactory {

    final static List<Class<? extends IValueLifecycleHandler>> defaultHandlers =
            Arrays.asList(LoggerLifecycleHandler.class, CsvLifecycleHandler.class, ArrayUtilLifecycleHandler.class,
                    JavaClasspathLoaderLifecycleHandler.class, MathLifecycleHandler.class, Fmi2LifecycleHandler.class);
    private final File workingDirectory;
    private final ByteArrayOutputStream baos;
    protected Map<String, IValueLifecycleHandler> lifecycleHandlers;
    protected Map<Value, IValueLifecycleHandler> values = new HashMap<>();

    public DefaultExternalValueFactory(File workingDirectory,
            InputStream config) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        this.workingDirectory = workingDirectory;


        lifecycleHandlers = new HashMap<>();

        for (Class<? extends IValueLifecycleHandler> handler : defaultHandlers) {
            lifecycleHandlers.put(handler.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                    instantiateHandler(workingDirectory, handler));
        }


        baos = new ByteArrayOutputStream();
        if (config != null) {
            config.transferTo(baos);
        }

        lifecycleHandlers.put(DataWriterLifecycleHandler.class.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                new DataWriterLifecycleHandler(workingDirectory, new ByteArrayInputStream(baos.toByteArray())));

        lifecycleHandlers.put(MEnvLifecycleHandler.class.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                new MEnvLifecycleHandler(new ByteArrayInputStream(baos.toByteArray())));

        //performance
        for (Class<? extends IValueLifecycleHandler> cls : new Class[]{Fmi2LifecycleHandler.class, LoggerLifecycleHandler.class,
                BooleanLogicLifecycleHandler.class, MathLifecycleHandler.class}) {

            lifecycleHandlers.put(cls.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(), instantiateHandler(workingDirectory, cls));

        }

        lifecycleHandlers.put(ModelTransitionLifecycleHandler.class.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                new ModelTransitionLifecycleHandler());
    }

    private IValueLifecycleHandler instantiateHandler(File workingDirectory,
            Class<? extends IValueLifecycleHandler> handler) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IValueLifecycleHandler value;
        try {
            //found constructor with a File argument. This is for the working directory
            value = handler.getDeclaredConstructor(File.class).newInstance(workingDirectory);
        } catch (NoSuchMethodException e) {
            try {
                value = handler.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
        return value;
    }


    @Override
    public boolean supports(String type) throws Exception {
        return this.lazyGet(type) != null;
    }

    private IValueLifecycleHandler lazyGet(String type) throws Exception {

        IValueLifecycleHandler known = this.lifecycleHandlers.get(type);
        if (known != null) {
            return known;
        } else {
            List<Class<? extends IValueLifecycleHandler>> handlers = scanForLifecucleHandlers(IValueLifecycleHandler.class);

            for (Class<? extends IValueLifecycleHandler> handler : handlers.stream()
                    .filter(h -> !this.lifecycleHandlers.containsKey(h.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name()))
                    .collect(Collectors.toList())) {

                IValueLifecycleHandler value = instantiateHandler(workingDirectory, handler);

                if (value != null) {
                    this.lifecycleHandlers.putIfAbsent(handler.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(), value);
                }
            }

            if (this.lifecycleHandlers.containsKey(type)) {
                return this.lifecycleHandlers.get(type);
            }

        }

        return null;
    }

    private <T> List<Class<? extends T>> scanForLifecucleHandlers(Class<T> type) {
        Reflections reflections = new Reflections("org.intocps.maestro", this.getClass().getClassLoader(), new SubTypesScanner());

        try {

            Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);

            Predicate<? super Class<? extends T>> containsAnnotation = clz -> clz.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class) != null;

            return subTypes.stream().filter(containsAnnotation).collect(Collectors.toList());
        } catch (ReflectionsException e) {

            throw e;
        }
    }

    @Override
    public Either<Exception, Value> create(String loaderName, List<Value> args) {
        IValueLifecycleHandler handler = null;
        try {
            handler = this.lazyGet(loaderName);
        } catch (Exception e) {
            return Either.left(e);
        }
        if (handler == null) {
            throw new InterpreterException("Could not construct type: " + loaderName);
        }

        Either<Exception, Value> value = handler.instantiate(args);
        if (value.isRight()) {
            values.put(value.getRight(), handler);
        }

        return value;
    }

    @Override
    public Value destroy(Value value) {

        IValueLifecycleHandler handler = values.get(value.deref());
        if (handler != null) {
            handler.destroy(value);
            values.remove(value);
            return new VoidValue();
        }

        throw new InterpreterException("UnLoad of unknown type: " + value);
    }

    @Override
    public IExternalValueFactory changeWorkingDirectory(Path newSuggestion,
            InputStream config) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return new DefaultExternalValueFactory(newSuggestion.toFile(), config == null ? new ByteArrayInputStream(baos.toByteArray()) : config);
    }

    protected abstract static class BaseLifecycleHandler implements IValueLifecycleHandler {
        @Override
        public void destroy(Value value) {

        }

        @Override
        public InputStream getMablModule() {
            return null;
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "VariableStep")
    public static class VariableStepLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            if (args == null || args.isEmpty()) {
                return Either.left(new AnalysisException("No values passed"));
            }

            if (args.stream().anyMatch(Objects::isNull)) {
                return Either.left(new AnalysisException("Argument list contains null values"));
            }

            String config = ((StringValue) args.get(0).deref()).getValue();
            return Either.right(new VariableStepValue(config));
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "DerivativeEstimator")
    public static class DerivativeEstimatorLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new DerivativeEstimatorValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "Logger")
    public static class LoggerLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new LoggerValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "ConsolePrinter")
    public static class ConsolePrinterLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new ConsolePrinterValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "CSV")
    public static class CsvLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new CSVValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "ArrayUtil")
    public static class ArrayUtilLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new ArrayUtilValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "JavaClasspathLoader")
    public static class JavaClasspathLoaderLifecycleHandler extends BaseLifecycleHandler {


        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            if (args.isEmpty()) {
                return Either.left(new Exception("Missing arguments for java classpath loader. Expecting: <fully qualified class name> <args>..."));
            }

            Value classNameArg = args.get(0).deref();
            if (classNameArg instanceof StringValue) {


                String qualifiedClassName = ((StringValue) classNameArg).getValue();
                try {
                    Class<?> clz = Class.forName(qualifiedClassName);

                    if (!Value.class.isAssignableFrom(clz)) {
                        return Either.left(new Exception("Class not compatible with: " + Value.class.getName()));
                    }

                    int argCount = args.size() - 1;
                    Class[] argTypes = IntStream.range(0, argCount).mapToObj(i -> Value.class).toArray(Class[]::new);

                    Constructor<?> ctor;
                    if (argTypes.length == 0) {
                        ctor = clz.getDeclaredConstructor();
                        return Either.right((Value) ctor.newInstance());
                    } else {
                        ctor = clz.getDeclaredConstructor(argTypes);
                        return Either.right((Value) ctor.newInstance(args.stream().skip(1).toArray(Value[]::new)));
                    }


                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException |
                         InvocationTargetException e) {
                    return Either.left(e);
                }
            }
            return Either.left(new Exception("Missing name of the class to load"));
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "Math")
    public static class MathLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new MathValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "RealTime")
    public static class RealTimeLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new RealTimeValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "BooleanLogic")
    public static class BooleanLogicLifecycleHandler extends BaseLifecycleHandler {
        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new BooleanLogicValue());
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "FMI2")
    public static class Fmi2LifecycleHandler extends BaseLifecycleHandler {
        final private File workingDirectory;

        public Fmi2LifecycleHandler(File workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public void destroy(Value value) {
            if (value instanceof FmuValue) {
                FmuValue fmuVal = (FmuValue) value;
                FunctionValue unloadFunction = (FunctionValue) fmuVal.lookup("unload");
                unloadFunction.evaluate(Collections.emptyList());
            }
        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            String guid = ((StringValue) args.get(0)).getValue();
            String path = ((StringValue) args.get(1)).getValue();
            try {
                path = (new URI(path)).getRawPath();
            } catch (URISyntaxException e) {
                return Either.left(new AnalysisException("The path passed to load is not a URI", e));
            }
            return Either.right(new Fmi2Interpreter(workingDirectory).createFmiValue(path, guid));
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "JFMI2")
    public static class JFmi2LifecycleHandler extends BaseLifecycleHandler {
        final private File workingDirectory;

        public JFmi2LifecycleHandler(File workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public void destroy(Value value) {
            if (value instanceof FmuValue) {
                FmuValue fmuVal = (FmuValue) value;
                FunctionValue unloadFunction = (FunctionValue) fmuVal.lookup("unload");
                unloadFunction.evaluate(Collections.emptyList());
            }
        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            String className = ((StringValue) args.get(0)).getValue();
            try {
                Class<?> clz = this.getClass().getClassLoader().loadClass(className);

                return Either.right(new Fmi2Interpreter(workingDirectory).createFmiValue(clz));
            } catch (ClassNotFoundException e) {
                return Either.left(new AnalysisException("The path passed to load is not a URI", e));
            }
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "ModelTransition")
    public static class ModelTransitionLifecycleHandler extends BaseLifecycleHandler {

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            String transitionPath = ((StringValue) args.get(0)).getValue();
            return Either.right(new ModelTransitionValue(transitionPath));
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "MEnv")
    public class MEnvLifecycleHandler extends BaseLifecycleHandler {

        public static final String ENVIRONMENT_VARIABLES = "environment_variables";

        private final Map<String, Object> rawData;

        public MEnvLifecycleHandler(InputStream config) throws IOException {

            if (config != null && config.available() > 0) {
                Map<String, Object> map = new ObjectMapper().readValue(config, new TypeReference<>() {
                });
                this.rawData = map;
            } else {
                this.rawData = null;
            }


        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {

            if (rawData == null || !rawData.containsKey(ENVIRONMENT_VARIABLES)) {
                return Either.left(new Exception("Missing required runtime key: " + ENVIRONMENT_VARIABLES));
            }

            final Map<String, Object> data = (Map<String, Object>) rawData.get(ENVIRONMENT_VARIABLES);


            Map<String, Value> members = new HashMap<>();
            members.put("getBool", new FunctionValue.ExternalFunctionValue(a -> {

                Value.checkArgLength(a, 1);

                String key = getEnvName(a);
                Object val = data.get(key);


                if (val instanceof Integer) {
                    return new BooleanValue(((Integer) val) > 1);
                } else if (val instanceof Boolean) {
                    return new BooleanValue((Boolean) val);
                } else {
                    throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
                }

            }));
            members.put("getInt", new FunctionValue.ExternalFunctionValue(a -> {

                Value.checkArgLength(a, 1);

                String key = getEnvName(a);
                Object val = data.get(key);

                if (val instanceof Integer) {
                    return new IntegerValue(((Integer) val).intValue());
                } else {
                    throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
                }

            }));
            members.put("getReal", new FunctionValue.ExternalFunctionValue(a -> {

                Value.checkArgLength(a, 1);

                String key = getEnvName(a);
                Object val = data.get(key);

                if (val instanceof Integer) {
                    return new RealValue(((Integer) val).doubleValue());
                } else if (val instanceof Double) {
                    return new RealValue((Double) val);
                } else {
                    throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
                }
            }));
            members.put("getString", new FunctionValue.ExternalFunctionValue(a -> {

                Value.checkArgLength(a, 1);

                String key = getEnvName(a);
                Object val = data.get(key);
                if (val instanceof String) {
                    return new StringValue((String) val);
                } else {
                    throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
                }
            }));


            ExternalModuleValue<Map<String, Object>> val = new ExternalModuleValue<>(members, data) {

            };
            return Either.right(val);
        }


        private String getEnvName(List<Value> a) {
            return ((StringValue) a.get(0).deref()).getValue();
        }
    }

    @IValueLifecycleHandler.ValueLifecycle(name = "DataWriter")
    protected class DataWriterLifecycleHandler extends BaseLifecycleHandler {

        static final String DEFAULT_CSV_FILENAME = "outputs.csv";
        final String DATA_WRITER_TYPE_NAME;
        final String dataWriterFileNameFinal;
        final List<String> dataWriterFilterFinal;
        final private File workingDirectory;

        public DataWriterLifecycleHandler(File workingDirectory, InputStream config) throws IOException {
            this.workingDirectory = workingDirectory;

            DATA_WRITER_TYPE_NAME = this.getClass().getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name();
            String dataWriterFileName = DEFAULT_CSV_FILENAME;
            List<String> dataWriterFilter = null;

            if (config != null) {
                JsonNode configTree = new ObjectMapper().readTree(config);

                if (configTree.has(DATA_WRITER_TYPE_NAME)) {
                    JsonNode dwConfig = configTree.get(DATA_WRITER_TYPE_NAME);

                    for (JsonNode val : dwConfig) {
                        if (val.has("type") && val.get("type").isTextual() && val.get("type").asText().equals("CSV")) {
                            if (val.has("filename") && val.get("filename").isTextual()) {
                                dataWriterFileName = val.get("filename").asText();
                            }
                            if (val.has("filter")) {
                                dataWriterFilter =
                                        StreamSupport.stream(Spliterators.spliteratorUnknownSize(val.get("filter").iterator(), Spliterator.ORDERED),
                                                false).map(v -> v.asText()).collect(Collectors.toList());
                            }

                        }
                    }
                }
            }

            dataWriterFileNameFinal = dataWriterFileName;
            dataWriterFilterFinal = dataWriterFilter;
        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new DataWriterValue(Collections.singletonList(new CsvDataWriter(
                    workingDirectory == null ? new File(dataWriterFileNameFinal) : new File(workingDirectory, dataWriterFileNameFinal),
                    dataWriterFilterFinal))));
        }
    }
}
