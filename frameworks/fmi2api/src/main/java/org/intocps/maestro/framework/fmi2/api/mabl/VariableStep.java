package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class VariableStep {
    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
    private final String FUNCTION_CLOSE = "close";
    private String moduleIdentifier;
    private Fmi2Builder.RuntimeModule<PStm> runtimeModule;
    private boolean runtimeModuleMode = false;


    public VariableStep(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {

        this.dynamicScope = dynamicScope;
        this.mablApiBuilder = mablApiBuilder;
        this.moduleIdentifier = "variableStep";
    }

    public VariableStep(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
        this(dynamicScope, mablApiBuilder);
        this.runtimeModuleMode = true;
        this.runtimeModule = runtimeModule;
        this.moduleIdentifier = runtimeModule.getName();
    }

    public VariableStepInstance createVariableStepInstanceInstance() {
        if (!runtimeModuleMode) {
            return new VariableStepInstance(dynamicScope, mablApiBuilder, this);
        } else {
            return new VariableStepInstance(dynamicScope, mablApiBuilder, this, runtimeModule);
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

    public class VariableStepInstance {
        private final String FUNCTION_SETFMUS = "setFMUs";
        private final String FUNCTION_INITIALIZEPORTNAMES = "initializePortNames";
        private final String FUNCTION_ADDDATAPOINT = "addDataPoint";
        private final String FUNCTION_GETSTEPSIZE = "getStepSize";
        private final String FUNCTION_SETENDTIME = "setEndTime";
        private final String FUNCTION_ISSTEPVALID = "isStepValid";
        private final String TYPE_VARIABLESTEPCONFIG = "VariableStepConfig";
        private final DynamicActiveBuilderScope dynamicScope;
        private final MablApiBuilder mablApiBuilder;
        private final VariableStep variableStep;
        private boolean runtimeModuleMode;
        private boolean initialized;
        /**
         * The name of the variable with the variable step instance configuration
         */
        private Fmi2Builder.RuntimeModule<PStm> runtimeModule;

        public VariableStepInstance(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, VariableStep variableStep) {

            this.dynamicScope = dynamicScope;
            this.mablApiBuilder = mablApiBuilder;
            this.variableStep = variableStep;
        }

        public VariableStepInstance(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, VariableStep variableStep,
                Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
            this(dynamicScope, mablApiBuilder, variableStep);
            this.runtimeModuleMode = true;
            this.runtimeModule = runtimeModule;
        }

        public void initialize(Collection<ComponentVariableFmi2Api> fmus, Collection<PortFmi2Api> ports, DoubleVariableFmi2Api endTime) {

            String fmuInstanceNamesIdentifierName = mablApiBuilder.getNameGenerator().getName("FMUInstanceNames");
            String fmuInstancesIdentifierName = mablApiBuilder.getNameGenerator().getName("fmuInstances");
            String variableStepConfigurationIdentifierName = mablApiBuilder.getNameGenerator().getName("variableStepConfiguration");
            String portNamesIdentifierName = mablApiBuilder.getNameGenerator().getName("portNames");
            ALocalVariableStm fmuNamesStm;
            ALocalVariableStm fmuInstancesStm;
            ALocalVariableStm setFMUsStm;
            ALocalVariableStm portNamesStm;
            AExpressionStm initializePortNamesStm;
            AExpressionStm setEndTimeStm;

            //fmu names exp
            List<AStringLiteralExp> fmuNames = fmus.stream().map(x -> MableAstFactory.newAStringLiteralExp(x.getName())).collect(Collectors.toList());

            fmuNamesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuInstanceNamesIdentifierName),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), fmuNames.size(),
                            MableAstFactory.newAArrayInitializer(fmuNames)));

            //fmu instances exp
            List<PExp> fmuInstances = fmus.stream().map(x -> x.getReferenceExp().clone()).collect(Collectors.toList());
            fmuInstancesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuInstanceNamesIdentifierName),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), fmuInstances.size(),
                            MableAstFactory.newAArrayInitializer(fmuInstances)));

            //setFMUs function
            setFMUsStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(variableStepConfigurationIdentifierName),
                            MableAstFactory.newANameType(TYPE_VARIABLESTEPCONFIG), MableAstFactory.newAExpInitializer(MableAstFactory
                                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                                            MableAstFactory.newAIdentifier(FUNCTION_SETFMUS),
                                            Arrays.asList(MableAstFactory.newAIdentifierExp(fmuInstanceNamesIdentifierName),
                                                    MableAstFactory.newAIdentifierExp(fmuInstancesIdentifierName))))));


            //port names exp
            List<AStringLiteralExp> portNames =
                    ports.stream().map(x -> MableAstFactory.newAStringLiteralExp(x.getName())).collect(Collectors.toList());

            portNamesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(portNamesIdentifierName),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), portNames.size(),
                            MableAstFactory.newAArrayInitializer(portNames)));

            //initializePortNames function
            initializePortNamesStm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(FUNCTION_INITIALIZEPORTNAMES),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifierName),
                                    MableAstFactory.newAIdentifierExp(portNamesIdentifierName))));


            //setEndTime function
            setEndTimeStm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(FUNCTION_SETENDTIME),
                            Arrays.asList(endTime.getExp())));

            if (this.runtimeModuleMode) {
                this.runtimeModule.getDeclaredScope().add(fmuNamesStm, fmuInstancesStm, setFMUsStm, portNamesStm, initializePortNamesStm, setEndTimeStm);
            } else {
                this.mablApiBuilder.getDynamicScope().add(fmuNamesStm, fmuInstancesStm, setFMUsStm, portNamesStm, initializePortNamesStm, setEndTimeStm);
            }

            this.initialized = true;
        }

        public void close() {
            AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.moduleIdentifier),
                            MableAstFactory.newAIdentifier(this.variableStep.FUNCTION_CLOSE), Arrays.asList()));
            this.dynamicScope.add(stm);
        }

    }
}
