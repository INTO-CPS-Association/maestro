package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3TypeEnum;
import org.intocps.maestro.framework.core.RelationVariable;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.PortValueExpresssionMapImpl;
import org.intocps.maestro.framework.fmi2.api.mabl.values.PortValueMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

@SuppressWarnings("rawtypes")
public class InstanceVariableFmi3Api extends VariableFmi2Api<FmiBuilder.NamedVariable<PStm>> implements FmiBuilder.Fmi3InstanceVariable<PStm, Fmi3ModelDescription.Fmi3ScalarVariable> {
    final static Logger logger = LoggerFactory.getLogger(InstanceVariableFmi3Api.class);
    private final static int FMI_STATUS_LAST_SUCCESSFUL = 2;
    private static final String LOGLEVELS_POSTFIX = "_log_levels";
    private final static String CATEGORY_STATUS = "category_status";
    final List<PortFmi3Api> outputPorts;
    final List<PortFmi3Api> inputPorts;
    final List<PortFmi3Api> ports;
    private final FmuVariableFmi3Api owner;
    private final String name;
    private final MablApiBuilder builder;
    //    private final Map<IMablScope, Map<PType, ArrayVariableFmi2Api<Object>>> tentativeBuffer = new HashMap<>();
//    private final Map<IMablScope, Map<PortFmi3Api, Integer>> tentativeBufferIndexMap = new HashMap<>();
    private final String environmentName;


    private final Buffers<Fmi3TypeEnum> buffers;
    Predicate<FmiBuilder.Port> isLinked = p -> ((PortFmi3Api) p).getSourcePort() != null;
    ModelDescriptionContext3 modelDescriptionContext;
    private ArrayVariableFmi2Api<Object> derSharedBuffer;
    private DoubleVariableFmi2Api currentTimeVar = null;
    private BooleanVariableFmi2Api currentTimeStepFullStepVar = null;
    private ArrayVariableFmi2Api<Object> valueRefBuffer;
    private Map<PortFmi3Api, List<VariableFmi2Api<Object>>> derivativePortsToShare;
    private List<String> variablesToLog;

    Predicate<FmiBuilder.Port> obtainAsShared = p -> p.isLinked() || (variablesToLog != null && variablesToLog.contains(p.getName()));

    public InstanceVariableFmi3Api(PStm declaration, FmuVariableFmi3Api parent, String name, ModelDescriptionContext3 modelDescriptionContext,
                                   MablApiBuilder builder, IMablScope declaringScope, PStateDesignator designator, PExp referenceExp) {
        this(declaration, parent, name, modelDescriptionContext, builder, declaringScope, designator, referenceExp, name);
    }

