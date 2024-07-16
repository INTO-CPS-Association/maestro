package org.intocps.maestro.interpreter;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.jnifmuapi.fmi3.Fmi3Instance;
import org.intocps.fmi.jnifmuapi.fmi3.Fmu3;
import org.intocps.maestro.ast.AEqualBinaryExp;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.ExternalReflectModuleHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.external.TP;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Disabled
public class ExternalReflectCallHelperAutoTest {
    public static ARootDocument getRuntimeModule(String module) throws IOException {
        InputStream resourceAsStream = TypeChecker.getRuntimeModule(module);
        if (resourceAsStream == null) {
            return null;
        }
        ARootDocument parse = MablParserUtil.parse(CharStreams.fromStream(resourceAsStream));
        return parse;
    }

    List<AImportedModuleCompilationUnit> parse(InputStream is) throws IOException, AnalysisException {
        ARootDocument doc = MablParserUtil.parse(CharStreams.fromStream(is));
        IErrorReporter reporter = new ErrorReporter();
        TypeChecker typeChecker = new TypeChecker(reporter);

        List<String> allModules = TypeChecker.getRuntimeModules();
        List<ARootDocument> documents = new ArrayList<>();
        documents.add(doc);
        documents.add(MablParserUtil.parse(CharStreams.fromStream(new ByteArrayInputStream("simulation{}".getBytes(StandardCharsets.UTF_8)))));

        List<String> imports = doc.getContent().stream().filter(m -> m instanceof AImportedModuleCompilationUnit)
                .flatMap(m -> ((AImportedModuleCompilationUnit) m).getImports().stream()).map(LexIdentifier::getText).collect(Collectors.toList());
        if (imports != null) {
            for (String module : imports) {
                if (allModules.contains(module)) {
                    documents.add(getRuntimeModule(module));
                }
            }
        }

        boolean res = typeChecker.typeCheck(documents, new Vector<>());
        PrintWriter writer = new PrintWriter(System.err);
        if (!res) {
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
        }
        writer.flush();
        Assertions.assertTrue(res);
        return doc.getContent().stream().filter(m -> m instanceof AImportedModuleCompilationUnit).map(AImportedModuleCompilationUnit.class::cast)
                .collect(Collectors.toList());
    }

    @Test

    public void test() throws IOException, AnalysisException, NoSuchMethodException, FmuInvocationException {
        String kk = "../src/main/resources/org/intocps/maestro/typechecker/FMI3.mabl";
        List<AImportedModuleCompilationUnit> modules = parse(new FileInputStream(kk));


        for (AImportedModuleCompilationUnit module : modules) {

            if (module.getModule().getName().getText().equals("FMI3")) {
                //                var fmu = new Fmu3(new File("/Users/kgl/data/au/into-cps-association/maestro/maestro/src/test/resources/singlewatertank-20sim.fmu"));
                //                ExternalReflectModuleHelper.createExternalModule(module.getModule(), fmu);
            } else if (module.getModule().getName().getText().equals("FMI3Instance")) {
                var fmu = new Fmu3(new File("..//maestro/src/test/resources/singlewatertank-20sim.fmu"));
                Fmi3Instance inst = new Fmi3Instance(0, fmu);
                ExternalReflectModuleHelper.createExternalModule(module.getModule(), inst,
                        fun -> !fun.getName().getText().equals("enterInitializationMode") && !fun.getName().getText()
                                .startsWith("get") && !fun.getName().getText().equals("setBinary"));
            }

        }
    }

