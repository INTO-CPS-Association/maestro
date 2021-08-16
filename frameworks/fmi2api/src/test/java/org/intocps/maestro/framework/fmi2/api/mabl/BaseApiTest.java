package org.intocps.maestro.framework.fmi2.api.mabl;

import com.spencerwi.either.Either;
import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.FunctionValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.VoidValue;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseApiTest {
    public static List<ARootDocument> getModuleDocuments(List<String> modules) throws IOException {
        List<String> allModules = TypeChecker.getRuntimeModules();
        List<ARootDocument> documents = new ArrayList<>();
        if (modules != null) {
            for (String module : modules) {
                if (allModules.contains(module)) {
                    documents.add(getRuntimeModule(module));
                }
            }
        }
        return documents;
    }

    public static ARootDocument getRuntimeModule(String module) throws IOException {
        InputStream resourceAsStream = TypeChecker.getRuntimeModule(module);
        if (resourceAsStream == null) {
            return null;
        }
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromStream(resourceAsStream));
        return parse;
    }

    public void check(String spec,
            String runtimedata) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, AnalysisException {
        IErrorReporter reporter = new ErrorReporter();

        ARootDocument doc = MablParserUtil.parse(CharStreams.fromStream(new ByteArrayInputStream(spec.getBytes())));

        TypeChecker typeChecker = new TypeChecker(reporter);

        List<AImportedModuleCompilationUnit> maestro2EmbeddedModules =
                getModuleDocuments(TypeChecker.getRuntimeModules()).stream().map(x -> NodeCollector.collect(x, AImportedModuleCompilationUnit.class))
                        .filter(Optional::isPresent).flatMap(x -> x.get().stream()).collect(Collectors.toList());
        ARootDocument defaultModules = new ARootDocument();
        defaultModules.setContent(maestro2EmbeddedModules);

        boolean res = typeChecker.typeCheck(Arrays.asList(doc, defaultModules,
                MablParserUtil.parse(CharStreams.fromStream(new MDebugAssert.MDebugAssertRuntime().getMablModule()))), new Vector<>());

        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        if (!res) {
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
        }

        Assertions.assertTrue(res, "Type check errors:" + out);

        new MableInterpreter(
                new DefaultExternalValueFactory(new File("target"), new ByteArrayInputStream(runtimedata.getBytes(StandardCharsets.UTF_8))))
                .execute(doc);
    }

    public static class MDebugAssert {
        private final MablApiBuilder builder;
        private final Fmi2Builder.RuntimeModule<PStm> mdebugAssert;

        public MDebugAssert(MablApiBuilder builder, Fmi2Builder.RuntimeModule<PStm> mdebugAssert) {
            this.builder = builder;
            this.mdebugAssert = mdebugAssert;
        }

        static public MDebugAssert create(MablApiBuilder builder) {
            Fmi2Builder.RuntimeModule<PStm> mdebugAssert = builder.loadRuntimeModule(MDebugAssert.class.getSimpleName());
            return new MDebugAssert(builder, mdebugAssert);

        }

        public void assertEquals(Fmi2Builder.Variable a, Fmi2Builder.Variable b) {
            this.mdebugAssert.callVoid(builder.getFunctionBuilder().addArgument("a", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any)
                    .addArgument("b", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).setName("assertEquals").build(), a, b);
        }

        public void assertEquals(Object a, Fmi2Builder.Variable b) {
            this.mdebugAssert.callVoid(builder.getFunctionBuilder().addArgument("a", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any)
                    .addArgument("b", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).setName("assertEquals").build(), a, b);
        }

        public void assertNotEquals(Fmi2Builder.Variable a, Fmi2Builder.Variable b) {
            this.mdebugAssert.callVoid(builder.getFunctionBuilder().addArgument("a", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any)
                    .addArgument("b", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).setName("assertNotEquals").build(), a, b);
        }

        public void assertNotEquals(Object a, Fmi2Builder.Variable b) {
            this.mdebugAssert.callVoid(builder.getFunctionBuilder().addArgument("a", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any)
                    .addArgument("b", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).setName("assertNotEquals").build(), a, b);
        }

        @IValueLifecycleHandler.ValueLifecycle(name = "MDebugAssert")
        public static class MDebugAssertRuntime implements IValueLifecycleHandler {

            @Override
            public Either<Exception, Value> instantiate(List<Value> args) {
                Map<String, Value> members = getMembers();

                ExternalModuleValue<Map<String, Object>> val = new ExternalModuleValue<>(members, null) {

                };
                return Either.right(val);
            }

            private Map<String, Value> getMembers() {
                Map<String, Value> members = new HashMap<>();
                members.put("assertEquals", new FunctionValue.ExternalFunctionValue(a -> {

                    Assertions.assertTrue(0 == a.get(0).deref().compareTo(a.get(1).deref()), "values does not match");
                    return new VoidValue();
                }));
                members.put("assertNotEquals", new FunctionValue.ExternalFunctionValue(a -> {

                    Assertions.assertFalse(0 == a.get(0).deref().compareTo(a.get(1).deref()), "values does not match");
                    return new VoidValue();
                }));
                return members;
            }

            @Override
            public void destroy(Value value) {

            }

            @Override
            public InputStream getMablModule() {
                return new ByteArrayInputStream(("module " + MDebugAssert.class.getSimpleName() + "{" +
                        getMembers().keySet().stream().map(n -> "void" + " " + n + "(?a,?b)").collect(Collectors.joining(";", "", ";")) + "" + "}")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
