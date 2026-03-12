package org.intocps.maestro.framework.fmi2.api.mabl;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.AIdentifierExp;
import org.intocps.maestro.ast.node.AIntLiteralExp;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;

import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.newExpressionStm;

public class LoggerFmi2Api {

    final FmiBuilder.RuntimeModule<PStm> module;

    final FmiBuilder.RuntimeFunction logFunction;

    final FmiBuilder.RuntimeFunction logFmiCallVRefsFunction;

    public LoggerFmi2Api(MablApiBuilder builder, FmiBuilder.RuntimeModule<PStm> module) {
        this.module = module;

        logFunction = builder.getFunctionBuilder().setName("log").addArgument("format", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", FmiBuilder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Void).build();
        module.initialize(logFunction);

        logFmiCallVRefsFunction = builder.getFunctionBuilder().setName("logFmiCallVRefs").addArgument("vr", FmiBuilder.RuntimeFunction.FunctionType.Type.Any).addArgument("nvr", FmiBuilder.RuntimeFunction.FunctionType.Type.Int).addArgument("format", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", FmiBuilder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Void).build();
        module.initialize(logFmiCallVRefsFunction);
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

    public void logFmiCallVRefs(ScopeFmi2Api scope,PExp vr, PExp nvr, String format, Object... args){
       // module.call(logFmiCallVRefsFunction,Level.ERROR, vr,nvr,format, args);
        var arguments = new Vector<>(Arrays.asList(args));
//        arguments.addFirst(Level.ERROR);
        arguments.addFirst(format);
        PStm stm = newExpressionStm(MableBuilder.call(module.getExp().clone(),logFmiCallVRefsFunction.getName(),
                Stream.concat(Stream.of(new AIntLiteralExp(new LexLocation("",0,0),Level.ERROR.level),vr,nvr), BuilderUtil.toExp(arguments.toArray()).stream()).map(PExp::clone).collect(Collectors.toList())));
        scope.add(stm);
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
