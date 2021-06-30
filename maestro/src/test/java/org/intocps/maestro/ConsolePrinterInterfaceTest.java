package org.intocps.maestro;

import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.intocps.maestro.framework.fmi2.api.mabl.ConsolePrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

public class ConsolePrinterInterfaceTest extends BaseApiTest {
    private MablApiBuilder builder;
    private DynamicActiveBuilderScope dynamicScope;

    @BeforeEach
    public void beforeEach() {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        builder = new MablApiBuilder(settings);
        dynamicScope = builder.getDynamicScope();
    }

    @Test
    public void failOnInvalidTypeBeforeRuntimeTest() {
        // Arrange
        String msg = "Invalid test variable: ";
        ConsolePrinter consolePrinter = builder.getConsolePrinter();
        Integer[] arr = new Integer[]{1,2,3};
        ArrayVariableFmi2Api<Integer> invalidVar = dynamicScope.store("InvalidValue", arr);

        // Assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> consolePrinter.print(msg.concat("%d"), invalidVar));
    }

    @Test
    public void printTest() throws Exception {
        // Arrange
        String msg = "Test value: ";
        int testVal = 1;
        String expectedToContain = msg.concat(Integer.toString(testVal));
        ConsolePrinter consolePrinter = builder.getConsolePrinter();
        IntVariableFmi2Api testValVar = dynamicScope.store("testVal", testVal);
        consolePrinter.print(msg.concat("%d"), testValVar);

        String spec = PrettyPrinter.print(builder.build());
        // Create a stream to hold console output
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bs);
        // Save the old stream!
        PrintStream old = System.out;
        // Set new stream
        System.setOut(ps);

        // Act
        check(spec, "");

        // Put things back
        System.out.flush();
        System.setOut(old);
        System.out.println(bs);

        // Assert
        String result = bs.toString();
        Assertions.assertTrue(result.contains(expectedToContain));
    }

    @Test
    public void printRawValuesTest() throws Exception{
        int intTestVal = 1;
        double doubleTestVal = 1.0;
        boolean booleanTestVal = true;
        String stringTestVal = "a string";

        String msg = "All raw values: ";

        String intTestValFormatted = String.format("%d", intTestVal);
        String doubleTestValFormatted = String.format("%.1f", doubleTestVal);
        String booleanTestValFormatted = String.format("%b", booleanTestVal);
        String stringTestValFormatted = String.format("%s", stringTestVal);

        String expectedResult =
                msg.concat(intTestValFormatted + ", ").concat(doubleTestValFormatted + ", ").concat(booleanTestValFormatted + ", ").concat(stringTestValFormatted);

        ConsolePrinter consolePrinter = builder.getConsolePrinter();

        consolePrinter.println(msg.concat("%d, ").concat("%.1f, ").concat("%b, ").concat("%s"), intTestVal, doubleTestVal, booleanTestVal, stringTestVal);

        String spec = PrettyPrinter.print(builder.build());
        // Create a stream to hold console output
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bs);
        // Save the old stream!
        PrintStream old = System.out;
        // Set new stream
        System.setOut(ps);

        // Act
        check(spec, "");

        // Put things back
        System.out.flush();
        System.setOut(old);
        System.out.println(bs);

        // Assert
        String result = bs.toString();
        Assertions.assertTrue(result.contains(expectedResult));
    }

    @Test
    public void printLnTest() throws Exception {
        // Arrange
        String stringMsg = "String value: ";
        String integerMsg = "Integer value: ";
        String doubleMsg = "Double value: ";
        String booleanMsg = "Boolean value: ";
        String allValues = "All values: ";
        int intTestVal = 1;
        double doubleTestVal = 1.0;
        boolean booleanTestVal = true;
        String stringTestVal = "a string";

        String intTestValFormatted = String.format("%d", intTestVal);
        String doubleTestValFormatted = String.format("%.1f", doubleTestVal);
        String booleanTestValFormatted = String.format("%b", booleanTestVal);
        String stringTestValFormatted = String.format("%s", stringTestVal);

        String allValuesConcat =
                allValues.concat(intTestValFormatted + ", ").concat(doubleTestValFormatted + ", ").concat(booleanTestValFormatted + ", ").concat(stringTestValFormatted);

        Set<String> toContain = Set.of(integerMsg.concat(intTestValFormatted), doubleMsg.concat(doubleTestValFormatted),
                booleanMsg.concat(booleanTestValFormatted), stringMsg.concat(stringTestValFormatted), allValuesConcat);



        IntVariableFmi2Api intTestValVar = dynamicScope.store("int_val", intTestVal);
        DoubleVariableFmi2Api doubleTestValVar = dynamicScope.store("double_val", doubleTestVal);
        BooleanVariableFmi2Api booleanTestValVar = dynamicScope.store("boolean_val", booleanTestVal);
        StringVariableFmi2Api stringTestValVar = dynamicScope.store("string_val", stringTestVal);

        ConsolePrinter consolePrinter = builder.getConsolePrinter();
        consolePrinter.println(integerMsg.concat("%d"), intTestValVar);
        consolePrinter.println(doubleMsg.concat("%.1f"), doubleTestValVar);
        consolePrinter.println(stringMsg.concat("%s"), stringTestValVar);
        consolePrinter.println(booleanMsg.concat("%b"), booleanTestValVar);
        consolePrinter.println(allValues.concat("%d, ").concat("%.1f, ").concat("%b, ").concat("%s"), intTestValVar, doubleTestValVar,
                booleanTestValVar, stringTestValVar);

        String spec = PrettyPrinter.print(builder.build());
        // Create a stream to hold console output
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bs);
        // Save the old stream!
        PrintStream old = System.out;
        // Set new stream
        System.setOut(ps);

        // Act
        check(spec, "");

        // Put things back
        System.out.flush();
        System.setOut(old);
        System.out.println(bs);

        // Assert
        String result = bs.toString();
        Assertions.assertTrue(toContain.stream().allMatch(result::contains));
    }
}
