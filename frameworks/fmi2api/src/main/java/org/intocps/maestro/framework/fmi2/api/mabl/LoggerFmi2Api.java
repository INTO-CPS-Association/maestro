package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;

public class LoggerFmi2Api {

    final Fmi2Builder.RuntimeModule<PStm> module;

    final Fmi2Builder.RuntimeFunction logFunction;

    public LoggerFmi2Api(MablApiBuilder builder, Fmi2Builder.RuntimeModule<PStm> module) {
        this.module = module;

        logFunction = builder.getFunctionBuilder().setName("log").addArgument("format", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Void).build();
        module.initialize(logFunction);
    }

    public void log(Level level, String format, Object... args) {


        module.call(logFunction, level.level, format, args);
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, format, args);
    }

    public void trace(String format, Object... args) {
        log(Level.TRACE, format, args);
    }

    public void debug(String format, Object... args) {
        log(Level.DEBUG, format, args);
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void error(String format, Object... args) {
        log(Level.ERROR, format, args);
    }

    public void log(ScopeFmi2Api scope, Level level, String format, Object... args) {
        module.callVoid(scope, logFunction, level.level, format, args);
    }

    public void warn(ScopeFmi2Api scope, String format, Object... args) {
        log(scope, Level.WARN, format, args);
    }

    public void trace(ScopeFmi2Api scope, String format, Object... args) {
        log(scope, Level.TRACE, format, args);
    }

    public void debug(ScopeFmi2Api scope, String format, Object... args) {
        log(scope, Level.DEBUG, format, args);
    }

    public void info(ScopeFmi2Api scope, String format, Object... args) {
        log(scope, Level.INFO, format, args);
    }

    public void error(ScopeFmi2Api scope, String format, Object... args) {
        log(scope, Level.ERROR, format, args);
    }


    public enum Level {

        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);
        public int level;

        Level(int level) {
            this.level = level;
        }
    }


}
