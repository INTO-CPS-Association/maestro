package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AExpressionStm;
import org.intocps.maestro.ast.node.ALocalVariableStm;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataWriter {

    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
    private final String moduleIdentifier;


    public DataWriter(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {

        this.dynamicScope = dynamicScope;
        this.mablApiBuilder = mablApiBuilder;
        this.moduleIdentifier = "dataWriter";
    }

    public DataWriterInstance CreateDataWriterInstance() {
        return new DataWriterInstance(dynamicScope, mablApiBuilder, this);
    }

    public String getModuleIdentifier() {
        return moduleIdentifier;
    }

    public class DataWriterInstance {
        private final DynamicActiveBuilderScope dynamicScope;
        private final MablApiBuilder mablApiBuilder;
        private final DataWriter dataWriter;
        private final String writeHeaderFunctionName = "writeHeader";
        private final String writeDataPointFunctionName = "writeDataPoint";
        private List<PortFmi2Api> portsToLog;
        private ALocalVariableStm logHeadersStm;
        private String logHeadersVariableName;
        private ALocalVariableStm writeHeadersStm;
        private boolean initialized;
        private String logConfigurationVariableName;

        public DataWriterInstance(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, DataWriter dataWriter) {

            this.dynamicScope = dynamicScope;
            this.mablApiBuilder = mablApiBuilder;
            this.dataWriter = dataWriter;
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

            this.mablApiBuilder.getRootScope().add(logHeadersStm, writeHeadersStm);


            this.initialized = true;
        }

        public void Log(DoubleVariableFmi2Api time) {
            AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.dataWriter.moduleIdentifier),
                            MableAstFactory.newAIdentifier(this.writeDataPointFunctionName), Stream.concat(
                                    Arrays.asList(MableAstFactory.newAIdentifierExp(this.logConfigurationVariableName), time.getReferenceExp())
                                            .stream(), portsToLog.stream().map(x -> x.getSharedAsVariable().getReferenceExp()))
                                    .collect(Collectors.toList())));
            this.dynamicScope.add(stm);

        }
    }
}
