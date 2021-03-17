package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.StringVariableFmi2Api;

import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;

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
        private Collection<PortFmi2Api> ports;
        /**
         * The name of the variable with the variable step instance configuration
         */
        private Fmi2Builder.RuntimeModule<PStm> runtimeModule;

        private String variableStepConfigurationIdentifier;
        private String portsWithDataIdentifier;

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

        private void checkForInitialized() {
            if (!initialized) {
                throw new RuntimeException("VariableStep has not been initialized!");
            }
        }

        public BooleanVariableFmi2Api validateStepSize(DoubleVariableFmi2Api nextTime) {
            AAssigmentStm assignUpdatedValuesStm;
            PStm targetVarStm;
            List<PExp> portsWithData = ports.stream().map(p -> p.getSharedAsVariable().getReferenceExp().clone()).collect(Collectors.toList());
            //            assignUpdatedValuesStm = newAAssignmentStm(newAIdentifierStateDesignator(portsWithDataIdentifierName),
            //                    call(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()), "getReal",
            //                            vrefBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) portsWithData.size()), valBuf.getReferenceExp().clone()));



            String variableName = dynamicScope.getName("validStep");
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(
                    newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_ISSTEPVALID),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier), nextTime.getExp(),
                                    MableAstFactory.newAIdentifierExp(portsWithDataIdentifier))))));

            this.dynamicScope.add(targetVarStm);

            return new BooleanVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                    newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
        }

        public DoubleVariableFmi2Api getStepSize(DoubleVariableFmi2Api simTime) {
            checkForInitialized();

            AExpressionStm addDataPointStm;
            PStm targetVarStm;

            addDataPointStm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(FUNCTION_ADDDATAPOINT),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier), simTime.getExp(),
                                    MableAstFactory.newAIdentifierExp(portsWithDataIdentifier))));


            String variableName = dynamicScope.getName("var_step_size");
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newARealNumericPrimitiveType(), newAExpInitializer(
                    newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_GETSTEPSIZE),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier))))));

            this.dynamicScope.add(addDataPointStm, targetVarStm);

            return new DoubleVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                    newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
        }

        public void initialize(Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmus, Collection<PortFmi2Api> ports,
                DoubleVariableFmi2Api endTime) {
            this.ports = ports;
            portsWithDataIdentifier = mablApiBuilder.getNameGenerator().getName("portsWithDataForVarStep");
            variableStepConfigurationIdentifier = mablApiBuilder.getNameGenerator().getName("varStepConfig");
            String fmuInstanceNamesIdentifier = mablApiBuilder.getNameGenerator().getName("FMUInstanceNames");
            String fmuInstancesIdentifier = mablApiBuilder.getNameGenerator().getName("fmuInstances");
            String portNamesIdentifier = mablApiBuilder.getNameGenerator().getName("portNamesForVarStep");
            ALocalVariableStm fmuNamesStm;
            ALocalVariableStm fmuInstancesStm;
            ALocalVariableStm setFMUsStm;
            ALocalVariableStm portNamesStm;
            AExpressionStm initializePortNamesStm;
            AExpressionStm setEndTimeStm;
            ALocalVariableStm portsWithDataStm;

            //ports with data variable
            List<PExp> portsWithData = ports.stream().map(p -> p.getSharedAsVariable().getReferenceExp().clone()).collect(Collectors.toList());
            portsWithDataStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(portsWithDataIdentifier),
                            MableAstFactory.newAArrayType(MableAstFactory.newARealNumericPrimitiveType()), portsWithData.size(),
                            MableAstFactory.newAArrayInitializer(portsWithData)));


            //fmu names variable
            List<PExp> fmuNames = fmus.entrySet().stream().map(v -> v.getKey().getExp()).collect(Collectors.toList());

            fmuNamesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuInstanceNamesIdentifier),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), fmuNames.size(),
                            MableAstFactory.newAArrayInitializer(fmuNames)));

            //fmu instances variable
            List<PExp> fmuInstances = fmus.values().stream().map(x -> x.getReferenceExp().clone()).collect(Collectors.toList());
            fmuInstancesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuInstancesIdentifier),
                            MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")), fmuInstances.size(),
                            MableAstFactory.newAArrayInitializer(fmuInstances)));

            //setFMUs function
            setFMUsStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(variableStepConfigurationIdentifier),
                            MableAstFactory.newANameType(TYPE_VARIABLESTEPCONFIG), MableAstFactory.newAExpInitializer(MableAstFactory
                                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                                            MableAstFactory.newAIdentifier(FUNCTION_SETFMUS),
                                            Arrays.asList(MableAstFactory.newAIdentifierExp(fmuInstanceNamesIdentifier),
                                                    MableAstFactory.newAIdentifierExp(fmuInstancesIdentifier))))));


            //port names variable
            List<AStringLiteralExp> portNames =
                    ports.stream().map(p -> MableAstFactory.newAStringLiteralExp(p.getLogScalarVariableName())).collect(Collectors.toList());

            portNamesStm = MableAstFactory.newALocalVariableStm(MableAstFactory
                    .newAVariableDeclaration(MableAstFactory.newAIdentifier(portNamesIdentifier),
                            MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), portNames.size(),
                            MableAstFactory.newAArrayInitializer(portNames)));

            //initializePortNames function
            initializePortNamesStm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(FUNCTION_INITIALIZEPORTNAMES),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier),
                                    MableAstFactory.newAIdentifierExp(portNamesIdentifier))));


            //setEndTime function
            setEndTimeStm = MableAstFactory.newExpressionStm(MableAstFactory
                    .newACallExp(MableAstFactory.newAIdentifierExp(this.variableStep.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(FUNCTION_SETENDTIME),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier), endTime.getExp())));

            if (this.runtimeModuleMode) {
                this.runtimeModule.getDeclaredScope()
                        .add(fmuNamesStm, fmuInstancesStm, setFMUsStm, portNamesStm, initializePortNamesStm, setEndTimeStm, portsWithDataStm);
            } else {
                this.mablApiBuilder.getDynamicScope()
                        .add(fmuNamesStm, fmuInstancesStm, setFMUsStm, portNamesStm, initializePortNamesStm, setEndTimeStm, portsWithDataStm);
            }

            this.initialized = true;
        }

    }
}