    @Test
    public void autoMapperTest() throws IOException, AnalysisException, NoSuchMethodException {
        List<AImportedModuleCompilationUnit> modules = parse(
                ExternalReflectCallHelperAutoTest.class.getResourceAsStream("interpreter_automapper.mabl"));


        LinkedList<AFunctionDeclaration> functions = modules.get(0).getModule().getFunctions();

        List<ExternalReflectCallHelper> helpers = functions.stream().map(f -> new ExternalReflectCallHelper(f, this, null))
                .collect(Collectors.toList());

        helpers.forEach(System.out::println);
        helpers.stream().map(h -> h.getSignature(true)).forEach(System.out::println);

        Interpreter interpreter = new Interpreter(null);

        final AEqualBinaryExp eq = new AEqualBinaryExp(new AIdentifierExp(new LexIdentifier("a", null)),
                new AIdentifierExp(new LexIdentifier("b", null)));
        for (AFunctionDeclaration f : functions) {
            ExternalReflectCallHelper helper = new ExternalReflectCallHelper(f, this);
            System.out.println(helper);
            List<Value> args = buildArgs(f);
            helper.build().evaluate(args);
            List<Value> argsRef = buildArgs(f);

            for (int i = 0; i < helper.size(); i++) {
                IArgMapping arg = helper.get(i);


                if (arg.getDirection() == ExternalReflectCallHelper.ArgMapping.InOut.Output) {
                    Value a = (Value) args.get(i).as(UpdatableValue.class).deref().as(ArrayValue.class).getValues().get(0);
                    Value b = (Value) argsRef.get(i).as(UpdatableValue.class).deref().as(ArrayValue.class).getValues().get(0);
                    Context c = new Context(null);
                    c.put(new AIdentifierExp(new LexIdentifier("a", null)), a);
                    c.put(new AIdentifierExp(new LexIdentifier("b", null)), b);
                    Assertions.assertFalse(((BooleanValue) interpreter.caseAEqualBinaryExp(eq, c)).getValue(), "value should have changed");

                    c.put(new AIdentifierExp(new LexIdentifier("a", null)), b);
                    c.put(new AIdentifierExp(new LexIdentifier("b", null)), b);
                    Assertions.assertTrue(((BooleanValue) interpreter.caseAEqualBinaryExp(eq, c)).getValue(), "value should not have changed");


                }
            }
        }
    }

    public List<Value> buildArgs(AFunctionDeclaration func) {
        return func.getFormals().stream().map(AFormalParameter::getType).map(t -> {


            boolean updatable = t instanceof AReferenceType;
            if (t instanceof AReferenceType) {
                t = ((AReferenceType) t).getType();
            }

            boolean array = t instanceof AArrayType;
            Map.Entry<TP, Integer> tt = null;
            try {
                tt = ExternalReflectCallHelper.getReverseType(t);
            } catch (ExternalReflectCallHelper.ExceptionUnknownTypeMapping e) {
                throw new RuntimeException(e);
            }
            Value v = null;
            switch (tt.getKey()) {
                case Bool:
                    v = new BooleanValue(false);
                    break;
                case Byte:
                    v = new ByteValue(0);
                    break;
                case Float:
                    v = new FloatValue(0);
                    break;
                case Int:
                    v = new IntegerValue(0);
                    break;
                case Long:
                    v = new LongValue(0);
                    break;
                case Real:
                    v = new RealValue(0);
                    break;
                case Short:
                    v = new ShortValue(Short.parseShort("0"));
                    break;
                case String:
                    v = new StringValue("hello");
                    break;
            }

            if (array) {
                v = new ArrayValue<>(new Vector<>(List.of(v)));
            }
            if (updatable) {
                v = new UpdatableValue(v);
            }
            return v;
        }).collect(Collectors.toList());
    }

    public int myFunc1(int arg0, long[] arg1) {
        return 1;
    }

    public int myFunc2(int arg0, long[] arg1) {
        arg1[0] = 1;
        return 1;
    }

    public int myFunc3(int arg0, long[] arg1) {
        arg1[0] = 1;
        return 1;
    }

    public boolean identityBoolArrCStyle(boolean[] arg0) {
        return true;
    }

    public boolean identityBoolArr(boolean[] arg0) {
        return true;
    }

    public boolean identityBoolArrOut(boolean[] arg0) {
        arg0[0] = true;
        return true;
    }
}