    public InstanceVariableFmi3Api(PStm declaration, FmuVariableFmi3Api parent, String name, ModelDescriptionContext3 modelDescriptionContext,
                                   MablApiBuilder builder, IMablScope declaringScope, PStateDesignator designator, PExp referenceExp, String environmentName) {
        super(declaration, newANameType("FMI3Instance"), declaringScope, builder.getDynamicScope(), designator, referenceExp);
        this.owner = parent;
        this.name = name;

        this.modelDescriptionContext = modelDescriptionContext;
        this.builder = builder;

        ports = modelDescriptionContext.nameToSv.values().stream().map(sv -> new PortFmi3Api(this, sv))
                .sorted(Comparator.comparing(PortFmi3Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        outputPorts = ports.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .sorted(Comparator.comparing(PortFmi3Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        inputPorts = ports.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Input)
                .sorted(Comparator.comparing(PortFmi3Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        this.environmentName = environmentName;

        Map<Fmi3TypeEnum, Integer> svTypeMaxCount = modelDescriptionContext.valRefToSv.values()
                .stream().collect(Collectors.groupingBy(i -> i.getVariable().getTypeIdentifier())).entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, l -> l.getValue().size()));
        this.buffers = new Buffers<>(builder, name, getDeclaringStm(), (ScopeFmi2Api) getDeclaredScope(), svTypeMaxCount);
    }

    public Fmi3ModelDescription getModelDescription() {
        return modelDescriptionContext.getModelDescription();
    }

    public List<PortFmi3Api> getVariablesToLog() {
        return this.getPorts(this.variablesToLog.toArray(new String[0]));
    }

    public void setVariablesToLog(List<? extends RelationVariable> variablesToLog) {
        this.variablesToLog = variablesToLog.stream().map(RelationVariable::getName).collect(Collectors.toList());
    }

    @Override
    public <V> void share(FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> port, FmiBuilder.Variable<PStm, V> value) {
        Map<FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, FmiBuilder.Variable<PStm, V>> map = new HashMap<>();
        map.put(port, value);
        share(map);
    }

    private DoubleVariableFmi2Api getCurrentTimeVar() {
        if (currentTimeVar == null) {
            String name = builder.getNameGenerator().getName(this.name, "current", "time");
            PStm var = newVariable(name, newRealType(), newARealLiteralExp(0d));
            this.getDeclaredScope().addAfter(this.getDeclaringStm(), var);
            currentTimeVar = new DoubleVariableFmi2Api(var, this.getDeclaredScope(), dynamicScope, newAIdentifierStateDesignator(name),
                    newAIdentifierExp(name));
        }
        return currentTimeVar;
    }

    private BooleanVariableFmi2Api getCurrentTimeFullStepVar() {
        if (currentTimeStepFullStepVar == null) {
            String name = builder.getNameGenerator().getName(this.name, "current", "time", "full", "step");
            PStm var = newVariable(name, newBoleanType(), newABoolLiteralExp(true));
            this.getDeclaredScope().addAfter(this.getDeclaringStm(), var);

            currentTimeStepFullStepVar = new BooleanVariableFmi2Api(var, this.getDeclaredScope(), dynamicScope, newAIdentifierStateDesignator(name),
                    newAIdentifierExp(name));
        }
        return currentTimeStepFullStepVar;
    }

    private ArrayVariableFmi2Api<Object> getValueReferenceBuffer() {
        if (this.valueRefBuffer == null) {
            this.valueRefBuffer = createBuffer(newUIntType(), "VRef", modelDescriptionContext.valRefToSv.size(), getDeclaredScope());
        }
        return this.valueRefBuffer;
    }

//    private ArrayVariableFmi2Api<Object> getBuffer(Map<PType, ArrayVariableFmi2Api<Object>> buffer, PType type, String prefix, int size,
//                                                   IMablScope scope) {
//        Optional<PType> first = buffer.keySet().stream().filter(x -> x.toString().equals(type.toString())).findFirst();
//        if (first.isEmpty()) {
//            ArrayVariableFmi2Api<Object> value = createBuffer(type, prefix, size, scope);
//            buffer.put(type, value);
//            return value;
//
//        } else {
//            return buffer.get(first.get());
//        }
//    }


    @SuppressWarnings("unchecked")
    private ArrayVariableFmi2Api<Object> getSharedDerBuffer() {
        if (this.derSharedBuffer == null) {
            try {
                this.derSharedBuffer = createDerValBuffer("DerShare", List.of(1, getModelDescription().getMaxOutputDerivativeOrder()));
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Exception occurred when accessing modeldescription: " + e);
            }
        }
        return this.derSharedBuffer;
    }


    @SuppressWarnings("unchecked")
    private ArrayVariableFmi2Api createMDArrayRecursively(List<Integer> arraySizes, PStm declaringStm, PStateDesignatorBase stateDesignator,
                                                          PExpBase indexExp) {

        if (arraySizes.size() > 1) {
            List<VariableFmi2Api> arrays = new ArrayList<>();
            for (int i = 0; i < arraySizes.get(0); i++) {
                arrays.add(createMDArrayRecursively(arraySizes.subList(1, arraySizes.size()), declaringStm,
                        newAArayStateDesignator(stateDesignator, newAIntLiteralExp(i)), newAArrayIndexExp(indexExp, List.of(newAIntLiteralExp(i)))));
            }
            return new ArrayVariableFmi2Api(declaringStm, arrays.get(0).getType(), getDeclaredScope(), builder.getDynamicScope(), stateDesignator,
                    indexExp.clone(), arrays);
        }

        List<VariableFmi2Api<Object>> variables = new ArrayList<>();
        for (int i = 0; i < arraySizes.get(0); i++) {
            variables.add(new VariableFmi2Api<>(declaringStm, type, getDeclaredScope(), builder.getDynamicScope(),
                    newAArayStateDesignator(stateDesignator.clone(), newAIntLiteralExp(i)),
                    newAArrayIndexExp(indexExp.clone(), List.of(newAIntLiteralExp(i)))));
        }
        return new ArrayVariableFmi2Api<>(declaringStm, variables.get(0).getType(), getDeclaredScope(), builder.getDynamicScope(),
                ((AArrayStateDesignator) variables.get(0).getDesignatorClone()).getTarget(), indexExp.clone(), variables);
    }

    private ArrayVariableFmi2Api createDerValBuffer(String identifyingName, List<Integer> lengths) {
        String bufferName = builder.getNameGenerator().getName(this.name, identifyingName);

        PStm arrayVariableStm = newALocalVariableStm(
                newAVariableDeclarationMultiDimensionalArray(newAIdentifier(bufferName), newARealNumericPrimitiveType(), lengths));

        getDeclaredScope().addAfter(getDeclaringStm(), arrayVariableStm);

        return createMDArrayRecursively(lengths, arrayVariableStm, newAIdentifierStateDesignator(newAIdentifier(bufferName)),
                newAIdentifierExp(bufferName));
    }

    private ArrayVariableFmi2Api<Object> createBuffer(PType type, String prefix, int length, IMablScope scope) {

        //lets find a good place to store the buffer.
        String ioBufName = builder.getNameGenerator().getName(this.name, type + "", prefix);

        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length, null));

        getDeclaredScope().addAfter(getDeclaringStm(), var);

        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(
                        i -> new VariableFmi2Api<>(var, type, scope, builder.getDynamicScope(),
                                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                                newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i)))))
                .collect(Collectors.toList());

        return new ArrayVariableFmi2Api<>(var, type, scope, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName), items);
    }

    BooleanVariableFmi2Api createReusableBooleanVariable(boolean initial, IMablScope scope, String... name) {

        String ioBufName = builder.getNameGenerator().getName(name);

        PType type = new ABooleanPrimitiveType();
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, new AExpInitializer(new ABoolLiteralExp(initial))));

        getDeclaredScope().addAfter(getDeclaringStm(), var);

        return new BooleanVariableFmi2Api(var, scope, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName));
    }

    DoubleVariableFmi2Api createReusableDoubleVariable(double initial, IMablScope scope, String... name) {

        String ioBufName = builder.getNameGenerator().getName(name);

        PType type = new ARealNumericPrimitiveType();
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, new AExpInitializer(new ARealLiteralExp(initial))));

        getDeclaredScope().addAfter(getDeclaringStm(), var);

        return new DoubleVariableFmi2Api(var, scope, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName));
    }

    @Override
    public void setDebugLogging(List<String> categories, boolean enableLogging) {
        final String method = "setDebugLogging";

        AArrayInitializer loglevelsArrayInitializer = null;
        String arrayName = name + LOGLEVELS_POSTFIX;
        IMablScope scope = builder.getDynamicScope().getActiveScope();
        if (!categories.isEmpty()) {
            loglevelsArrayInitializer =
                    newAArrayInitializer(categories.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList()));
        }
        ALocalVariableStm arrayContent = MableAstFactory.newALocalVariableStm(
                MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(arrayName),
                        MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), categories.size(), loglevelsArrayInitializer));

        LexIdentifier statusIdentifier = newAIdentifier(getEnvironmentName() + "_" + CATEGORY_STATUS);

        ALocalVariableStm callStm = newALocalVariableStm(newAVariableDeclaration(statusIdentifier, newAIntNumericPrimitiveType(), newAExpInitializer(
                newACallExp(newAIdentifierExp(name), newAIdentifier(method),
                        Arrays.asList(newABoolLiteralExp(enableLogging),
                                MableAstFactory.newAIdentifierExp(arrayName))))));

        scope.add(arrayContent, callStm);

        handleError(scope, method);
    }

    @Override
    public void setupExperiment(FmiBuilder.DoubleVariable<PStm> startTime, FmiBuilder.DoubleVariable<PStm> endTime,
                                FmiBuilder.BoolVariable<PStm> endTimeDefined, Double tolerance) {
        this.setupExperiment(((DoubleVariableFmi2Api) startTime).getReferenceExp().clone(),
                ((DoubleVariableFmi2Api) endTime).getReferenceExp().clone(), ((BooleanVariableFmi2Api) endTimeDefined).getReferenceExp().clone(),
                tolerance);
    }

    @Override
    public void setupExperiment(double startTime, Double endTime, Double tolerance) {
        this.setupExperiment(newARealLiteralExp(startTime), newARealLiteralExp(endTime), newABoolLiteralExp(true), tolerance);
    }

    private void setupExperiment(PExp startTime, PExp endTime, PExp endTimeDefined, Double tolerance) {
        IMablScope scope = builder.getDynamicScope().getActiveScope();
        this.setupExperiment(scope, startTime, endTime, endTimeDefined, tolerance);
    }

    private void setupExperiment(FmiBuilder.Scope<PStm> scope, PExp startTime, PExp endTime, PExp endTimeDefined, Double tolerance) {
        final String method = "setupExperiment";

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), method, new ArrayList<>(
                        Arrays.asList(newABoolLiteralExp(tolerance != null), newARealLiteralExp(tolerance != null ? tolerance : 0d),
                                startTime.clone(), endTimeDefined, endTime != null ? endTime.clone() : newARealLiteralExp(0d)))));
        scope.add(stm);
        handleError(scope, method);
    }

    @Override
    public void enterInitializationMode(boolean toleranceDefined, double tolerance, double startTime, boolean stopTimeDefined, double stopTime) {
        this.enterInitializationMode(builder.getDynamicScope(), toleranceDefined, tolerance, startTime, stopTimeDefined, stopTime);

    }

    @Override
    public void enterInitializationMode(FmiBuilder.Scope<PStm> scope, boolean toleranceDefined, double tolerance, double startTime,
                                        boolean stopTimeDefined, double stopTime) {
        this.enterInitializationMode(builder.getDynamicScope(), VarWrap.wrap(toleranceDefined), VarWrap.wrap(tolerance), VarWrap.wrap(startTime),
                VarWrap.wrap(stopTimeDefined), VarWrap.wrap(stopTime));

    }

    @Override
    public void enterInitializationMode(FmiBuilder.BoolVariable<PStm> toleranceDefined, FmiBuilder.DoubleVariable<PStm> tolerance,
                                        FmiBuilder.DoubleVariable<PStm> startTime, FmiBuilder.BoolVariable<PStm> stopTimeDefined,
                                        FmiBuilder.DoubleVariable<PStm> stopTime) {
        enterInitializationMode(builder.getDynamicScope(), toleranceDefined, tolerance, startTime, stopTimeDefined, stopTime);
    }

    @Override
    public void enterInitializationMode(FmiBuilder.Scope<PStm> scope, FmiBuilder.BoolVariable<PStm> toleranceDefined,
                                        FmiBuilder.DoubleVariable<PStm> tolerance, FmiBuilder.DoubleVariable<PStm> startTime,
                                        FmiBuilder.BoolVariable<PStm> stopTimeDefined,
                                        FmiBuilder.DoubleVariable<PStm> stopTime) {

        final String method = "enterInitializationMode";

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), method, toleranceDefined.getExp().clone(), tolerance.getExp().clone(),
                        startTime.getExp().clone(), stopTimeDefined.getExp().clone(), stopTime.getExp().clone()));

        scope.add(stm);

        handleError(scope, method);
    }

    public void enterEventMode() {
        this.enterEventMode(this.dynamicScope);
    }

    public void enterEventMode(FmiBuilder.Scope<PStm> scope) {
        //enterEventMode
        fmiCall(scope, "enterEventMode");

    }

    public void enterStepMode() {
        this.enterStepMode(this.dynamicScope);
    }

    public void enterStepMode(FmiBuilder.Scope<PStm> scope) {
        //enterEventMode
        fmiCall(scope, "enterStepMode");
    }

    public void getEventIndicators(FmiBuilder.Scope<PStm> scope, FmiBuilder.ArrayVariable<PStm, ? extends FmiBuilder.UIntVariable<PStm>> eventIndicators,
                                   FmiBuilder.UIntVariable<PStm> nEventIndicators) {

        fmiCall(scope, "getEventIndicators", new ARefExp(eventIndicators.getExp().clone()), nEventIndicators.getExp().clone());

    }

    public void getNumberOfEventIndicators(FmiBuilder.Scope<PStm> scope, FmiBuilder.UIntVariable<PStm> nEventIndicators) {
        fmiCall(scope, "getNumberOfEventIndicators", new ARefExp(nEventIndicators.getExp().clone()));
    }

    private void handleError(FmiBuilder.Scope<PStm> scope, String method) {
        handleError(scope, method, MablApiBuilder.Fmi3Status.FMI_ERROR, MablApiBuilder.Fmi3Status.FMI_FATAL);
    }

    private void handleError(FmiBuilder.Scope<PStm> scope, String method, MablApiBuilder.Fmi3Status... statusesToFail) {
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, method, this, (IMablScope) scope, statusesToFail);
        }
    }

    @Override
    public void exitInitializationMode() {
        this.exitInitializationMode(builder.getDynamicScope());
    }

    @Override
    public void setupExperiment(FmiBuilder.Scope<PStm> scope, FmiBuilder.DoubleVariable<PStm> startTime, FmiBuilder.DoubleVariable<PStm> endTime,
                                FmiBuilder.BoolVariable<PStm> endTimeDefined, Double tolerance) {
        this.setupExperiment(scope, ((DoubleVariableFmi2Api) startTime).getReferenceExp().clone(),
                ((DoubleVariableFmi2Api) endTime).getReferenceExp().clone(), endTimeDefined.getExp().clone(), tolerance);
    }

    @Override
    public void setupExperiment(FmiBuilder.Scope<PStm> scope, double startTime, Double endTime, Double tolerance) {
        this.setupExperiment(scope, newARealLiteralExp(startTime), newARealLiteralExp(endTime), newABoolLiteralExp(true), tolerance);
    }


    @Override
    public void exitInitializationMode(FmiBuilder.Scope<PStm> scope) {
        fmiCall(scope,"exitInitializationMode");
    }

    public void getClock(ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs, FmiBuilder.IntVariable<PStm> nvr,
                         ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks) {
        this.getClock(builder.getDynamicScope(), vrs, nvr, triggeredClocks);
    }


    public void getClock(FmiBuilder.Scope<PStm> scope, ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs, FmiBuilder.IntVariable<PStm> nvr,
                         ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks) {
        fmiCall(scope,"getClock", vrs.getReferenceExp().clone(), nvr.getExp().clone(),
                                new ARefExp(triggeredClocks.getExp().clone()));
    }

    public void setClock(ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs, FmiBuilder.IntVariable<PStm> nvr,
                         ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks) {
        this.setClock(builder.getDynamicScope(), vrs, nvr, triggeredClocks);
    }


    public void setClock(FmiBuilder.Scope<PStm> scope, ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs, FmiBuilder.IntVariable<PStm> nvr,
                         ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks) {

        fmiCall(scope,"setClock", vrs.getReferenceExp().clone(), nvr.getExp().clone(),
                                triggeredClocks.getExp().clone());

    }

    public void updateDiscreteStates(FmiBuilder.Scope<PStm> scope, BooleanVariableFmi2Api discreteStatesNeedUpdate,
                                     BooleanVariableFmi2Api terminateSimulation,
                                     BooleanVariableFmi2Api nominalsOfContinuousStatesChanged, BooleanVariableFmi2Api valuesOfContinuousStatesChanged,
                                     BooleanVariableFmi2Api nextEventTimeDefined, DoubleVariableFmi2Api nextEventTime) {

        fmiCall(scope,"updateDiscreteStates",
                                new ARefExp(discreteStatesNeedUpdate.getReferenceExp().clone()),
                                new ARefExp(terminateSimulation.getReferenceExp().clone()),
                                new ARefExp(nominalsOfContinuousStatesChanged.getReferenceExp().clone()),
                                new ARefExp(valuesOfContinuousStatesChanged.getReferenceExp().clone()),
                                new ARefExp(nextEventTimeDefined.getReferenceExp().clone()),
                                new ARefExp(nextEventTime.getReferenceExp().clone()));

    }

