package org.intocps.maestro;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.extensions.JavaClasspathLoaderLifecycleHandler;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class DefaultLoaderFactoryTest {

    @Test
    public void javaClasspathLoadMissingArgTest() throws AnalysisException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String moduleDef = "module A{}";
        String spec = "simulation \n" + "import A;\n" + "{\n" + "A obj = load(\"" + JavaClasspathLoaderLifecycleHandler.class.getAnnotation(
                IValueLifecycleHandler.ValueLifecycle.class).name() + "\");}";

        ErrorReporter reporter = new ErrorReporter();
        TypeChecker tc = new TypeChecker(reporter);
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromString(moduleDef + spec), reporter);
        boolean tcRes = tc.typeCheck(Arrays.asList(parse), new Vector<>());
        Throwable thrown = Assertions.assertThrows(AnalysisException.class,
                () -> new MableInterpreter(new DefaultExternalValueFactory(new File("target"), tc::findModule, null)).execute(parse));

        Assertions.assertEquals("Load failed", thrown.getMessage());
    }

    @Test
    public void javaClasspathLoadTest() throws AnalysisException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String clz = MyCustomAValue.class.getName();

        String moduleDef = "module A{}";
        String spec = "simulation \n" + "import A;\n" + "{\n" + "A obj = load(\"" + JavaClasspathLoaderLifecycleHandler.class.getAnnotation(
                IValueLifecycleHandler.ValueLifecycle.class).name() + "\",\"" + clz + "\");}";

        ErrorReporter reporter = new ErrorReporter();
        TypeChecker tc = new TypeChecker(reporter);
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromString(moduleDef + spec), reporter);
        boolean tcRes = tc.typeCheck(Arrays.asList(parse), new Vector<>());

        MyCustomAValue.staticValue = 0;
        new MableInterpreter(new DefaultExternalValueFactory(new File("target"), tc::findModule, null)).execute(parse);
        Assertions.assertEquals(999, MyCustomAValue.staticValue.intValue());
    }

    @Test
    public void javaClasspathLoadWithArgsTest() throws AnalysisException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String clz = MyCustomAValue.class.getName();

        String moduleDef = "module A{}";
        String spec = "simulation \n" + "import A;\n" + "{\n" + "A obj = load(\"" + JavaClasspathLoaderLifecycleHandler.class.getAnnotation(
                IValueLifecycleHandler.ValueLifecycle.class).name() + "\",\"" + clz + "\",1000);}";

        ErrorReporter reporter = new ErrorReporter();
        TypeChecker tc = new TypeChecker(reporter);
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromString(moduleDef + spec), reporter);
        boolean tcRes = tc.typeCheck(Arrays.asList(parse), new Vector<>());

        MyCustomAValue.staticValue = 0;
        new MableInterpreter(new DefaultExternalValueFactory(new File("target"), tc::findModule, null)).execute(parse);
        Assertions.assertEquals(1000, MyCustomAValue.staticValue.intValue());
    }

    @Test
    public void javaClasspathLoadAndCallTest() throws AnalysisException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String clz = MyCustomAValue.class.getName();

        String moduleDef = "module A{ int getA();}";
        String spec = "simulation \n" + "import A;\n" + "{\n" + "A obj = load(\"" + JavaClasspathLoaderLifecycleHandler.class.getAnnotation(
                IValueLifecycleHandler.ValueLifecycle.class).name() + "\",\"" + clz + "\"); int v = obj.getA();}";

        ErrorReporter reporter = new ErrorReporter();
        TypeChecker tc = new TypeChecker(reporter);
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromString(moduleDef + spec), reporter);
        boolean tcRes = tc.typeCheck(Arrays.asList(parse), new Vector<>());

        MyCustomAValue.staticValue = 0;
        new MableInterpreter(new DefaultExternalValueFactory(new File("target"), tc::findModule, null)).execute(parse);
        Assertions.assertEquals(999, MyCustomAValue.staticValue.intValue());
    }

    public static class MyCustomAValue extends ExternalModuleValue<Integer> {

        static Integer staticValue;

        public MyCustomAValue(Value module) {
            super(makeMembers(((NumericValue) module).intValue()), ((NumericValue) module).intValue());
        }

        public MyCustomAValue() {
            super(makeMembers(999), 999);
        }

        private static Map<String, Value> makeMembers(Integer value) {
            Map<String, Value> members = new HashMap<>();
            staticValue = value;
            members.put("getA", new FunctionValue.ExternalFunctionValue(fcargs -> new IntegerValue(value)));
            return members;
        }
    }
}