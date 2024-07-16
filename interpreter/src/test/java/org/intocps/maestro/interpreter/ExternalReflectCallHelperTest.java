package org.intocps.maestro.interpreter;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.TP;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExternalReflectCallHelperTest {

    @Test
    public void test() throws NoSuchMethodException {


        ExternalReflectCallHelper helper = new ExternalReflectCallHelper("myFunc9", this);

        var caller = helper.addArg(TP.Int).addArg(TP.Long, 2).build();

        List<Value> args = List.of(NumericValue.valueOf(2),
                new ArrayValue<>(List.of(NumericValue.valueOf(1), NumericValue.valueOf(2), NumericValue.valueOf(3))));
        caller.evaluate(args);

        var rCaller = helper.addReturn(TP.Int).build();
        Assertions.assertEquals(1, ((NumericValue) rCaller.evaluate(args)).intValue());
    }

    @Test
    public void testRet() throws NoSuchMethodException {

        ExternalReflectCallHelper helper = new ExternalReflectCallHelper("myFuncOut1", this);

        var caller = helper.addArg(TP.Long, 2, ExternalReflectCallHelper.ArgMapping.InOut.Output).build();

        List<Value> args = List.of(new UpdatableValue(
                new ArrayValue<>(new Vector<>(List.of(NumericValue.valueOf(1), NumericValue.valueOf(2), NumericValue.valueOf(3))))));
        caller.evaluate(args);

        var rCaller = helper.addReturn(TP.Int).build();
        Assertions.assertEquals(1, ((NumericValue) rCaller.evaluate(args)).intValue());
        Assertions.assertEquals(99, ((NumericValue) args.get(0).as(UpdatableValue.class).deref().as(ArrayValue.class).getValues().get(0)).intValue());
    }

    @Test
    public void autoMapperTest() throws IOException, AnalysisException, NoSuchMethodException {
        ARootDocument doc = MablParserUtil.parse(
                CharStreams.fromStream(ExternalReflectCallHelperTest.class.getResourceAsStream("interpreter_automapper.mabl")));
        IErrorReporter reporter = new ErrorReporter();
        TypeChecker typeChecker = new TypeChecker(reporter);
        boolean res = typeChecker.typeCheck(List.of(doc), new Vector<>());
        PrintWriter writer = new PrintWriter(System.err);
        if (!res) {
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
        }
        writer.flush();
        Assertions.assertTrue(res);

        LinkedList<AFunctionDeclaration> functions = ((AImportedModuleCompilationUnit) doc.getContent().get(0)).getModule().getFunctions();

        List<ExternalReflectCallHelper> helpers = functions.stream().map(f -> new ExternalReflectCallHelper(f, this)).collect(Collectors.toList());

        helpers.forEach(System.out::println);
        helpers.stream().map(h -> h.getSignature(true)).forEach(System.out::println);

        AFunctionDeclaration func = functions.get(0);

        //        AImportedModuleCompilationUnit im = (AImportedModuleCompilationUnit) doc.getContent().get(0);
        //        im.get
        //        ExternalReflectCallHelper helper = new ExternalReflectCallHelper(func, this);
        //        List<Value> args = List.of(NumericValue.valueOf(2),
        //                new ArrayValue<>(List.of(NumericValue.valueOf(1), NumericValue.valueOf(2), NumericValue.valueOf(3))));
        //        System.out.println(helper);
        //
        //
        //        FunctionValue.ExternalFunctionValue call = helper.build();
        //        call.evaluate(args);
        //
        //        func = functions.get(1);
        //        helper = new ExternalReflectCallHelper(func, this);
        //        System.out.println(helper);
        //        args = List.of(NumericValue.valueOf(1), new UpdatableValue(
        //                new ArrayValue<>(new Vector<>(List.of(NumericValue.valueOf(1), NumericValue.valueOf(2), NumericValue.valueOf(3))))));
        //        helper.build().evaluate(args);

        for (AFunctionDeclaration f : functions) {
            ExternalReflectCallHelper helper = new ExternalReflectCallHelper(f, this);
            System.out.println(helper);
            helper.build().evaluate(buildArgs(f));
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
                    v = new ByteValue(1);
                    break;
                case Float:
                    v = new FloatValue(1.1f);
                    break;
                case Int:
                    v = new IntegerValue(1);
                    break;
                case Long:
                    v = new LongValue(1);
                    break;
                case Real:
                    v = new RealValue(3.33);
                    break;
                case Short:
                    v = new ShortValue(Short.parseShort("1"));
                    break;
                case String:
                    v = new StringValue("hello");
                    break;
            }

            if (array) {
                final Value vv = v;
                v = new ArrayValue<>(IntStream.range(1, 4).mapToObj(i -> vv).collect(Collectors.toList()));
            }
            if (updatable) {
                v = new UpdatableValue(v);
            }
            return v;
        }).collect(Collectors.toList());
    }

    public int myFunc9(int ints, long[] longs) {
        Assertions.assertEquals(2, ints);
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, longs);
        return 1;
    }

    public int myFunc1(int ints, long[] longs) {
        Assertions.assertEquals(1, ints);
        Assertions.assertArrayEquals(new long[]{1, 1, 1}, longs);
        return 1;
    }

    public int myFuncOut1(long[] longs) {
        longs[0] = 99;
        return 1;
    }

    public int myFunc2(int arg0, long[] arg1) {
        return 1;
    }

    public int myFunc3(long arg0, long[] arg1) {
        return 1;
    }

    public boolean identityBoolArrCStyle(boolean[] arg0) {
        return true;
    }

    public boolean identityBoolArr(boolean[] arg0) {
        return true;
    }

    public boolean identityBoolArrOut(boolean[] arg0) {
        return true;
    }
}
