package org.intocps.maestro.framework.fmi2.api.mabl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class EnvTest extends BaseApiTest {
    @Test
    public void test() throws AnalysisException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);

        MDebugAssert assertModule = MDebugAssert.create(builder);

        DynamicActiveBuilderScope dscope = builder.getDynamicScope();

        BooleanVariableFmi2Api trueVar = dscope.store(true);
        BooleanVariableFmi2Api falseVar = dscope.store(false);

        dscope.enterIf(dscope.store(true).toPredicate()).enterThen();
        assertModule.assertEquals(trueVar, dscope.copy("var1", builder.getExecutionEnvironment().getBool("my.true")));
        assertModule.assertEquals(falseVar, builder.getExecutionEnvironment().getBool("my.false"));

        assertModule.assertEquals("string", builder.getExecutionEnvironment().getString("my.string"));
        assertModule.assertEquals(1, builder.getExecutionEnvironment().getInt("my.int"));
        assertModule.assertEquals(123.456, builder.getExecutionEnvironment().getReal("my.double"));


        String spec = PrettyPrinter.print(builder.build());
        System.out.println(spec);

        Map<String, Map<String, Object>> data = new HashMap<>();

        data.put("environment_variables", new HashMap<>() {
            {
                put("my.false", false);
                put("my.true", true);
                put("my.string", "string");
                put("my.int", 1);
                put("my.double", 123.456);
            }
        });

        String runtimeData = new ObjectMapper().writeValueAsString(data);
        check(spec, runtimeData);

    }


}
