package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.junit.jupiter.api.Test;

public class ParallelScopeTest {
    @Test
    public void test() throws AnalysisException {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);

        DynamicActiveBuilderScope scope = builder.getDynamicScope();

        scope.store("a", 1);
        scope.store("b", 1);
        scope.parallel().activate();
        scope.store("c", 1);
        scope.store("d", 1);
        scope.leave();
        scope.store("e", 1);


        String spec = PrettyPrinter.print(builder.build());
        System.out.println(spec);
    }
}
