package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.PExp;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class LogUtil {
    static PExp simLog(SimLogLevel level, String msg, PExp... args) {

        List<PExp> cargs = new Vector<>();
        cargs.addAll(Arrays.asList(newAIntLiteralExp(level.value), newAStringLiteralExp(msg)));
        for (PExp a : args) {
            cargs.add(a.clone());
        }

        return newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"), cargs);
    }

    enum SimLogLevel {
        TRACE(0),
        DEBUG(2),
        INFO(3),
        WARN(3),
        ERROR(4);

        public final int value;

        private SimLogLevel(int value) {
            this.value = value;
        }
    }
}
