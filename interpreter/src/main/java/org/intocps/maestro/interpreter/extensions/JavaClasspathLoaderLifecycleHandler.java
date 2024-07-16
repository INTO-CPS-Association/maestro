package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.IntStream;

@IValueLifecycleHandler.ValueLifecycle(name = "JavaClasspathLoader")
public  class JavaClasspathLoaderLifecycleHandler extends BaseLifecycleHandler {


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