//    @Override
//    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.Scope<PStm> scope,
//                                                                                          FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint, FmiBuilder.DoubleVariable<PStm> communicationStepSize,
//                                                                                          FmiBuilder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
//        return step(scope, currentCommunicationPoint, communicationStepSize, ((VariableFmi2Api) noSetFMUStatePriorToCurrentPoint).getReferenceExp());
//    }

//    @Override
//    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.Scope<PStm> scope,
//                                                                                          FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint, FmiBuilder.DoubleVariable<PStm> communicationStepSize) {
//        return step(scope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
//    }
//
//    @Override
//    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint,
//                                                                                          FmiBuilder.DoubleVariable<PStm> communicationStepSize, FmiBuilder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
//        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint);
//    }
//
//    @Override
//    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint,
//                                                                                          FmiBuilder.DoubleVariable<PStm> communicationStepSize) {
//        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
//    }

    public static class StepResult {
        final BooleanVariableFmi2Api eventHandlingNeeded;
        final BooleanVariableFmi2Api terminateSimulation;
        final BooleanVariableFmi2Api earlyReturn;

        public StepResult(BooleanVariableFmi2Api eventHandlingNeeded, BooleanVariableFmi2Api terminateSimulation, BooleanVariableFmi2Api earlyReturn,
                          DoubleVariableFmi2Api lastSuccessfulTime) {
            this.eventHandlingNeeded = eventHandlingNeeded;
            this.terminateSimulation = terminateSimulation;
            this.earlyReturn = earlyReturn;
            this.lastSuccessfulTime = lastSuccessfulTime;
        }

        final DoubleVariableFmi2Api lastSuccessfulTime;

        public BooleanVariableFmi2Api getEventHandlingNeeded() {
            return eventHandlingNeeded;
        }

        public BooleanVariableFmi2Api getTerminateSimulation() {
            return terminateSimulation;
        }

        public BooleanVariableFmi2Api getEarlyReturn() {
            return earlyReturn;
        }

        public DoubleVariableFmi2Api getLastSuccessfulTime() {
            return lastSuccessfulTime;
        }
    }

    StepResult stepResult;

    public Map.Entry<FmiBuilder.BoolVariable<PStm>, StepResult> step(FmiBuilder.Scope<PStm> scope,
                                                                     FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint,
                                                                     FmiBuilder.DoubleVariable<PStm> communicationStepSize,
                                                                     PExp noSetFMUStatePriorToCurrentPoint) {

        if (stepResult == null) {
            stepResult = new StepResult(createReusableBooleanVariable(false, this.getDeclaredScope(), this.name + "EventHandlingNeeded"),
                    createReusableBooleanVariable(false, this.getDeclaredScope(), this.name + "TerminateSimulation"),
                    createReusableBooleanVariable(false, this.getDeclaredScope(), this.name + "EarlyReturn"),
                    createReusableDoubleVariable(0d, this.getDeclaredScope(), this.name + "LastSuccessfulTime"));
        }


        scope.add(newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                newACallExp(this.getReferenceExp().clone(), newAIdentifier("doStep"),
                        Arrays.asList(((VariableFmi2Api) currentCommunicationPoint).getReferenceExp().clone(),
                                ((VariableFmi2Api) communicationStepSize).getReferenceExp().clone(), noSetFMUStatePriorToCurrentPoint.clone(),
                                new ARefExp(stepResult.eventHandlingNeeded.getReferenceExp()).clone(),
                                new ARefExp(stepResult.terminateSimulation.getReferenceExp()).clone(),
                                new ARefExp(stepResult.earlyReturn.getReferenceExp()).clone(),
                                new ARefExp(stepResult.lastSuccessfulTime.getReferenceExp()).clone()))));

        /*
              int doStep(
              real currentCommunicationPoint,
              real communicationStepSize,
              bool nosetFMUStatePriorToCurrentPoint,

              out bool eventHandlingNeeded,
              out bool terminateSimulation,
              out bool earlyReturn,
              out real lastSuccessfulTime
              );

              ok lets make these return variables like IO_buffers and return them every time

        * */


        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "doStep", this, (IMablScope) scope, MablApiBuilder.Fmi3Status.FMI_ERROR,
                    MablApiBuilder.Fmi3Status.FMI_FATAL);
        }


