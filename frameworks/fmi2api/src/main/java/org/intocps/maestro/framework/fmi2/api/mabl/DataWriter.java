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
    private final String FUNCTION_CLOSE = "close";
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

    public DataWriterInstance createDataWriterInstance() {
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
        private final String FUNCTION_WRITEHEADER = "writeHeader";
        private final String FUNCTION_WRITEDATAPOINT = "writeDataPoint";
        private final String TYPE_DATAWRITERCONFIG = "DataWriterConfig";
        private final DynamicActiveBuilderScope dynamicScope;
        private final MablApiBuilder mablApiBuilder;
        private final DataWriter dataWriter;
        private boolean runtimeModuleMode;
        private List<PortFmi2Api> portsToLog;
        /**
         * A String Array of the names of the variables to log
         */
        private ALocalVariableStm logHeadersStm;
        /**
         * The name of the variable containing the array of the names of the variables to log.
         */
        private String logHeadersVariableName;
        /**
         * The statement with the writeHeader function call on the datawriter.
         * This function call returns an instance configuration, which is to be used for subsequent calls to the datawriter
         */
        private ALocalVariableStm writeHeadersStm;
        private boolean initialized;
        /**
         * The name of the variable with the data writer instance configuration
         */
        private String dataWriterInstanceConfigurationVariableName;
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
            this.dataWriterInstanceConfigurationVariableName = mablApiBuilder.getNameGenerator().getName("datawriter_configuration");

            List<AStringLiteralExp> variablesNamesToLog =
                    this.portsToLog.stream().map(x -> MableAstFactory.newAStringLiteralExp(x.getLogScalarVariableName()))
                            .collect(Collectors.toList());

            AVariableDeclaration datawriter_configuration = MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(logHeadersVariableName),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), variablesNamesToLog.size(),
                            MableAstFactory.newAArrayInitializer(variablesNamesToLog));

            this.logHeadersStm = MableAstFactory.newALocalVariableStm(datawriter_configuration);


            this.writeHeadersStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(this.dataWriterInstanceConfigurationVariableName),
                            MableAstFactory.newANameType(TYPE_DATAWRITERCONFIG), MableAstFactory.newAExpInitializer(MableAstFactory
                                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.getModuleIdentifier()),
                                            MableAstFactory.newAIdentifier(FUNCTION_WRITEHEADER),
                                            Arrays.asList(MableAstFactory.newAIdentifierExp(logHeadersVariableName))))));

            if (this.runtimeModuleMode) {
                this.runtimeModule.getDeclaredScope().add(logHeadersStm, writeHeadersStm);
            } else {
                this.mablApiBuilder.getDynamicScope().add(logHeadersStm, writeHeadersStm);
            }

            this.initialized = true;
        }

        public void log(DoubleVariableFmi2Api time) {
            if (!initialized) {
                throw new RuntimeException("DataWriter has not been initialized!");
            }
            AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.moduleIdentifier),
                            MableAstFactory.newAIdentifier(this.FUNCTION_WRITEDATAPOINT), Stream.concat(
                                    Arrays.asList(MableAstFactory.newAIdentifierExp(this.dataWriterInstanceConfigurationVariableName),
                                            time.getReferenceExp().clone()).stream(),
                                    portsToLog.stream().map(x -> x.getSharedAsVariable().getReferenceExp().clone())).collect(Collectors.toList())));
            this.dynamicScope.add(stm);

        }

        public void close() {
            AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.moduleIdentifier),
                            MableAstFactory.newAIdentifier(this.dataWriter.FUNCTION_CLOSE), Arrays.asList()));
            this.dynamicScope.add(stm);
        }

    }
}
