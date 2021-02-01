package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class DataWriter {

    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
    private String moduleIdentifier;
    private Fmi2Builder.RuntimeModule<PStm> runtimeModule;
    private boolean runtimeModuleMode = false;


    public DataWriter(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {

        this.dynamicScope = dynamicScope;
        this.mablApiBuilder = mablApiBuilder;
        this.moduleIdentifier = "dataWriter";
    }

    public DataWriter(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
        this(dynamicScope, mablApiBuilder);
        this.runtimeModuleMode = true;
        this.runtimeModule = runtimeModule;
        this.moduleIdentifier = runtimeModule.getName();
    }

    public DataWriterInstance CreateDataWriterInstance() {
        if (!runtimeModuleMode) {
            return new DataWriterInstance(dynamicScope, mablApiBuilder, this);
        } else {
            return new DataWriterInstance(dynamicScope, mablApiBuilder, this, runtimeModule);
        }
    }

    public String getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void unload() {
        mablApiBuilder.getDynamicScope().add(newExpressionStm(newUnloadExp(Arrays.asList(getReferenceExp().clone()))));
    }

    private PExp getReferenceExp() {
        return newAIdentifierExp(moduleIdentifier);
    }

    // TODO: Only works for shared variables. Fixed this.
    public class DataWriterInstance {
        private final String writeHeaderFunctionName = "writeHeader";
        private final String writeDataPointFunctionName = "writeDataPoint";
        private final DynamicActiveBuilderScope dynamicScope;
        private final MablApiBuilder mablApiBuilder;
        private final DataWriter dataWriter;
        private boolean runtimeModuleMode;
        private List<PortFmi2Api> portsToLog;
        private ALocalVariableStm logHeadersStm;
        private String logHeadersVariableName;
        private ALocalVariableStm writeHeadersStm;
        private boolean initialized;
        private String logConfigurationVariableName;
        private Fmi2Builder.RuntimeModule<PStm> runtimeModule;

        public DataWriterInstance(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, DataWriter dataWriter) {

            this.dynamicScope = dynamicScope;
            this.mablApiBuilder = mablApiBuilder;
            this.dataWriter = dataWriter;
        }

        public DataWriterInstance(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, DataWriter dataWriter,
                Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
            this(dynamicScope, mablApiBuilder, dataWriter);
            this.runtimeModuleMode = true;
            this.runtimeModule = runtimeModule;
        }

        public void initialize(PortFmi2Api... portsToLog) {
            this.initialize(Arrays.asList(portsToLog));
        }

        public void initialize(List<PortFmi2Api> portsToLog) {

            this.portsToLog = portsToLog;

            this.logHeadersVariableName = mablApiBuilder.getNameGenerator().getName("datawriter_headers");
            this.logConfigurationVariableName = mablApiBuilder.getNameGenerator().getName("datawriter_configuration");
            List<AStringLiteralExp> variablesNamesToLog =
                    this.portsToLog.stream().map(x -> MableAstFactory.newAStringLiteralExp(x.getLogScalarVariableName()))
                            .collect(Collectors.toList());
            AVariableDeclaration datawriter_configuration = MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(logHeadersVariableName),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), variablesNamesToLog.size(),
                            MableAstFactory.newAArrayInitializer(variablesNamesToLog));
            this.logHeadersStm = MableAstFactory.newALocalVariableStm(datawriter_configuration);


            this.writeHeadersStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(this.logConfigurationVariableName),
                            MableAstFactory.newANameType("DataWriterConfig"), MableAstFactory.newAExpInitializer(MableAstFactory
                                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.getModuleIdentifier()),
                                            MableAstFactory.newAIdentifier(writeHeaderFunctionName),
                                            Arrays.asList(MableAstFactory.newAIdentifierExp(logHeadersVariableName))))));

            if (this.runtimeModuleMode) {
                this.runtimeModule.getDeclaredScope().add(logHeadersStm, writeHeadersStm);
            } else {
                this.mablApiBuilder.getRootScope().add(logHeadersStm, writeHeadersStm);
            }

            this.initialized = true;
        }

        public void Log(DoubleVariableFmi2Api time) {
            if (!initialized) {
                throw new RuntimeException("DataWriter has not been initialized!");
            }
            AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.moduleIdentifier),
                            MableAstFactory.newAIdentifier(this.writeDataPointFunctionName), Stream.concat(
                                    Arrays.asList(MableAstFactory.newAIdentifierExp(this.logConfigurationVariableName),
                                            time.getReferenceExp().clone()).stream(),
                                    portsToLog.stream().map(x -> x.getSharedAsVariable().getReferenceExp().clone())).collect(Collectors.toList())));
            this.dynamicScope.add(stm);

        }
    }
}