//        scope.add(newIf(newNotEqual(((IMablScope) scope).getFmiStatusVariable().getReferenceExp().clone(),
//                builder.getFmiStatusConstant(MablApiBuilder.FmiStatus.FMI_OK).getReferenceExp().clone()), newABlockStm(
//                newIf(newEqual(((IMablScope) scope).getFmiStatusVariable().getReferenceExp().clone(),
//                        builder.getFmiStatusConstant(MablApiBuilder.FmiStatus.FMI_DISCARD).getReferenceExp().clone()), newABlockStm(
//                        newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
//                                newACallExp(this.getReferenceExp().clone(), newAIdentifier("getRealStatus"),
//                                        Arrays.asList(newAIntLiteralExp(FMI_STATUS_LAST_SUCCESSFUL),
//                                                newARefExp(getCurrentTimeVar().getReferenceExp().clone())))),
//                        newAAssignmentStm(getCurrentTimeFullStepVar().getDesignator().clone(), newABoolLiteralExp(false))), null)), newABlockStm(
//                newAAssignmentStm(this.getCurrentTimeVar().getDesignator().clone(),
//                        newPlusExp(((VariableFmi2Api<?>) currentCommunicationPoint).getReferenceExp().clone(),
//                                ((VariableFmi2Api<?>) communicationStepSize).getReferenceExp().clone())),
//                newAAssignmentStm(getCurrentTimeFullStepVar().getDesignator().clone(), newABoolLiteralExp(true)))));

        return Map.entry(getCurrentTimeFullStepVar(), this.stepResult);
    }

    @Override
    public List<PortFmi3Api> getPorts() {
        return ports;
    }

    @Override
    public List<PortFmi3Api> getPorts(String... names) {
        List<String> accept = Arrays.asList(names);
        return ports.stream().filter(p -> accept.contains(p.getName())).collect(Collectors.toList());
    }

    @Override
    public List<PortFmi3Api> getPorts(int... valueReferences) {
        List<Integer> accept = Arrays.stream(valueReferences).boxed().collect(Collectors.toList());
        return ports.stream().filter(p -> accept.contains(p.getPortReferenceValue().intValue())).collect(Collectors.toList());
    }

    @Override
    public PortFmi3Api getPort(String name) {
        return this.getPorts(name).get(0);
    }

    @Override
    public PortFmi3Api getPort(int valueReference) {
        return this.getPorts(valueReference).get(0);
    }

    //TODO: Move tentative buffer and global share buffer logic to its own module so that it is not coupled with the component logic?
    @SuppressWarnings("unchecked")
