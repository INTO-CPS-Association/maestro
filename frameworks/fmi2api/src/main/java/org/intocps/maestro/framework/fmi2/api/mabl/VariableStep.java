package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class VariableStep {
    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
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
        private final String FUNCTION_HASREDUCEDSTEPSIZE = "hasReducedStepsize";
        private final String FUNCTION_GETREDUCEDSTEPSIZE = "getReducedStepSize";
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

        public BooleanVariableFmi2Api validateStepSize(DoubleVariableFmi2Api nextTime, BooleanVariableFmi2Api supportsRollBack) {
            checkForInitialized();

            PStm targetVarStm;
            List<AAssigmentStm> assignmentStms = new ArrayList<>();
            List<VariableFmi2Api> portsWithData = ports.stream().map(PortFmi2Api::getSharedAsVariable).collect(Collectors.toList());

            for (int i = 0; i < portsWithData.size(); i++) {
                AArrayStateDesignator to = newAArayStateDesignator(newAIdentifierStateDesignator(portsWithDataIdentifier), newAIntLiteralExp(i));
                PExp from = portsWithData.get(i).getReferenceExp().clone();
                assignmentStms.add(newAAssignmentStm(to, from));
            }

            String variableName = dynamicScope.getName("valid_step");
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(
                    newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_ISSTEPVALID),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier), nextTime.getExp(),
                                    supportsRollBack.getExp(),
                                    MableAstFactory.newAIdentifierExp(portsWithDataIdentifier))))));

            this.dynamicScope.add(assignmentStms.toArray(AAssigmentStm[]::new));
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
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newARealNumericPrimitiveType(),
                    newAExpInitializer(newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_GETSTEPSIZE),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier))))));

            this.dynamicScope.add(addDataPointStm, targetVarStm);

            return new DoubleVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                    newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
        }

        public BooleanVariableFmi2Api hasReducedStepsize() {
            checkForInitialized();

            PStm targetVarStm;

            String variableName = dynamicScope.getName("has_reduced_step_size");
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(),
                    newAExpInitializer(newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_HASREDUCEDSTEPSIZE),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier))))));

            this.dynamicScope.add(targetVarStm);

            return new BooleanVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                    newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
        }

        public DoubleVariableFmi2Api getReducedStepSize() {
            checkForInitialized();

            PStm targetVarStm;

            String variableName = dynamicScope.getName("reduced_step_size");
            targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newARealNumericPrimitiveType(),
                    newAExpInitializer(newACallExp(newAIdentifierExp(this.variableStep.getModuleIdentifier()), newAIdentifier(FUNCTION_GETREDUCEDSTEPSIZE),
                            Arrays.asList(MableAstFactory.newAIdentifierExp(variableStepConfigurationIdentifier))))));

            this.dynamicScope.add(targetVarStm);

            return new DoubleVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                    newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
        }

        public void initialize(Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmus, Collection<PortFmi2Api> ports,
                DoubleVariableFmi2Api endTime) {
            this.ports = ports;
            portsWithDataIdentifier = mablApiBuilder.getNameGenerator().getName("ports_with_data_for_varstep");
            variableStepConfigurationIdentifier = mablApiBuilder.getNameGenerator().getName("varstep_config");
            String fmuInstanceNamesIdentifier = mablApiBuilder.getNameGenerator().getName("FMU_instance_names");
            String fmuInstancesIdentifier = mablApiBuilder.getNameGenerator().getName("fmu_instances");
            String portNamesIdentifier = mablApiBuilder.getNameGenerator().getName("portnames_for_varstep");
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
                            portsWithData.size() > 0 ? MableAstFactory.newAArrayInitializer(portsWithData) : null));


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
                            portNames.size() > 0 ? MableAstFactory.newAArrayInitializer(portNames) : null));

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
