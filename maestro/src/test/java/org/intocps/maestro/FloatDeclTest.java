package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.AExpInitializer;
import org.intocps.maestro.ast.node.AFloatNumericPrimitiveType;
import org.intocps.maestro.ast.node.AIdentifierExp;
import org.intocps.maestro.ast.node.ALocalVariableStm;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FloatVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class FloatDeclTest extends BaseApiTest {
    protected FloatDeclTest() {
        super(Assertions::assertTrue, Assertions::assertFalse);
    }

    @Test
    public void test() throws AnalysisException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);

        MDebugAssert assertModule = MDebugAssert.create(builder);

        DynamicActiveBuilderScope dscope = builder.getDynamicScope();

        DoubleVariableFmi2Api r = dscope.store(123.456);
        FloatVariableFmi2Api f = dscope.store(123.456f);

        AExpInitializer aExpInitializer = new AExpInitializer();
        aExpInitializer.setExp(r.getReferenceExp().clone());
        AVariableDeclaration decl = new AVariableDeclaration();
        decl.setName(new LexIdentifier("fr", null));
        decl.setType(new AFloatNumericPrimitiveType());
        decl.setInitializer(aExpInitializer);
        ALocalVariableStm stm = new ALocalVariableStm();
        stm
                .setDeclaration(decl);
        dscope.add(stm);


        assertModule.assertEquals(f, new VariableFmi2Api<>(stm, decl.getType().clone(), dscope,dscope, null, new AIdentifierExp(new LexIdentifier("fr", null))));


        String spec = PrettyPrinter.print(builder.build());
        System.out.println(spec);


        check(spec, "");

    }
}