//    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> getTentative(IMablScope scope, String... names) {
//        // Get filtered port values
//        FmiBuilder.Port[] filteredPorts = this.ports.stream()
//                .filter(p -> Arrays.asList(names).contains(p.getName()) && (p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output))
//                .toArray(FmiBuilder.Port[]::new);
//        Map<PortFmi3Api, VariableFmi2Api<Object>> portToValueMap = get(scope, filteredPorts);
//        if (portToValueMap.isEmpty()) {
//            return Map.of();
//        }
//
//        // Get tentative buffer index map for the scope
//        tentativeBufferIndexMap.computeIfAbsent(scope, (s) -> new HashMap<>());
//        Map<PortFmi3Api, Integer> portToVariableIndexMap = tentativeBufferIndexMap.get(scope);
//
//        // Return a port value map where the value is an index in a tentative buffer for the given scope instead of the global IO buffer
//        return portToValueMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
//            PType type = entry.getKey().getType();
//            PortFmi3Api port = entry.getKey();
//            VariableFmi2Api variable = entry.getValue();
//            VariableFmi2Api<V> varToReturn;
//
//            // Get the tentative buffer for the given scope
//            tentativeBuffer.computeIfAbsent(scope, (s) -> new HashMap<>());
//            ArrayVariableFmi2Api<Object> buffer =
//                    this.buffers.getBuffer(tentativeBuffer.getOrDefault(scope, new HashMap<>()), type, "TentativeBuffer", 0, scope);
//
//            // Expand the buffer if the port has not been indexed
//            if (!portToVariableIndexMap.containsKey(port)) {
//                portToVariableIndexMap.put(port, portToVariableIndexMap.entrySet().size());
//                ArrayVariableFmi2Api<Object> newBuf = growBuffer(buffer, 1);
//                tentativeBuffer.get(scope).entrySet().removeIf(x -> x.getKey().toString().equals(type.toString()));
//                tentativeBuffer.get(scope).put(type, newBuf);
//                varToReturn = (VariableFmi2Api<V>) newBuf.items().get(newBuf.items().size() - 1);
//            } else {
//                varToReturn = (VariableFmi2Api<V>) buffer.items().get(portToVariableIndexMap.get(port));
//            }
//
//            // Create the assignment from the IO buffer to the tentative buffer in MaBL
//            scope.add(MableAstFactory.newAAssignmentStm(varToReturn.getDesignator(), variable.getExp()));
//            return varToReturn;
//        }));
//    }

    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> get(FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>... ports) {
        return get(builder.getDynamicScope().getActiveScope(), ports);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> get(FmiBuilder.Scope<PStm> scope,
                                                        FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>... ports) {

        List<PortFmi3Api> selectedPorts;
        if (ports == null || ports.length == 0) {
            return Map.of();
        } else {
            selectedPorts = Arrays.stream(ports).map(PortFmi3Api.class::cast).collect(Collectors.toList());
        }

        Map<PortFmi3Api, VariableFmi2Api<V>> results = new HashMap<>();

        Map<Fmi3TypeEnum, List<PortFmi3Api>> typeToSortedPorts = new HashMap<>();
        ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();

        selectedPorts.stream().map(p -> p.scalarVariable.getVariable().getTypeIdentifier()).distinct()
                .map(t -> selectedPorts.stream().filter(p -> p.scalarVariable.getVariable().getTypeIdentifier().equals(t))
                        .sorted(Comparator.comparing(FmiBuilder.Port::getPortReferenceValue)).collect(Collectors.toList()))
                .forEach(l -> typeToSortedPorts.put(l.get(0).scalarVariable.getVariable().getTypeIdentifier(), l));

        for (Map.Entry<Fmi3TypeEnum, List<PortFmi3Api>> e : typeToSortedPorts.entrySet()) {
            for (int i = 0; i < e.getValue().size(); i++) {
                PortFmi3Api p = e.getValue().get(i);
                PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
                scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
            }

//all ports should have the same type so lets use the first port to define the io array type
            PortFmi3Api firstTargetPort = e.getValue().get(0);
            PType type = firstTargetPort.getType();

            ArrayVariableFmi2Api<Object> valBuf = this.buffers.getBuffer(Buffers.BufferTypes.IO, type,
                    firstTargetPort.getSourceObject().getVariable().getTypeIdentifier());

            AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                    call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.GET, firstTargetPort),
                            vrefBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) e.getValue().size()), newARefExp(valBuf.getReferenceExp().clone()),
                            newAUIntLiteralExp((long) e.getValue().size())));
            scope.add(stm);

            handleError(scope, createFunctionName(FmiFunctionType.GET, firstTargetPort));

            if (builder.getSettings().setGetDerivatives && type.equals(new ARealNumericPrimitiveType())) {
                derivativePortsToShare = getDerivatives(e.getValue(), scope);
            }

            for (int i = 0; i < e.getValue().size(); i++) {
                results.put(e.getValue().get(i), (VariableFmi2Api<V>) valBuf.items().get(i));
            }
        }
        return results;
    }

    /**
     * @param ports The ports for which derivatives should be retrieved
     * @param scope The builder scope.
     * @return Derivative ports with a list of derivative values up to max derivative order
     */
    public Map<PortFmi3Api, List<VariableFmi2Api<Object>>> getDerivatives(List<PortFmi3Api> ports, FmiBuilder.Scope<PStm> scope) {
        Map<PortFmi3Api, List<VariableFmi2Api<Object>>> derivativePortsToReturn = new HashMap<>();

        //        // If any target ports exists that can interpolate the port is also linked and derivatives should be retrieved.
        //        // TODO: interpolation for FMI3?
        //        List<PortFmi3Api> portsLinkedToTargetsThatInterpolates = ports.stream().filter(p1 -> p1.getTargetPorts().stream().anyMatch(p2 -> {
        //            try {
        //                return p2.aMablFmi3InstanceAPI.getModelDescription().getCanInterpolateInputs();
        //            } catch (XPathExpressionException e) {
        //                throw new RuntimeException("Exception occurred when accessing modeldescription: ", e);
        //            }
        //        })).collect(Collectors.toList());
        //
        //        try {
        //            int maxOutputDerOrder = modelDescriptionContext.getModelDescription().getMaxOutputDerivativeOrder();
        //            if (portsLinkedToTargetsThatInterpolates.size() > 0 && maxOutputDerOrder > 0) {
        //
        //                // Find derivative ports
        //                List<PortFmi3Api> derivativePorts = new ArrayList<>();
        //                modelDescriptionContext.getModelDescription().getDerivativeToDerivativeSourceMap().entrySet().stream()
        //                        .filter(entry -> portsLinkedToTargetsThatInterpolates.stream()
        //                                .anyMatch(p -> p.getPortReferenceValue().equals(entry.getKey().getVariable().getValueReference()))).forEach(e ->
        //                                // Find the PortFmi3Api representation of the scalarvariable derivative port.
        //                                getPorts().stream().filter(p -> p.getPortReferenceValue().equals(e.getValue().getVariable().getValueReference())).findAny()
        //                                        .ifPresent(derivativePorts::add));
        //
        //                if (derivativePorts.size() > 0) {
        //                    // Array size: number of ports for which to get derivatives multiplied the max derivative order.
        //                    int arraySize = derivativePorts.size() * maxOutputDerOrder;
        //
        //                    ArrayVariableFmi2Api<Object> derValOutBuf = createBuffer(newRealType(), "DVal_OUT", arraySize, getDeclaredScope());
        //                    ArrayVariableFmi2Api<Object> derOrderOutBuf = createBuffer(newIntType(), "DOrder_OUT", arraySize, getDeclaredScope());
        //                    ArrayVariableFmi2Api<Object> derRefOutBuf = createBuffer(newUIntType(), "DRef_OUT", arraySize, getDeclaredScope());
        //
        //                    // Loop arrays and assign derivative value reference and derivative order.
        //                    // E.g for two ports with and a max derivative order of two: derRefOutBuf = [derPortRef1, derPortRef1, derPortRef2,derPortRef2],
        //                    // derOrderOutBuf = [1,2,1,2]
        //                    PortFmi3Api derivativePort = null;
        //                    for (int arrayIndex = 0, portIndex = 0, order = 1; arrayIndex < arraySize; arrayIndex++) {
        //                        // Switch to the next port when we have traversed a length of 'maxOutputDerOrder' in the arrays and reset order.
        //                        if (arrayIndex % maxOutputDerOrder == 0) {
        //                            order = 1;
        //                            derivativePort = derivativePorts.get(portIndex);
        //                            derivativePortsToReturn.put(derivativePort, derValOutBuf.items().subList(arrayIndex, arrayIndex + maxOutputDerOrder));
        //                            portIndex++;
        //                        }
        //
        //                        PStateDesignator dRefDesignator = derRefOutBuf.items().get(arrayIndex).getDesignator().clone();
        //                        scope.add(newAAssignmentStm(dRefDesignator, newAIntLiteralExp(derivativePort.getPortReferenceValue().intValue())));
        //
        //                        PStateDesignator derOrderDesignator = derOrderOutBuf.items().get(arrayIndex).getDesignator().clone();
        //                        scope.add(newAAssignmentStm(derOrderDesignator, newAIntLiteralExp(order)));
        //                        order++;
        //                    }
        //
        //                    // Create assignment statement which assigns derivatives to derValOutBuf by calling getRealOutputDerivatives with
        //                    // derRefOutBuf, arraySize and derOrderOutBuf.
        //                    PStm AStm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
        //                            call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.GETREALOUTPUTDERIVATIVES),
        //                                    derRefOutBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) arraySize),
        //                                    derOrderOutBuf.getReferenceExp().clone(), derValOutBuf.getReferenceExp().clone()));
        //                    scope.add(AStm);
        //
        //                    // If enabled handle potential errors from calling getRealOutputDerivatives
        //                    if (builder.getSettings().fmiErrorHandlingEnabled) {
        //                        FmiStatusErrorHandlingBuilder.generate(builder, createFunctionName(FmiFunctionType.GETREALOUTPUTDERIVATIVES), this,
        //                                (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
        //                    }
        //                }
        //            }
        //            // TODO InvocationTargetException | IllegalAccessException ?
        //        } catch (XPathExpressionException e) {
        //            throw new RuntimeException("Exception occurred when retrieving derivatives: ", e);
        //        }

        return derivativePortsToReturn;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> get() {
        return get(builder.getDynamicScope(), outputPorts.toArray(FmiBuilder.Port[]::new));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> get(int... valueReferences) {
        List<Integer> accept = Arrays.stream(valueReferences).boxed().collect(Collectors.toList());
        return get(builder.getDynamicScope(),
                outputPorts.stream().filter(p -> accept.contains(p.getPortReferenceValue().intValue())).toArray(FmiBuilder.Port[]::new));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> get(String... names) {
        List<String> accept = Arrays.asList(names);
        FmiBuilder.Port[] ports = this.ports.stream().filter(p -> accept.contains(p.getName())).toArray(FmiBuilder.Port[]::new);
        return get(builder.getDynamicScope(), ports);
    }

    /**
     * Stores the final value in rootScope
     * Uses the rootScope for valueReferences
     */
    @Override
    public <V> Map<PortFmi3Api, VariableFmi2Api<V>> getAndShare(String... names) {

        Map<PortFmi3Api, VariableFmi2Api<V>> values = get(names);
        share(values);
        return values;
    }

    @Override
    public <V> Map<? extends FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> getAndShare(
            FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>... ports) {
        Map<PortFmi3Api, VariableFmi2Api<V>> values = get(ports);
        share(values);
        return values;
    }

    @Override
    public <V> Map<? extends FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> getAndShare() {
        Map<PortFmi3Api, VariableFmi2Api<V>> values = get(getPorts().stream().filter(obtainAsShared).toArray(FmiBuilder.Port[]::new));
        share(values);
        return values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public VariableFmi2Api getShared(String name) {
        return this.getPort(name).getSharedAsVariable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public VariableFmi2Api getShared(FmiBuilder.Port port) {
        return ((PortFmi3Api) port).getSharedAsVariable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> VariableFmi2Api<V> getSingle(FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> port) {
        return (VariableFmi2Api) this.get(port).entrySet().iterator().next().getValue();
    }

    private String createFunctionName(FmiFunctionType fun, PortFmi3Api p) {
        return createFunctionName(fun, p.scalarVariable.getVariable().getTypeIdentifier());
    }

    private String createFunctionName(FmiFunctionType f, Fmi3TypeEnum type) {
        String functionName = "";
        switch (f) {
            case GET:
                functionName += "get";
                break;
            case SET:
                functionName += "set";
                break;
            default:
                throw new RuntimeException("Attempting to call non type-dependant function with type: " + type);
        }
        functionName += type.name().substring(0, type.name().length() - "Type".length());

        if (functionName.endsWith("Enumeration")) {
            functionName = functionName.replace("Enumeration", "Int64");
        }
        return functionName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public VariableFmi2Api getSingle(String name) {
        return this.get(name).entrySet().iterator().next().getValue();
    }

    @SuppressWarnings("unchecked")
    public void set(FmiBuilder.Port p, FmiBuilder.ExpressionValue v) {
        this.set(new PortValueExpresssionMapImpl(Map.of(p, v)));
    }

    @SuppressWarnings("unchecked")
    public void set(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> p, FmiBuilder.ExpressionValue v) {
        this.set(scope, new PortValueMapImpl(Map.of(p, v)));
    }

    public void set(PortExpressionValueMap value) {
        this.set(builder.getDynamicScope().getActiveScope(), value);
    }

    public void set(FmiBuilder.Scope<PStm> scope, PortExpressionValueMap<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        List<PortFmi3Api> selectedPorts = value.keySet().stream().map(PortFmi3Api.class::cast).collect(Collectors.toList());

        set(scope, selectedPorts, port -> {
            FmiBuilder.ExpressionValue value_ = value.get(port);
            return Map.entry(value_.getExp(), value_.getType());
        });
    }

    @Override
    public <V> void set(FmiBuilder.Scope<PStm> scope, PortValueMap<V, Fmi3ModelDescription.Fmi3ScalarVariable, PStm> value) {


        if (value == null || value.isEmpty()) {
            return;
        }

        List<PortFmi3Api> selectedPorts = value.keySet().stream().map(PortFmi3Api.class::cast).collect(Collectors.toList());


        set(scope, selectedPorts, port -> {
            Object val = (value.get(port)).get();
            // TODO: Add the rest
            if (val instanceof Double) {
                return Map.entry(newARealLiteralExp((Double) val), newRealType());
            }
            if (val instanceof Long) {
                return Map.entry(newAUIntLiteralExp((Long) val), newUIntType());
            }
            if (val instanceof Integer) {
                return Map.entry(newAIntLiteralExp((Integer) val), newIntType());
            }
            if (val instanceof Boolean) {
                return Map.entry(newABoolLiteralExp((Boolean) val), newBoleanType());
            }
            if (val instanceof String) {
                return Map.entry(newAStringLiteralExp((String) val), newStringType());
            }
            return null;
        });
    }

    @Override
    public <V> void set(FmiBuilder.Scope<PStm> scope, PortVariableMap<V, Fmi3ModelDescription.Fmi3ScalarVariable, PStm> value) {

        List<PortFmi3Api> selectedPorts;
        if (value == null || value.isEmpty()) {
            return;
        } else {
            selectedPorts = value.keySet().stream().map(PortFmi3Api.class::cast).collect(Collectors.toList());
        }

        final PortVariableMap valueFinal = value;
        set(scope, selectedPorts,
                port -> Map.entry(((VariableFmi2Api) valueFinal.get(port)).getReferenceExp().clone(), ((VariableFmi2Api) valueFinal.get(port)).type));
    }

    public void set(FmiBuilder.Scope<PStm> scope, List<PortFmi3Api> selectedPorts, Function<PortFmi3Api, Map.Entry<PExp, PType>> portToValue) {

        Set<String> selectedPortsAsStrings = selectedPorts.stream()
                .map(p -> p.getName() + "-" + p.aMablFmi3InstanceAPI.getName() + "-" + p.aMablFmi3InstanceAPI.getOwner().getName()).collect(toSet());
        selectedPortsAsStrings.removeAll(
                ports.stream().map(p -> p.getName() + "-" + p.aMablFmi3InstanceAPI.getName() + "-" + p.aMablFmi3InstanceAPI.getOwner().getName())
                        .collect(toSet()));
        if (!selectedPortsAsStrings.isEmpty()) {
            throw new RuntimeException("Unable to set port(s) that is not declared in the FMU: " +
                    selectedPortsAsStrings.stream().map(name -> name.split("-")[0]).collect(Collectors.joining(", ")));
        }

        List<PortFmi3Api> sortedPorts =
                selectedPorts.stream().sorted(Comparator.comparing(FmiBuilder.Port::getPortReferenceValue)).collect(Collectors.toList());

        // Group by the string value of the port type as grouping by the port type itself doesnt utilise equals
        sortedPorts.stream().collect(Collectors.groupingBy(i -> i.getSourceObject().getVariable().getTypeIdentifier().toString())).forEach((key, value) -> {
            ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();
            PortFmi3Api firstTargetPort = value.get(0);
            PType type = firstTargetPort.getType();
            for (int i = 0; i < value.size(); i++) {
                FmiBuilder.Port p = value.get(i);
                PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
                scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
            }

            ArrayVariableFmi2Api<Object> valBuf = this.buffers.getBuffer(Buffers.BufferTypes.IO, type,
                    firstTargetPort.getSourceObject().getVariable().getTypeIdentifier());
            for (int i = 0; i < value.size(); i++) {
                PortFmi3Api p = value.get(i);
                PStateDesignator designator = valBuf.items().get(i).getDesignator();

                scope.addAll(BuilderUtil.createTypeConvertingAssignment(builder, scope, designator.clone(), portToValue.apply(p).getKey().clone(),
                        portToValue.apply(p).getValue(), valBuf.type));
            }

            AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                    call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SET, firstTargetPort), vrefBuf.getReferenceExp().clone(),
                            newAIntLiteralExp(value.size()), valBuf.getReferenceExp().clone(), newAIntLiteralExp(value.size())));
            scope.add(stm);

            handleError(scope, createFunctionName(FmiFunctionType.SET, firstTargetPort));

            // TODO: IntermediateUpdateMode instead of CanInterpolateInputs?
            // TODO: commented out for no so it compiles
            //            try {
            //                if (builder.getSettings().setGetDerivatives && modelDescriptionContext.getModelDescription().getCanInterpolateInputs() &&
            //                        type.equals(new ARealNumericPrimitiveType())) {
            //                    setDerivativesForSharedPorts(value, scope);
            //                }
            //            } catch (XPathExpressionException e) {
            //                throw new RuntimeException("Exception occurred when when setting derivatives.", e);
            //            }
        });
    }
    //
    //    /**
    //     * @param ports The ports for which derivative should be set from SHARED derivative ports
    //     * @param scope The builder scope
    //     */
    //    private void setDerivativesForSharedPorts(List<PortFmi3Api> ports, Fmi2Builder.Scope<PStm> scope) {
    //        // Find all ports for which derivatives should be passed together with the derivatives and their order.
    //        LinkedHashMap<PortFmi3Api, Map.Entry<PortFmi3Api, Integer>> mapPortsToDerPortsWithOrder =
    //                ports.stream().filter(port -> port.getSourcePort() != null).map(port -> {
    //                    try {
    //                        Map.Entry<PortFmi3Api, Integer> innerEntry;
    //                        // Find if port is in map of derivatives
    //                        Optional<Map.Entry<Fmi3ModelDescription.Fmi3ScalarVariable, Fmi3ModelDescription.Fmi3ScalarVariable>> derivativePortEntry =
    //                                port.getSourcePort().aMablFmi3InstanceAPI.getModelDescription().getDerivativeToDerivativeSourceMap().entrySet().stream()
    //                                        .filter(e -> e.getKey().getVariable().getValueReferenceAsLong().equals(port.getSourcePort().getPortReferenceValue())).findAny();
    //
    //
    //                        if (derivativePortEntry.isPresent()) {
    //                            // Find PortFmi3Api value of the scalarvariable derivative port and the components max output derivative.
    //                            innerEntry = Map.entry(port.getSourcePort().aMablFmi3InstanceAPI.getPort((int)
    //                                            derivativePortEntry.get().getValue().getVariable().getValueReferenceAsLong()),
    //                                    port.getSourcePort().aMablFmi3InstanceAPI.getModelDescription().getMaxOutputDerivativeOrder());
    //                            return Map.entry(port, innerEntry);
    //                        }
    //
    //                        return null;
    //                    } catch (XPathExpressionException | IllegalAccessException | InvocationTargetException e) {
    //                        throw new RuntimeException("Exception occurred when accessing modeldescription: ", e);
    //                    }
    //                }).filter(Objects::nonNull).collect(LinkedHashMap::new, (map, item) -> map.put(item.getKey(), item.getValue()),  // Accumulator
    //                        Map::putAll);
    //
    //        if (mapPortsToDerPortsWithOrder.size() > 0) {
    //            // Get the total array size as the sum of derivative orders.
    //            int arraySize = mapPortsToDerPortsWithOrder.values().stream().mapToInt(Map.Entry::getValue).sum();
    //
    //            // Create input arrays
    //            ArrayVariableFmi2Api<Object> derValInBuf = createBuffer(newRealType(), "DVal_IN", arraySize, getDeclaredScope());
    //            ArrayVariableFmi2Api<Object> derOrderInBuf = createBuffer(newIntType(), "DOrder_IN", arraySize, getDeclaredScope());
    //            ArrayVariableFmi2Api<Object> derRefInBuf = createBuffer(newUIntType(), "DRef_IN", arraySize, getDeclaredScope());
    //
    //            // Loop through arrays and assign the port reference, derivative order and derivative value.
    //            // E.g: for two derivative ports Out1 (linked to In1) and Out2 (linked to In2) with a max derivative order of 2 and 1: derValInBuf = [der
    //            // (Out1), der(der(Out1)),der(Out2))], derOrderInBuf = [1,2,1], derRefInBuf = [In1, In1, In2]
    //            int arrayIndex = 0;
    //            for (Map.Entry<PortFmi3Api, Map.Entry<PortFmi3Api, Integer>> entry : mapPortsToDerPortsWithOrder.entrySet()) {
    //                PortFmi3Api port = entry.getKey();
    //                int maxOrder = entry.getValue().getValue();
    //                for (int order = 1; order <= maxOrder; order++, arrayIndex++) {
    //                    // Assign to array variables
    //                    PStateDesignator derRefDesignator = derRefInBuf.items().get(arrayIndex).getDesignator().clone();
    //                    scope.add(newAAssignmentStm(derRefDesignator, newAIntLiteralExp(port.getPortReferenceValue().intValue())));
    //
    //                    PStateDesignator derOrderDesignator = derOrderInBuf.items().get(arrayIndex).getDesignator().clone();
    //                    scope.add(newAAssignmentStm(derOrderDesignator, newAIntLiteralExp(order)));
    //
    //                    PStateDesignator derValInDesignator = derValInBuf.items().get(arrayIndex).getDesignator().clone();
    //                    scope.add(newAAssignmentStm(derValInDesignator,
    //                            ((VariableFmi2Api) ((ArrayVariableFmi2Api) entry.getValue().getKey().getSharedAsVariable()).items()
    //                                    .get(order - 1)).getReferenceExp().clone()));
    //                }
    //            }
    //
    //            setDerivatives(derValInBuf, derOrderInBuf, derRefInBuf, scope);
    //        }
    //    }

    /**
     * If two derivative ports 'Out1' (linked to 'In1') and 'Out2' (linked to 'In2') with a max derivative order of 2 and 1 then derValInBuf =
     * [der(Out1), der(der(Out1)), der(Out2))], derOrderInBuf = [1,2,1], derRefInBuf = [In1, In1, In2]
     *
     * @param derValInBuf   derivative values
     * @param derOrderInBuf derivative value orders
     * @param derRefInBuf   ports for which to set the derivative
     * @param scope         the builder scope
     */
    public void setDerivatives(ArrayVariableFmi2Api derValInBuf, ArrayVariableFmi2Api derOrderInBuf, ArrayVariableFmi2Api derRefInBuf,
                               FmiBuilder.Scope<PStm> scope) {
        final String method = "setRealInputDerivatives";
        int arraySize = derValInBuf.size();

        // Create set derivatives statement which calls setRealOutputDerivatives with derRefInBuf, arraySize, derOrderInBuf and
        // derValInBuf.
        PStm ifStm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), method, derRefInBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) arraySize),
                        derOrderInBuf.getReferenceExp().clone(), derValInBuf.getReferenceExp().clone()));
        scope.add(ifStm);

        handleError(scope, method);
    }

    @Override
    public <V> void set(PortValueMap<V, Fmi3ModelDescription.Fmi3ScalarVariable, PStm> value) {
        set(builder.getDynamicScope(), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void set(FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> port, FmiBuilder.Variable<PStm, V> value) {
        this.set(new PortVariableMapImpl(Map.of(port, value)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void set(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> port,
                        FmiBuilder.Variable<PStm, V> value) {
        this.set(scope, new PortVariableMapImpl(Map.of(port, value)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void set(FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm> port, FmiBuilder.Value<V> value) {
        PortValueMap map = new PortValueMapImpl();
        map.put(port, value);
        set(map);
    }

    @Override
    public <V> void set(PortVariableMap<V, Fmi3ModelDescription.Fmi3ScalarVariable, PStm> value) {
        set(builder.getDynamicScope(), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setLinked(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>... filterPorts) {

        List<PortFmi3Api> selectedPorts = ports.stream().filter(isLinked).collect(Collectors.toList());
        if (filterPorts != null && filterPorts.length != 0) {

            List<FmiBuilder.Port> filterList = Arrays.asList(filterPorts);

            for (FmiBuilder.Port p : filterList) {
                if (!isLinked.test(p)) {
                    logger.warn("Filter for setLinked contains unlined port. Its ignored. {}", p);
                }
            }

            selectedPorts = selectedPorts.stream().filter(filterList::contains).collect(Collectors.toList());
        }
        if (selectedPorts.isEmpty()) {
            logger.warn("No linked input variables for FMU instance: " + this.getName());
            return;
        }


        for (PortFmi3Api port : selectedPorts) {
            if (port.getSourcePort() == null) {
                throw new RuntimeException(
                        "Attempting to obtain shared value from a port that is not linked. This port is missing a required " + "link: " + port);
            }

            if (port.getSourcePort().getSharedAsVariable() == null) {
                throw new RuntimeException(
                        "Attempting to obtain shared values from a port that is linked but has no value shared. Share a value " + "first. " + port);

            }
        }

        set(scope, selectedPorts,
                k -> Map.entry(k.getSourcePort().getSharedAsVariable().getReferenceExp().clone(), k.getSourcePort().getSharedAsVariable().type));

    }

    @Override
    public void setLinked() {
        this.setLinked(dynamicScope, (FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>[]) null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setLinked(FmiBuilder.Port... filterPorts) {

        this.setLinked(dynamicScope, filterPorts);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setLinked(String... filterNames) {
        List<String> accept = Arrays.asList(filterNames);
        this.setLinked(dynamicScope, getPorts().stream().filter(p -> accept.contains(p.getName())).toArray(FmiBuilder.Port[]::new));

    }

    @SuppressWarnings("unchecked")
    @Override
    public void setLinked(long... filterValueReferences) {
        List<Long> accept = Arrays.stream(filterValueReferences).boxed().collect(Collectors.toList());
        this.setLinked(dynamicScope, getPorts().stream().filter(p -> accept.contains(p.getPortReferenceValue())).toArray(FmiBuilder.Port[]::new));

    }

    @Override
    public void setInt(Map<? extends Integer, ? extends FmiBuilder.Value<Integer>> values) {

    }

    @Override
    public void setString(Map<? extends String, ? extends FmiBuilder.Value<String>> value) {

    }

    @Override
    public void terminate() {
        this.terminate(builder.getDynamicScope());
    }

    @Override
    public void terminate(FmiBuilder.Scope<PStm> scope) {
        fmiCall(scope, "terminate");
    }

    public void reset() {
        this.reset(builder.getDynamicScope());
    }

    public void reset(FmiBuilder.Scope<PStm> scope) {
        fmiCall(scope, "reset");
    }

    private void fmiCall(FmiBuilder.Scope<PStm> scope, String methodName, PExp... args) {
        AAssigmentStm stm =
                newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(), call(this.getReferenceExp().clone(), methodName, args));

        scope.add(stm);

        handleError(scope, methodName);
    }

    // TODO: these are work in progress
    public void freeInstance() {
        this.freeInstance(builder.getDynamicScope());
    }

    public void freeInstance(FmiBuilder.Scope<PStm> scope) {
        scope.add(MableAstFactory.newExpressionStm(call(getReferenceExp().clone(), "freeInstance")));
    }


    // TODO: implement this from the other share function below. This for now to build.
    //    @Override
    //    public <V> void share(
    //            Map<? extends FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> values) {
    //        ;
    //    }

    @Override
    public <V> void share(
            Map<? extends FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> values) {
        // Group by the string value of the port type as grouping by the port type itself doesnt utilise equals
        values.entrySet().stream().collect(Collectors.groupingBy(map -> ((PortFmi3Api) map.getKey()).getType().toString())).entrySet().stream()
                .forEach(map -> {
                    PType type = ((PortFmi3Api) map.getValue().get(0).getKey()).getType();


                    Map<FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>, FmiBuilder.Variable> data =
                            map.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    data.keySet().stream().map(PortFmi3Api.class::cast).sorted(Comparator.comparing(PortFmi3Api::getPortReferenceValue))
                            .forEach(port -> {
                                //this is the sorted set of assignments, these can be replaced by a memcopy later
                                Fmi3TypeEnum fmiType = port.getSourceObject().getVariable().getTypeIdentifier();


                                ArrayVariableFmi2Api<Object> buffer = buffers.getBuffer(Buffers.BufferTypes.Share, type, fmiType);
                                if (port.getSharedAsVariable() == null) {
                                    ArrayVariableFmi2Api<Object> newBuf = this.buffers.growBuffer(Buffers.BufferTypes.Share, buffer, 1, fmiType);

                                    VariableFmi2Api<Object> newShared = newBuf.items().get(newBuf.items().size() - 1);
                                    port.setSharedAsVariable(newShared);
                                }

                                PStateDesignator designator = port.getSharedAsVariable().getDesignator();
                                builder.getDynamicScope().addAll(BuilderUtil.createTypeConvertingAssignment(builder, dynamicScope, designator.clone(),
                                        ((VariableFmi2Api) data.get(port)).getReferenceExp().clone(), port.getType(),
                                        ((VariableFmi2Api) ((VariableFmi2Api<?>) data.get(port))).type));


                                if (type.equals(new ARealNumericPrimitiveType()) && derivativePortsToShare != null) {
                                    // Find match a derivativePortsToShare where both port reference and derivative port reference matches.
                                    derivativePortsToShare.keySet().stream().filter(derivativePort -> {
                                        return modelDescriptionContext.getModelDescription().getDerivatives().stream()
                                                .map(v -> v.getVariable().getValueReferenceAsLong()).anyMatch(
                                                        vref -> vref.equals(port.getPortReferenceValue()) &&
                                                                vref.equals(derivativePort.getPortReferenceValue()));
                                    }).findFirst().ifPresent((derivativePort) -> {
                                        // If the derivative port is not yet shared then get shared derivative buffer, grow it by one and set the port as shared
                                        // with the new array.
                                        if (derivativePort.getSharedAsVariable() == null) {
                                            ArrayVariableFmi2Api<Object> sharedDerBuf = growSharedDerBuf(1);

                                            ArrayVariableFmi2Api newSharedArray =
                                                    (ArrayVariableFmi2Api) sharedDerBuf.items().get(sharedDerBuf.items().size() - 1);
                                            derivativePort.setSharedAsVariable(newSharedArray);
                                        }

                                        // DerivativePorts.get(derivativePort).size should equal derivativePort.getSharedAsVariable().items().size() as they are
                                        // both determined by the max derivative order.
                                        List<VariableFmi2Api> derivatives = ((ArrayVariableFmi2Api) derivativePort.getSharedAsVariable()).items();
                                        for (int i = 0; i < derivatives.size(); i++) {
                                            PExp val = derivativePortsToShare.get(derivativePort).get(i).getReferenceExp().clone();
                                            builder.getDynamicScope().add(newAAssignmentStm(derivatives.get(i).getDesignator().clone(), val));
                                        }
                                    });
                                }
                            });
                });
    }

    /**
     * @param increaseByCount the length of which the outer array should grow
     * @return a two dimensional non-jagged array for derivatives.
     * e.g.: <type>[originalSize+increaseByCount][maxDerivativeOrder]
     */
    @SuppressWarnings("unchecked")
    private ArrayVariableFmi2Api<Object> growSharedDerBuf(int increaseByCount) {

        // Get shared buffer creates the buffer with one element.
        if (this.derSharedBuffer == null && increaseByCount == 1) {
            return getSharedDerBuffer();
        }

        ArrayVariableFmi2Api<Object> sharedBuffer = getSharedDerBuffer();

        int innerArraySize;
        try {
            innerArraySize = modelDescriptionContext.getModelDescription().getMaxOutputDerivativeOrder();
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Exception occurred when accessing model description: ", e);
        }

        String bufferName = ((AIdentifierExp) sharedBuffer.getReferenceExp()).getName().getText();
        int outerArraySize = sharedBuffer.size() + increaseByCount;

        PStm outerArrayVariableStm = newALocalVariableStm(
                newAVariableDeclarationMultiDimensionalArray(newAIdentifier(bufferName), newARealNumericPrimitiveType(),
                        Arrays.asList(outerArraySize, innerArraySize)));

        sharedBuffer.getDeclaringStm().parent().replaceChild(sharedBuffer.getDeclaringStm(), outerArrayVariableStm);

        List<VariableFmi2Api<Object>> items = new ArrayList<>();

        // Add new element(s) to array.
        // E.g: buff[1][1] with increaseByCount of 2 will become: buff[3][1] where 1 equals the max derivative order.
        for (int i = 0; i < increaseByCount; i++) {
            int outerIndex = sharedBuffer.size() + i;

            List<VariableFmi2Api<Object>> variables = new ArrayList<>();
            // Create variables
            for (int l = 0; l < innerArraySize; l++) {
                AArrayStateDesignator designator = newAArayStateDesignator(
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(bufferName)), newAIntLiteralExp(outerIndex)),
                        newAIntLiteralExp(l));

                AArrayIndexExp indexExp =
                        newAArrayIndexExp(newAIdentifierExp(bufferName), Arrays.asList(newAIntLiteralExp(outerIndex), newAIntLiteralExp(l)));

                variables.add(
                        new VariableFmi2Api<>(outerArrayVariableStm, type, this.getDeclaredScope(), builder.getDynamicScope(), designator, indexExp));
            }
            // Create array with variables
            ArrayVariableFmi2Api innerArrayVariable =
                    new ArrayVariableFmi2Api<>(outerArrayVariableStm, newARealNumericPrimitiveType(), getDeclaredScope(), builder.getDynamicScope(),
                            newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(bufferName)), newAIntLiteralExp(outerIndex)),
                            newAArrayIndexExp(newAIdentifierExp(bufferName), Collections.singletonList(newAIntLiteralExp(outerIndex))), variables);

            items.add(innerArrayVariable);
        }
        // Add new array(s) to existing arrays
        items.addAll(0, sharedBuffer.items());

        return new ArrayVariableFmi2Api<>(outerArrayVariableStm, newAArrayType(newARealNumericPrimitiveType()), getDeclaredScope(),
                builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(bufferName)), newAIdentifierExp(bufferName), items);

    }


    @Override
    public FmiBuilder.StateVariable<PStm> getState() throws XPathExpressionException {
        return getState(builder.getDynamicScope());
    }

    @Override
    public FmiBuilder.StateVariable<PStm> getState(FmiBuilder.Scope<PStm> scope) throws XPathExpressionException {
        if (!this.modelDescriptionContext.getModelDescription().getCanGetAndSetFmustate()) {
            throw new RuntimeException("Unable to get state on fmu: " + this.getOwner() + " with instance name: " + this.getName());
        }

        String stateName = builder.getNameGenerator().getName(name, "state");
        PStm stateVar = newVariable(stateName, newANameType("FmiComponentState"));
        scope.add(stateVar);

        StateMablVariableFmi3Api state =
                new StateMablVariableFmi3Api(stateVar, newANameType("FmiComponentState"), (IMablScope) scope, builder.getDynamicScope(),
                        newAIdentifierStateDesignator(stateName), newAIdentifierExp(stateName), builder, this);

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), "getState", Collections.singletonList(newARefExp(state.getReferenceExp().clone()))));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "getState", this, (IMablScope) scope, MablApiBuilder.Fmi3Status.FMI_ERROR,
                    MablApiBuilder.Fmi3Status.FMI_FATAL);
        }

        return state;
    }

    public FmuVariableFmi3Api getOwner() {
        return this.owner;
    }

    public List<PortFmi3Api> getAllConnectedOutputs() {
        return this.ports.stream().filter(x -> x.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output && x.getTargetPorts().size() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getEnvironmentName() {
        return environmentName;
    }


    public enum FmiFunctionType {
        GET,
        SET
    }

    /**
     * Error and Fatal should lead to freeInstance calls followed by subsequent termination.
     */
    static class FmiStatusErrorHandlingBuilder {
        static void generate(MablApiBuilder builder, String method, InstanceVariableFmi3Api instance, IMablScope scope,
                             MablApiBuilder.Fmi3Status... statusesToFail) {
            if (statusesToFail == null || statusesToFail.length == 0) {
                return;
            }

            Function<MablApiBuilder.Fmi3Status, PExp> checkStatusEq =
                    s -> newEqual(scope.getFmiStatusVariable().getReferenceExp().clone(), builder.getFmiStatusConstant(s).getReferenceExp().clone());

            PExp exp = checkStatusEq.apply(statusesToFail[0]);

            for (int i = 1; i < statusesToFail.length; i++) {
                exp = newOr(exp, checkStatusEq.apply(statusesToFail[i]));
            }

            ScopeFmi2Api thenScope = scope.enterIf(new PredicateFmi2Api(exp)).enterThen();

            // thenScope.add(newAAssignmentStm(builder.getGlobalExecutionContinue().getDesignator().clone(), newABoolLiteralExp(false)));

            for (MablApiBuilder.Fmi3Status status : statusesToFail) {
                ScopeFmi2Api s = thenScope.enterIf(new PredicateFmi2Api(checkStatusEq.apply(status))).enterThen();
                builder.getLogger()
                        .error(s, method.substring(0, 1).toUpperCase() + method.substring(1) + " failed on '%s' with status: " + status, instance);
            }

            thenScope.add(new AErrorStm(newAStringLiteralExp("Failed to '" + method + "' on '" + instance.getName() + "'")));
            thenScope.leave();
        }

        static Set<String> collectedPreviousLoadedModules(INode node/*, Set<String> externalLoadedModuleIdentifiers*/) {

            if (node == null) {
                return new HashSet<>();
            }

            Function<PStm, String> getLoadedIdentifier = s -> {
                AtomicReference<String> id = new AtomicReference<>();
                try {
                    s.apply(new DepthFirstAnalysisAdaptor() {
                        @Override
                        public void caseALoadExp(ALoadExp node) {
                            AVariableDeclaration decl = node.getAncestor(AVariableDeclaration.class);
                            if (decl != null) {
                                id.set(decl.getName().getText());
                            }
                            ALocalVariableStm ldecl = node.getAncestor(ALocalVariableStm.class);
                            if (decl != null) {
                                id.set(ldecl.getDeclaration().getName().getText());
                            }
                        }
                    });
                } catch (AnalysisException e) {
                    e.addSuppressed(e);
                }
                return id.get();
            };

            Set<String> identifiers = new HashSet<>();
            if (node instanceof SBlockStm) {

                for (PStm n : ((SBlockStm) node).getBody()) {
                    String id = getLoadedIdentifier.apply(n);
                    if (id != null) {
                        identifiers.add(id);
                    }
                }
            }

            if (node.parent() != null) {
                identifiers.addAll(collectedPreviousLoadedModules(node.parent()/*, externalLoadedModuleIdentifiers*/));
            }
            /*
            if (identifiers != null) {
                identifiers.removeAll(externalLoadedModuleIdentifiers);
            }*/

            return identifiers;

        }
    }


}
