package org.intocps.maestro;

import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.intocps.maestro.framework.fmi2.api.mabl.ConsolePrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.LoggerFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.StringVariableFmi2Api;
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
    public void beforeEach(){
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        builder = new MablApiBuilder(settings, false);
        dynamicScope = builder.getDynamicScope();
    }

    @Test
    public void PrintTest() throws Exception {
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
    public void PrintlnTest() throws Exception {
        // Arrange
        String msg = "Test value: ";
        int intTestVal = 1;
        double doubleTestVal = 1.0;
        boolean booleanTestVal = true;
        String stringTestVal = "s";
        Set<String> toContain = Set.of(msg.concat(String.format("%d", intTestVal)), msg.concat(String.format("%.1f", doubleTestVal)),
                msg.concat(String.format("%b", booleanTestVal)), String.format("%s", msg.concat(stringTestVal)));

        IntVariableFmi2Api intTestValVar = dynamicScope.store("intTestVal", intTestVal);
        DoubleVariableFmi2Api doubleTestValVar = dynamicScope.store("doubleTestVal", doubleTestVal);
        BooleanVariableFmi2Api booleanTestValVar = dynamicScope.store("booleanTestVal", booleanTestVal);
        StringVariableFmi2Api stringTestValVar = dynamicScope.store("stringTestVal", stringTestVal);

        ConsolePrinter consolePrinter = builder.getConsolePrinter();
        consolePrinter.println(msg.concat("%d"), intTestValVar);
        consolePrinter.println(msg.concat("%.1f"), doubleTestValVar);
        consolePrinter.println(msg.concat("%s"), stringTestValVar);
        consolePrinter.println(msg.concat("%b"), booleanTestValVar);

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
