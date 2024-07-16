package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.extensions.*;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.VoidValue;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Default interpreter factory with framework support and other basic features. This class provides run-time support only. It creates and destroys
 * certain types based on load and unload
 */
public class DefaultExternalValueFactory implements IExternalValueFactory {

    final static List<Class<? extends IValueLifecycleHandler>> defaultHandlers = Arrays.asList(LoggerLifecycleHandler.class,
            CsvLifecycleHandler.class, ArrayUtilLifecycleHandler.class, JavaClasspathLoaderLifecycleHandler.class, MathLifecycleHandler.class,
            Fmi2LifecycleHandler.class);
    private final File workingDirectory;
    private final ByteArrayOutputStream baos;
    private final Function<String, AModuleDeclaration> resolver;
    protected Map<String, IValueLifecycleHandler> lifecycleHandlers;
    protected Map<Value, IValueLifecycleHandler> values = new HashMap<>();

    public DefaultExternalValueFactory(File workingDirectory, Function<String, AModuleDeclaration> resolver,
            InputStream config) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        this.workingDirectory = workingDirectory;
        this.resolver = resolver;


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

    public void addLifecycleHandler(
            Class<? extends IValueLifecycleHandler> handlerClass) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        IValueLifecycleHandler.ValueLifecycle annotation = handlerClass.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class);
        if (annotation != null) {
            IValueLifecycleHandler value = instantiateHandler(workingDirectory, handlerClass);
            if (value != null) {
                lifecycleHandlers.put(annotation.name(), value);
            }
        }
    }

    public void addLifecycleHandler(IValueLifecycleHandler handler) {
        IValueLifecycleHandler.ValueLifecycle annotation = handler.getClass().getAnnotation(IValueLifecycleHandler.ValueLifecycle.class);
        if (annotation != null) {
            if (handler != null) {
                lifecycleHandlers.put(annotation.name(), handler);
            }
        }
    }

    private IValueLifecycleHandler instantiateHandler(File workingDirectory,
            Class<? extends IValueLifecycleHandler> handler) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IValueLifecycleHandler value;

        try {
            //found constructor with a File argument. This is for the working directory
            value = handler.getDeclaredConstructor(File.class, Function.class).newInstance(workingDirectory, resolver);
        } catch (NoSuchMethodException e) {
            try {
                //found constructor with a File argument. This is for the working directory
                value = handler.getDeclaredConstructor(File.class).newInstance(workingDirectory);
            } catch (NoSuchMethodException e2) {
                try {
                    value = handler.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e3) {
                    return null;
                }
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
            List<Class<? extends IValueLifecycleHandler>> handlers = scanForLifecycleHandlers(IValueLifecycleHandler.class);

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

    private <T> List<Class<? extends T>> scanForLifecycleHandlers(Class<T> type) {
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
        return new DefaultExternalValueFactory(newSuggestion.toFile(), resolver,
                config == null ? new ByteArrayInputStream(baos.toByteArray()) : config);
    }

}
