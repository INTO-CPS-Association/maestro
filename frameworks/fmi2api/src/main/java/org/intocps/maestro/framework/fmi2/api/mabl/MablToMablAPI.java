package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.RuntimeModuleVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.intocps.maestro.ast.MableAstFactory.newAIdentifierExp;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class MablToMablAPI {

    private final MablApiBuilder mablApiBuilder;
    private BooleanBuilderFmi2Api booleanBuilderApi;
    private DataWriter dataWriter;
    private MathBuilderFmi2Api mathBuilderFmi2Api;
    private LoggerFmi2Api runtimeLogger;

    public MablToMablAPI(MablApiBuilder mablApiBuilder) {
        this.mablApiBuilder = mablApiBuilder;
    }

    public static Map<MablApiBuilder.FmiStatus, IntVariableFmi2Api> getFmiStatusVariables(TagNameGenerator nameGenerator) {
        Map<MablApiBuilder.FmiStatus, IntVariableFmi2Api> fmiStatusVariables = new HashMap<>();
        Function<String, IntVariableFmi2Api> f = (str) -> new IntVariableFmi2Api(null, null, null, null, newAIdentifierExp(str));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_OK, f.apply("FMI_STATUS_OK"));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_WARNING, f.apply("FMI_STATUS_WARNING"));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_DISCARD, f.apply("FMI_STATUS_DISCARD"));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_ERROR, f.apply("FMI_STATUS_ERROR"));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_FATAL, f.apply("FMI_STATUS_FATAL"));
        fmiStatusVariables.put(MablApiBuilder.FmiStatus.FMI_PENDING, f.apply("FMI_STATUS_PENDING"));
        return fmiStatusVariables;
    }

    public void createExternalRuntimeLogger() {
        String name = "logger";
        RuntimeModuleVariable runtimeModule =
                new RuntimeModuleVariable(null, newANameType("Logger"), null, mablApiBuilder.getDynamicScope(), mablApiBuilder, null,
                        newAIdentifierExp(name), true);
        if (this.runtimeLogger == null) {
            this.runtimeLogger = new LoggerFmi2Api(this.mablApiBuilder, runtimeModule);
        }
        mablApiBuilder.setRuntimeLogger(runtimeLogger);
        mablApiBuilder.getSettings().externalRuntimeLogger = true;
        mablApiBuilder.addExternalLoadedModuleIdentifier(name);
    }

    public DataWriter getDataWriter() {
        if (this.dataWriter == null) {
            this.dataWriter = new DataWriter(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.dataWriter;
    }

    public BooleanBuilderFmi2Api getBooleanBuilder() {
        if (this.booleanBuilderApi == null) {
            this.booleanBuilderApi = new BooleanBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.booleanBuilderApi;
    }

    public MathBuilderFmi2Api getMathBuilder() {
        if (this.mathBuilderFmi2Api == null) {
            this.mathBuilderFmi2Api = new MathBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.mathBuilderFmi2Api;
    }
}
