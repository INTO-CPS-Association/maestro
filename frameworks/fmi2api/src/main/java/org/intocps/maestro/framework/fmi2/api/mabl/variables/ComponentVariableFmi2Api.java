package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
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
import java.lang.reflect.InvocationTargetException;
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
public class ComponentVariableFmi2Api extends VariableFmi2Api<FmiBuilder.NamedVariable<PStm>> implements FmiBuilder.Fmi2ComponentVariable<PStm, Fmi2ModelDescription.ScalarVariable> {
    final static Logger logger = LoggerFactory.getLogger(ComponentVariableFmi2Api.class);
    private final static int FMI_OK = 0;
    private final static int FMI_WARNING = 1;
    private final static int FMI_DISCARD = 2;
    private final static int FMI_ERROR = 3;
    private final static int FMI_FATAL = 4;
    private final static int FMI_PENDING = 5;
    private final static int FMI_STATUS_LAST_SUCCESSFUL = 2;
    private static final String LOGLEVELS_POSTFIX = "_log_levels";
    private final static String CATEGORY_STATUS = "category_status";
    final List<PortFmi2Api> outputPorts;
    final List<PortFmi2Api> inputPorts;
    final List<PortFmi2Api> ports;
    private final FmuVariableFmi2Api owner;
    private final String name;
    private final MablApiBuilder builder;
    private final Map<PType, ArrayVariableFmi2Api<Object>> ioBuffer = new HashMap<>();
    private final Map<PType, ArrayVariableFmi2Api<Object>> sharedBuffer = new HashMap<>();
    private final Map<IMablScope, Map<PType, ArrayVariableFmi2Api<Object>>> tentativeBuffer = new HashMap<>();
    private final Map<IMablScope, Map<PortFmi2Api, Integer>> tentativeBufferIndexMap = new HashMap<>();
    private final String environmentName;
    Predicate<FmiBuilder.Port> isLinked = p -> ((PortFmi2Api) p).getSourcePort() != null;
    ModelDescriptionContext modelDescriptionContext;
    private ArrayVariableFmi2Api<Object> derSharedBuffer;
    private DoubleVariableFmi2Api currentTimeVar = null;
    private BooleanVariableFmi2Api currentTimeStepFullStepVar = null;
    private ArrayVariableFmi2Api<Object> valueRefBuffer;
    private Map<PortFmi2Api, List<VariableFmi2Api<Object>>> derivativePortsToShare;
    private List<String> variabesToLog;

    public ComponentVariableFmi2Api(PStm declaration, FmuVariableFmi2Api parent, String name, ModelDescriptionContext modelDescriptionContext,
            MablApiBuilder builder, IMablScope declaringScope, PStateDesignator designator, PExp referenceExp) {
        this(declaration, parent, name, modelDescriptionContext, builder, declaringScope, designator, referenceExp, name);
    }

    public ComponentVariableFmi2Api(PStm declaration, FmuVariableFmi2Api parent, String name, ModelDescriptionContext modelDescriptionContext,
            MablApiBuilder builder, IMablScope declaringScope, PStateDesignator designator, PExp referenceExp, String environmentName) {
        super(declaration, newANameType("FMI2Component"), declaringScope, builder.getDynamicScope(), designator, referenceExp);
        this.owner = parent;
        this.name = name;

        this.modelDescriptionContext = modelDescriptionContext;
        this.builder = builder;

        ports = modelDescriptionContext.nameToSv.values().stream().map(sv -> new PortFmi2Api(this, sv))
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        outputPorts = ports.stream().filter(p -> p.scalarVariable.causality == Fmi2ModelDescription.Causality.Output)
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        inputPorts = ports.stream().filter(p -> p.scalarVariable.causality == Fmi2ModelDescription.Causality.Input)
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        this.environmentName = environmentName;
    }

    public Fmi2ModelDescription getModelDescription() {
        return modelDescriptionContext.getModelDescription();
    }

    public List<PortFmi2Api> getVariablesToLog() {
        return this.getPorts(this.variabesToLog.toArray(new String[0]));
    }

    public void setVariablesToLog(List<? extends RelationVariable> variablesToLog) {
        this.variabesToLog = variablesToLog.stream().map(RelationVariable::getName).collect(Collectors.toList());
    }

    @Override
    public <V> void share(FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm> port, FmiBuilder.Variable<PStm, V> value) {
        Map<FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>, FmiBuilder.Variable<PStm, V>> map = new HashMap<>();
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

    private ArrayVariableFmi2Api<Object> getBuffer(Map<PType, ArrayVariableFmi2Api<Object>> buffer, PType type, String prefix, int size,
            IMablScope scope) {
        Optional<PType> first = buffer.keySet().stream().filter(x -> x.toString().equals(type.toString())).findFirst();
        if (first.isEmpty()) {
            ArrayVariableFmi2Api<Object> value = createBuffer(type, prefix, size, scope);
            buffer.put(type, value);
            return value;

        } else {
            return buffer.get(first.get());
        }
    }

    private ArrayVariableFmi2Api<Object> getIOBuffer(PType type) {
        return getBuffer(this.ioBuffer, type, "IO", modelDescriptionContext.valRefToSv.size(), getDeclaredScope());
    }

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


    private ArrayVariableFmi2Api<Object> getSharedBuffer(PType type) {
        return this.getBuffer(this.sharedBuffer, type, "Share", 0, getDeclaredScope());
    }

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

    @Override
    public void setDebugLogging(List<String> categories, boolean enableLogging) {
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
                newACallExp(newAIdentifierExp(name), newAIdentifier("setDebugLogging"),
                        Arrays.asList(newABoolLiteralExp(enableLogging), MableAstFactory.newAIntLiteralExp(categories.size()),
                                MableAstFactory.newAIdentifierExp(arrayName))))));

        scope.add(arrayContent, callStm);

        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "setDebugLogging", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }
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
        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SETUPEXPERIMENT), new ArrayList<>(
                        Arrays.asList(newABoolLiteralExp(tolerance != null), newARealLiteralExp(tolerance != null ? tolerance : 0d),
                                startTime.clone(), endTimeDefined, endTime != null ? endTime.clone() : newARealLiteralExp(0d)))));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "setupExperiment", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public void enterInitializationMode() {
        this.enterInitializationMode(builder.getDynamicScope());

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
    public void enterInitializationMode(FmiBuilder.Scope<PStm> scope) {
        PStm stm = stateTransitionFunction(FmiFunctionType.ENTERINITIALIZATIONMODE);
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "enterInitializationMode", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public void exitInitializationMode(FmiBuilder.Scope<PStm> scope) {
        PStm stm = stateTransitionFunction(FmiFunctionType.EXITINITIALIZATIONMODE);
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "exitInitializationMode", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.Scope<PStm> scope,
            FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint, FmiBuilder.DoubleVariable<PStm> communicationStepSize,
            FmiBuilder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
        return step(scope, currentCommunicationPoint, communicationStepSize, ((VariableFmi2Api) noSetFMUStatePriorToCurrentPoint).getReferenceExp());
    }

    @Override
    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.Scope<PStm> scope,
            FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint, FmiBuilder.DoubleVariable<PStm> communicationStepSize) {
        return step(scope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
    }

    @Override
    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint,
            FmiBuilder.DoubleVariable<PStm> communicationStepSize, FmiBuilder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint);
    }

    @Override
    public Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint,
            FmiBuilder.DoubleVariable<PStm> communicationStepSize) {
        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
    }

    private Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> step(FmiBuilder.Scope<PStm> scope,
            FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint, FmiBuilder.DoubleVariable<PStm> communicationStepSize,
            PExp noSetFMUStatePriorToCurrentPoint) {

        scope.add(newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                newACallExp(this.getReferenceExp().clone(), newAIdentifier("doStep"),
                        Arrays.asList(((VariableFmi2Api) currentCommunicationPoint).getReferenceExp().clone(),
                                ((VariableFmi2Api) communicationStepSize).getReferenceExp().clone(), noSetFMUStatePriorToCurrentPoint.clone()))));

        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "doStep", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }


        scope.add(newIf(newNotEqual(((IMablScope) scope).getFmiStatusVariable().getReferenceExp().clone(),
                builder.getFmiStatusConstant(MablApiBuilder.FmiStatus.FMI_OK).getReferenceExp().clone()), newABlockStm(
                newIf(newEqual(((IMablScope) scope).getFmiStatusVariable().getReferenceExp().clone(),
                        builder.getFmiStatusConstant(MablApiBuilder.FmiStatus.FMI_DISCARD).getReferenceExp().clone()), newABlockStm(
                        newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                                newACallExp(this.getReferenceExp().clone(), newAIdentifier("getRealStatus"),
                                        Arrays.asList(newAIntLiteralExp(FMI_STATUS_LAST_SUCCESSFUL),
                                                newARefExp(getCurrentTimeVar().getReferenceExp().clone())))),
                        newAAssignmentStm(getCurrentTimeFullStepVar().getDesignator().clone(), newABoolLiteralExp(false))), null)), newABlockStm(
                newAAssignmentStm(this.getCurrentTimeVar().getDesignator().clone(),
                        newPlusExp(((VariableFmi2Api<?>) currentCommunicationPoint).getReferenceExp().clone(),
                                ((VariableFmi2Api<?>) communicationStepSize).getReferenceExp().clone())),
                newAAssignmentStm(getCurrentTimeFullStepVar().getDesignator().clone(), newABoolLiteralExp(true)))));

        return Map.entry(getCurrentTimeFullStepVar(), getCurrentTimeVar());
    }

    private PStm stateTransitionFunction(FmiFunctionType type) {
        switch (type) {
            case ENTERINITIALIZATIONMODE:
                break;
            case EXITINITIALIZATIONMODE:
                break;
            case TERMINATE:
                break;
            default:
                throw new RuntimeException("Attempting to call state transition function with non-state transition function type: " + type);
        }

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) this.dynamicScope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(type)));
        return stm;
    }

    @Override
    public List<PortFmi2Api> getPorts() {
        return ports;
    }

    @Override
    public List<PortFmi2Api> getPorts(String... names) {
        List<String> accept = Arrays.asList(names);
        return ports.stream().filter(p -> accept.contains(p.getName())).collect(Collectors.toList());
    }

    @Override
    public List<PortFmi2Api> getPorts(int... valueReferences) {
        List<Integer> accept = Arrays.stream(valueReferences).boxed().collect(Collectors.toList());
        return ports.stream().filter(p -> accept.contains(p.getPortReferenceValue().intValue())).collect(Collectors.toList());
    }

    @Override
    public PortFmi2Api getPort(String name) {
        return (PortFmi2Api) this.getPorts(name).get(0);
    }

    @Override
    public PortFmi2Api getPort(int valueReference) {
        return (PortFmi2Api) this.getPorts(valueReference).get(0);
    }

    //TODO: Move tentative buffer and global share buffer logic to its own module so that it is not coupled with the component logic?
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> getTentative(IMablScope scope, String... names) {
        // Get filtered port values
        FmiBuilder.Port[] filteredPorts = this.ports.stream()
                .filter(p -> Arrays.asList(names).contains(p.getName()) && (p.scalarVariable.causality == Fmi2ModelDescription.Causality.Output))
                .toArray(FmiBuilder.Port[]::new);
        Map<PortFmi2Api, VariableFmi2Api<Object>> portToValueMap = get(scope, filteredPorts);
        if (portToValueMap.isEmpty()) {
            return Map.of();
        }

        // Get tentative buffer index map for the scope
        tentativeBufferIndexMap.computeIfAbsent(scope, (s) -> new HashMap<>());
        Map<PortFmi2Api, Integer> portToVariableIndexMap = tentativeBufferIndexMap.get(scope);

        // Return a port value map where the value is an index in a tentative buffer for the given scope instead of the global IO buffer
        return portToValueMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            PType type = entry.getKey().getType();
            PortFmi2Api port = entry.getKey();
            VariableFmi2Api variable = entry.getValue();
            VariableFmi2Api<V> varToReturn;

            // Get the tentative buffer for the given scope
            tentativeBuffer.computeIfAbsent(scope, (s) -> new HashMap<>());
            ArrayVariableFmi2Api<Object> buffer =
                    this.getBuffer(tentativeBuffer.getOrDefault(scope, new HashMap<>()), type, "TentativeBuffer", 0, scope);

            // Expand the buffer if the port has not been indexed
            if (!portToVariableIndexMap.containsKey(port)) {
                portToVariableIndexMap.put(port, portToVariableIndexMap.entrySet().size());
                ArrayVariableFmi2Api<Object> newBuf = growBuffer(buffer, 1);
                tentativeBuffer.get(scope).entrySet().removeIf(x -> x.getKey().toString().equals(type.toString()));
                tentativeBuffer.get(scope).put(type, newBuf);
                varToReturn = (VariableFmi2Api<V>) newBuf.items().get(newBuf.items().size() - 1);
            } else {
                varToReturn = (VariableFmi2Api<V>) buffer.items().get(portToVariableIndexMap.get(port));
            }

            // Create the assignment from the IO buffer to the tentative buffer in MaBL
            scope.add(MableAstFactory.newAAssignmentStm(varToReturn.getDesignator(), variable.getExp()));
            return varToReturn;
        }));
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>... ports) {
        return get(builder.getDynamicScope().getActiveScope(), ports);
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(FmiBuilder.Scope<PStm> scope,
            FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>... ports) {

        List<PortFmi2Api> selectedPorts;
        if (ports == null || ports.length == 0) {
            return Map.of();
        } else {
            selectedPorts = Arrays.stream(ports).map(PortFmi2Api.class::cast).collect(Collectors.toList());
        }

        Map<PortFmi2Api, VariableFmi2Api<V>> results = new HashMap<>();

        Map<Fmi2ModelDescription.Type, List<PortFmi2Api>> typeToSortedPorts = new HashMap<>();
        ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();

        selectedPorts.stream().map(p -> p.scalarVariable.getType().type).distinct()
                .map(t -> selectedPorts.stream().filter(p -> p.scalarVariable.getType().type.equals(t))
                        .sorted(Comparator.comparing(FmiBuilder.Port::getPortReferenceValue)).collect(Collectors.toList()))
                .forEach(l -> typeToSortedPorts.put(l.get(0).scalarVariable.getType(), l));

        for (Map.Entry<Fmi2ModelDescription.Type, List<PortFmi2Api>> e : typeToSortedPorts.entrySet()) {
            for (int i = 0; i < e.getValue().size(); i++) {
                PortFmi2Api p = e.getValue().get(i);
                PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
                scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
            }


            PType type;
            switch (e.getKey().type) {
                case Boolean:
                    type = new ABooleanPrimitiveType();
                    break;
                case Real:
                    type = new ARealNumericPrimitiveType();
                    break;
                case Integer:
                    type = new AIntNumericPrimitiveType();
                    break;
                case String:
                    type = new AStringPrimitiveType();
                    break;
                case Enumeration:
                    throw new RuntimeException("Cannot assign enumeration port type.");

                default:
                    throw new RuntimeException("Cannot match port types.");
            }

            ArrayVariableFmi2Api<Object> valBuf = getIOBuffer(type);

            AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                    call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.GET, e.getValue().get(0)),
                            vrefBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) e.getValue().size()), valBuf.getReferenceExp().clone()));
            scope.add(stm);

            if (builder.getSettings().fmiErrorHandlingEnabled) {
                FmiStatusErrorHandlingBuilder.generate(builder, createFunctionName(FmiFunctionType.GET, e.getValue().get(0)), this,
                        (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
            }

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
    public Map<PortFmi2Api, List<VariableFmi2Api<Object>>> getDerivatives(List<PortFmi2Api> ports, FmiBuilder.Scope<PStm> scope) {
        Map<PortFmi2Api, List<VariableFmi2Api<Object>>> derivativePortsToReturn = new HashMap<>();

        // If any target ports exists that can interpolate the port is also linked and derivatives should be retrieved.
        List<PortFmi2Api> portsLinkedToTargetsThatInterpolates = ports.stream().filter(p1 -> p1.getTargetPorts().stream().anyMatch(p2 -> {
            try {
                return p2.aMablFmi2ComponentAPI.getModelDescription().getCanInterpolateInputs();
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Exception occurred when accessing modeldescription: ", e);
            }
        })).collect(Collectors.toList());

        try {
            int maxOutputDerOrder = modelDescriptionContext.getModelDescription().getMaxOutputDerivativeOrder();
            if (portsLinkedToTargetsThatInterpolates.size() > 0 && maxOutputDerOrder > 0) {

                // Find derivative ports
                List<PortFmi2Api> derivativePorts = new ArrayList<>();
                modelDescriptionContext.getModelDescription().getDerivativesMap().entrySet().stream()
                        .filter(entry -> portsLinkedToTargetsThatInterpolates.stream()
                                .anyMatch(p -> p.getPortReferenceValue().equals(entry.getKey().getValueReference()))).forEach(e ->
                                // Find the PortFmi2Api representation of the scalarvariable derivative port.
                                getPorts().stream().filter(p -> p.getPortReferenceValue().equals(e.getValue().getValueReference())).findAny()
                                        .ifPresent(derivativePorts::add));

                if (derivativePorts.size() > 0) {
                    // Array size: number of ports for which to get derivatives multiplied the max derivative order.
                    int arraySize = derivativePorts.size() * maxOutputDerOrder;

                    ArrayVariableFmi2Api<Object> derValOutBuf = createBuffer(newRealType(), "DVal_OUT", arraySize, getDeclaredScope());
                    ArrayVariableFmi2Api<Object> derOrderOutBuf = createBuffer(newIntType(), "DOrder_OUT", arraySize, getDeclaredScope());
                    ArrayVariableFmi2Api<Object> derRefOutBuf = createBuffer(newUIntType(), "DRef_OUT", arraySize, getDeclaredScope());

                    // Loop arrays and assign derivative value reference and derivative order.
                    // E.g for two ports with and a max derivative order of two: derRefOutBuf = [derPortRef1, derPortRef1, derPortRef2,derPortRef2],
                    // derOrderOutBuf = [1,2,1,2]
                    PortFmi2Api derivativePort = null;
                    for (int arrayIndex = 0, portIndex = 0, order = 1; arrayIndex < arraySize; arrayIndex++) {
                        // Switch to the next port when we have traversed a length of 'maxOutputDerOrder' in the arrays and reset order.
                        if (arrayIndex % maxOutputDerOrder == 0) {
                            order = 1;
                            derivativePort = derivativePorts.get(portIndex);
                            derivativePortsToReturn.put(derivativePort, derValOutBuf.items().subList(arrayIndex, arrayIndex + maxOutputDerOrder));
                            portIndex++;
                        }

                        PStateDesignator dRefDesignator = derRefOutBuf.items().get(arrayIndex).getDesignator().clone();
                        scope.add(newAAssignmentStm(dRefDesignator, newAIntLiteralExp(derivativePort.getPortReferenceValue().intValue())));

                        PStateDesignator derOrderDesignator = derOrderOutBuf.items().get(arrayIndex).getDesignator().clone();
                        scope.add(newAAssignmentStm(derOrderDesignator, newAIntLiteralExp(order)));
                        order++;
                    }

                    // Create assignment statement which assigns derivatives to derValOutBuf by calling getRealOutputDerivatives with
                    // derRefOutBuf, arraySize and derOrderOutBuf.
                    PStm AStm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                            call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.GETREALOUTPUTDERIVATIVES),
                                    derRefOutBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) arraySize),
                                    derOrderOutBuf.getReferenceExp().clone(), derValOutBuf.getReferenceExp().clone()));
                    scope.add(AStm);

                    // If enabled handle potential errors from calling getRealOutputDerivatives
                    if (builder.getSettings().fmiErrorHandlingEnabled) {
                        FmiStatusErrorHandlingBuilder.generate(builder, createFunctionName(FmiFunctionType.GETREALOUTPUTDERIVATIVES), this,
                                (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
                    }
                }
            }

        } catch (InvocationTargetException | IllegalAccessException | XPathExpressionException e) {
            throw new RuntimeException("Exception occurred when retrieving derivatives: ", e);
        }

        return derivativePortsToReturn;
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get() {
        return get(builder.getDynamicScope(), outputPorts.toArray(FmiBuilder.Port[]::new));
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(int... valueReferences) {
        List<Integer> accept = Arrays.stream(valueReferences).boxed().collect(Collectors.toList());
        return get(builder.getDynamicScope(),
                outputPorts.stream().filter(p -> accept.contains(p.getPortReferenceValue().intValue())).toArray(FmiBuilder.Port[]::new));
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(String... names) {
        List<String> accept = Arrays.asList(names);
        FmiBuilder.Port[] ports = this.ports.stream().filter(p -> accept.contains(p.getName())).toArray(FmiBuilder.Port[]::new);
        return get(builder.getDynamicScope(), ports);
    }

    /**
     * Stores the final value in rootScope
     * Uses the rootScope for valueReferences
     */
    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> getAndShare(String... names) {

        Map<PortFmi2Api, VariableFmi2Api<V>> values = get(names);
        share(values);
        return values;
    }

    @Override
    public <V> Map<? extends FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> getAndShare(
            FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>... ports) {
        Map<PortFmi2Api, VariableFmi2Api<V>> values = get(ports);
        share(values);
        return values;
    }

    @Override
    public <V> Map<? extends FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> getAndShare() {
        Map<PortFmi2Api, VariableFmi2Api<V>> values = get();
        share(values);
        return values;
    }

    @Override
    public VariableFmi2Api getShared(String name) {
        return this.getPort(name).getSharedAsVariable();
    }

    @Override
    public VariableFmi2Api getShared(FmiBuilder.Port port) {
        return ((PortFmi2Api) port).getSharedAsVariable();
    }

    @Override
    public <V> VariableFmi2Api<V> getSingle(FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm> port) {
        return (VariableFmi2Api) this.get(port).entrySet().iterator().next().getValue();
    }

    private String createFunctionName(FmiFunctionType fun) {
        switch (fun) {
            case ENTERINITIALIZATIONMODE:
                return "enterInitializationMode";
            case EXITINITIALIZATIONMODE:
                return "exitInitializationMode";
            case SETUPEXPERIMENT:
                return "setupExperiment";
            case GETREALOUTPUTDERIVATIVES:
                return "getRealOutputDerivatives";
            case SETREALINPUTDERIVATIVES:
                return "setRealInputDerivatives";
            case TERMINATE:
                return "terminate";
            default:
                throw new RuntimeException("Attempting to call function that is type dependant without specifying type: " + fun);
        }

    }

    private String createFunctionName(FmiFunctionType fun, PortFmi2Api p) {
        return createFunctionName(fun, p.scalarVariable.getType().type);
    }

    private String createFunctionName(FmiFunctionType f, Fmi2ModelDescription.Types type) {
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
        functionName += type.name();
        return functionName;
    }

    @Override
    public VariableFmi2Api getSingle(String name) {
        return this.get(name).entrySet().iterator().next().getValue();
    }


    public void set(FmiBuilder.Port p, FmiBuilder.ExpressionValue v) {
        this.set(new PortValueExpresssionMapImpl(Map.of(p, v)));
    }

    public void set(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port p, FmiBuilder.ExpressionValue v) {
        this.set(scope, new PortValueMapImpl(Map.of(p, v)));
    }

    public void set(PortExpressionValueMap value) {
        this.set(builder.getDynamicScope().getActiveScope(), value);
    }

    public void set(FmiBuilder.Scope<PStm> scope, PortExpressionValueMap<Fmi2ModelDescription.ScalarVariable, PStm> value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        List<PortFmi2Api> selectedPorts = value.keySet().stream().map(PortFmi2Api.class::cast).collect(Collectors.toList());

        set(scope, selectedPorts, port -> {
            FmiBuilder.ExpressionValue value_ = value.get(port);
            return Map.entry(value_.getExp(), value_.getType());
        });
    }

    @Override
    public <V> void set(FmiBuilder.Scope<PStm> scope, PortValueMap<V, Fmi2ModelDescription.ScalarVariable, PStm> value) {


        if (value == null || value.isEmpty()) {
            return;
        }

        List<PortFmi2Api> selectedPorts = value.keySet().stream().map(PortFmi2Api.class::cast).collect(Collectors.toList());


        set(scope, selectedPorts, port -> {
            Object val = (value.get(port)).get();
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
    public <V> void set(FmiBuilder.Scope<PStm> scope, PortVariableMap<V, Fmi2ModelDescription.ScalarVariable, PStm> value) {

        List<PortFmi2Api> selectedPorts;
        if (value == null || value.isEmpty()) {
            return;
        } else {
            selectedPorts = value.keySet().stream().map(PortFmi2Api.class::cast).collect(Collectors.toList());
        }

        final PortVariableMap valueFinal = value;
        set(scope, selectedPorts,
                port -> Map.entry(((VariableFmi2Api) valueFinal.get(port)).getReferenceExp().clone(), ((VariableFmi2Api) valueFinal.get(port)).type));
    }

    public void set(FmiBuilder.Scope<PStm> scope, List<PortFmi2Api> selectedPorts, Function<PortFmi2Api, Map.Entry<PExp, PType>> portToValue) {

        Set<String> selectedPortsAsStrings = selectedPorts.stream()
                .map(p -> p.getName() + "-" + p.aMablFmi2ComponentAPI.getName() + "-" + p.aMablFmi2ComponentAPI.getOwner().getName())
                .collect(toSet());
        selectedPortsAsStrings.removeAll(
                ports.stream().map(p -> p.getName() + "-" + p.aMablFmi2ComponentAPI.getName() + "-" + p.aMablFmi2ComponentAPI.getOwner().getName())
                        .collect(toSet()));
        if (selectedPortsAsStrings.size() > 0) {
            throw new RuntimeException("Unable to set port(s) that is not declared in the FMU: " +
                    selectedPortsAsStrings.stream().map(name -> name.split("-")[0]).collect(Collectors.joining(", ")));
        }

        List<PortFmi2Api> sortedPorts =
                selectedPorts.stream().sorted(Comparator.comparing(FmiBuilder.Port::getPortReferenceValue)).collect(Collectors.toList());

        // Group by the string value of the port type as grouping by the port type itself doesnt utilise equals
        sortedPorts.stream().collect(Collectors.groupingBy(i -> i.getType().toString())).forEach((key, value) -> {
            ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();
            PType type = value.get(0).getType();
            for (int i = 0; i < value.size(); i++) {
                FmiBuilder.Port p = value.get(i);
                PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
                scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
            }

            ArrayVariableFmi2Api<Object> valBuf = getIOBuffer(type);
            for (int i = 0; i < value.size(); i++) {
                PortFmi2Api p = value.get(i);
                PStateDesignator designator = valBuf.items().get(i).getDesignator();

                scope.addAll(BuilderUtil.createTypeConvertingAssignment(builder, scope, designator.clone(), portToValue.apply(p).getKey().clone(),
                        portToValue.apply(p).getValue(), valBuf.type));
            }

            AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                    call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SET, value.get(0)), vrefBuf.getReferenceExp().clone(),
                            newAUIntLiteralExp((long) value.size()), valBuf.getReferenceExp().clone()));
            scope.add(stm);

            if (builder.getSettings().fmiErrorHandlingEnabled) {
                FmiStatusErrorHandlingBuilder.generate(builder, createFunctionName(FmiFunctionType.SET, sortedPorts.get(0)), this, (IMablScope) scope,
                        MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
            }

            try {
                if (builder.getSettings().setGetDerivatives && modelDescriptionContext.getModelDescription().getCanInterpolateInputs() &&
                        type.equals(new ARealNumericPrimitiveType())) {
                    setDerivativesForSharedPorts(value, scope);
                }
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Exception occurred when when setting derivatives.", e);
            }
        });
    }

    /**
     * @param ports The ports for which derivative should be set from SHARED derivative ports
     * @param scope The builder scope
     */
    private void setDerivativesForSharedPorts(List<PortFmi2Api> ports, FmiBuilder.Scope<PStm> scope) {
        // Find all ports for which derivatives should be passed together with the derivatives and their order.
        LinkedHashMap<PortFmi2Api, Map.Entry<PortFmi2Api, Integer>> mapPortsToDerPortsWithOrder =
                ports.stream().filter(port -> port.getSourcePort() != null).map(port -> {
                    try {
                        Map.Entry<PortFmi2Api, Integer> innerEntry;
                        // Find if port is in map of derivatives
                        Optional<Map.Entry<Fmi2ModelDescription.ScalarVariable, Fmi2ModelDescription.ScalarVariable>> derivativePortEntry =
                                port.getSourcePort().aMablFmi2ComponentAPI.getModelDescription().getDerivativesMap().entrySet().stream()
                                        .filter(e -> e.getKey().getValueReference().equals(port.getSourcePort().getPortReferenceValue())).findAny();


                        if (derivativePortEntry.isPresent()) {
                            // Find PortFmi2Api value of the scalarvariable derivative port and the components max output derivative.
                            innerEntry = Map.entry(port.getSourcePort().aMablFmi2ComponentAPI.getPort(
                                            derivativePortEntry.get().getValue().getValueReference().intValue()),
                                    port.getSourcePort().aMablFmi2ComponentAPI.getModelDescription().getMaxOutputDerivativeOrder());
                            return Map.entry(port, innerEntry);
                        }

                        return null;
                    } catch (XPathExpressionException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Exception occurred when accessing modeldescription: ", e);
                    }
                }).filter(Objects::nonNull).collect(LinkedHashMap::new, (map, item) -> map.put(item.getKey(), item.getValue()),  // Accumulator
                        Map::putAll);

        if (mapPortsToDerPortsWithOrder.size() > 0) {
            // Get the total array size as the sum of derivative orders.
            int arraySize = mapPortsToDerPortsWithOrder.values().stream().mapToInt(Map.Entry::getValue).sum();

            // Create input arrays
            ArrayVariableFmi2Api<Object> derValInBuf = createBuffer(newRealType(), "DVal_IN", arraySize, getDeclaredScope());
            ArrayVariableFmi2Api<Object> derOrderInBuf = createBuffer(newIntType(), "DOrder_IN", arraySize, getDeclaredScope());
            ArrayVariableFmi2Api<Object> derRefInBuf = createBuffer(newUIntType(), "DRef_IN", arraySize, getDeclaredScope());

            // Loop through arrays and assign the port reference, derivative order and derivative value.
            // E.g: for two derivative ports Out1 (linked to In1) and Out2 (linked to In2) with a max derivative order of 2 and 1: derValInBuf = [der
            // (Out1), der(der(Out1)),der(Out2))], derOrderInBuf = [1,2,1], derRefInBuf = [In1, In1, In2]
            int arrayIndex = 0;
            for (Map.Entry<PortFmi2Api, Map.Entry<PortFmi2Api, Integer>> entry : mapPortsToDerPortsWithOrder.entrySet()) {
                PortFmi2Api port = entry.getKey();
                int maxOrder = entry.getValue().getValue();
                for (int order = 1; order <= maxOrder; order++, arrayIndex++) {
                    // Assign to array variables
                    PStateDesignator derRefDesignator = derRefInBuf.items().get(arrayIndex).getDesignator().clone();
                    scope.add(newAAssignmentStm(derRefDesignator, newAIntLiteralExp(port.getPortReferenceValue().intValue())));

                    PStateDesignator derOrderDesignator = derOrderInBuf.items().get(arrayIndex).getDesignator().clone();
                    scope.add(newAAssignmentStm(derOrderDesignator, newAIntLiteralExp(order)));

                    PStateDesignator derValInDesignator = derValInBuf.items().get(arrayIndex).getDesignator().clone();
                    scope.add(newAAssignmentStm(derValInDesignator,
                            ((VariableFmi2Api) ((ArrayVariableFmi2Api) entry.getValue().getKey().getSharedAsVariable()).items()
                                    .get(order - 1)).getReferenceExp().clone()));
                }
            }

            setDerivatives(derValInBuf, derOrderInBuf, derRefInBuf, scope);
        }
    }

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
        int arraySize = derValInBuf.size();

        // Create set derivatives statement which calls setRealOutputDerivatives with derRefInBuf, arraySize, derOrderInBuf and
        // derValInBuf.
        PStm ifStm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SETREALINPUTDERIVATIVES),
                        derRefInBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) arraySize), derOrderInBuf.getReferenceExp().clone(),
                        derValInBuf.getReferenceExp().clone()));
        scope.add(ifStm);

        // If enabled handle potential errors from calling setRealInputDerivatives
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, createFunctionName(FmiFunctionType.SETREALINPUTDERIVATIVES), this, (IMablScope) scope,
                    MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public <V> void set(PortValueMap<V, Fmi2ModelDescription.ScalarVariable, PStm> value) {
        set(builder.getDynamicScope(), value);
    }

    @Override
    public <V> void set(FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm> port, FmiBuilder.Variable<PStm, V> value) {
        this.set(new PortVariableMapImpl(Map.of(port, value)));
    }

    @Override
    public <V> void set(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm> port,
            FmiBuilder.Variable<PStm, V> value) {
        this.set(scope, new PortVariableMapImpl(Map.of(port, value)));
    }

    @Override
    public void set(FmiBuilder.Port port, FmiBuilder.Value value) {
        PortValueMap map = new PortValueMapImpl();
        map.put(port, value);
        set(map);

    }


    @Override
    public <V> void set(PortVariableMap<V, Fmi2ModelDescription.ScalarVariable, PStm> value) {
        set(builder.getDynamicScope(), value);
    }

    @Override
    public void setLinked(FmiBuilder.Scope<PStm> scope, FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>... filterPorts) {

        List<PortFmi2Api> selectedPorts = ports.stream().filter(isLinked).collect(Collectors.toList());
        if (filterPorts != null && filterPorts.length != 0) {

            List<FmiBuilder.Port> filterList = Arrays.asList(filterPorts);

            for (FmiBuilder.Port p : filterList) {
                if (!isLinked.test(p)) {
                    logger.warn("Filter for setLinked contains unlined port. Its ignored. {}", p);
                }
            }

            selectedPorts = selectedPorts.stream().filter(filterList::contains).collect(Collectors.toList());
        }
        if (selectedPorts.size() == 0) {
            logger.warn("No linked input variables for FMU instance: " + this.getName());
            return;
        }


        for (PortFmi2Api port : selectedPorts) {
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
        this.setLinked(dynamicScope, (FmiBuilder.Port[]) null);
    }

    @Override
    public void setLinked(FmiBuilder.Port... filterPorts) {

        this.setLinked(dynamicScope, filterPorts);
    }

    @Override
    public void setLinked(String... filterNames) {
        List<String> accept = Arrays.asList(filterNames);
        this.setLinked(dynamicScope, getPorts().stream().filter(p -> accept.contains(p.getName())).toArray(FmiBuilder.Port[]::new));

    }

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
        PStm stm = stateTransitionFunction(FmiFunctionType.TERMINATE);
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "terminate", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public <V> void share(Map<? extends FmiBuilder.Port<Fmi2ModelDescription.ScalarVariable, PStm>, ? extends FmiBuilder.Variable<PStm, V>> values) {
        // Group by the string value of the port type as grouping by the port type itself doesnt utilise equals
        values.entrySet().stream().collect(Collectors.groupingBy(map -> ((PortFmi2Api) map.getKey()).getType().toString())).entrySet().stream()
                .forEach(map -> {
                    PType type = ((PortFmi2Api) map.getValue().get(0).getKey()).getType();

                    Map<FmiBuilder.Port, FmiBuilder.Variable> data =
                            map.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    data.keySet().stream().map(PortFmi2Api.class::cast).sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue))
                            .forEach(port -> {
                                //this is the sorted set of assignments, these can be replaced by a memcopy later
                                ArrayVariableFmi2Api<Object> buffer = getSharedBuffer(type);
                                if (port.getSharedAsVariable() == null) {
                                    ArrayVariableFmi2Api<Object> newBuf = growBuffer(buffer, 1);
                                    this.setSharedBuffer(newBuf, type);

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
                                        try {
                                            return modelDescriptionContext.getModelDescription().getDerivativesMap().entrySet().stream().anyMatch(
                                                    e -> e.getKey().getValueReference().equals(port.getPortReferenceValue()) &&
                                                            e.getValue().getValueReference().equals(derivativePort.getPortReferenceValue()));
                                        } catch (XPathExpressionException | InvocationTargetException | IllegalAccessException e) {
                                            throw new RuntimeException(
                                                    "Attempting to obtain shared values from a port that is linked but has no value shared. Share a value " +
                                                            "first. " + port);
                                        }
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

    private void setSharedBuffer(ArrayVariableFmi2Api<Object> newBuf, PType type) {
        this.sharedBuffer.entrySet().removeIf(x -> x.getKey().toString().equals(type.toString()));
        this.sharedBuffer.put(type, newBuf);

    }

    private ArrayVariableFmi2Api<Object> growBuffer(ArrayVariableFmi2Api<Object> buffer, int increaseByCount) {

        String ioBufName = ((AIdentifierExp) buffer.getReferenceExp()).getName().getText();

        int length = buffer.size() + increaseByCount;
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), buffer.type, length, null));

        buffer.getDeclaringStm().parent().replaceChild(buffer.getDeclaringStm(), var);
        // getDeclaredScope().addAfter(getDeclaringStm(), var);

        List<VariableFmi2Api<Object>> items = IntStream.range(buffer.size(), length).mapToObj(
                        i -> new VariableFmi2Api<>(var, buffer.type, this.getDeclaredScope(), builder.getDynamicScope(),
                                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                                newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i)))))
                .collect(Collectors.toList());

        //we can not replace these as some of them may be used and could potential have reference problems (they should not but just to be sure)
        items.addAll(0, buffer.items());

        return new ArrayVariableFmi2Api<>(var, buffer.type, getDeclaredScope(), builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIdentifierExp(ioBufName), items);

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

        StateMablVariableFmi2Api state =
                new StateMablVariableFmi2Api(stateVar, newANameType("FmiComponentState"), (IMablScope) scope, builder.getDynamicScope(),
                        newAIdentifierStateDesignator(stateName), newAIdentifierExp(stateName), builder, this);

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.getReferenceExp().clone(), "getState", Collections.singletonList(newARefExp(state.getReferenceExp().clone()))));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            FmiStatusErrorHandlingBuilder.generate(builder, "getState", this, (IMablScope) scope, MablApiBuilder.FmiStatus.FMI_ERROR,
                    MablApiBuilder.FmiStatus.FMI_FATAL);
        }

        return state;
    }

    public FmuVariableFmi2Api getOwner() {
        return this.owner;
    }

    public List<PortFmi2Api> getAllConnectedOutputs() {
        return this.ports.stream().filter(x -> x.scalarVariable.causality == Fmi2ModelDescription.Causality.Output && x.getTargetPorts().size() > 0)
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
        SET,
        ENTERINITIALIZATIONMODE,
        EXITINITIALIZATIONMODE,
        SETUPEXPERIMENT,
        GETREALOUTPUTDERIVATIVES,
        SETREALINPUTDERIVATIVES,
        TERMINATE
    }

    /**
     * Error and Fatal should lead to freeInstance calls followed by subsequent termination.
     */
    static class FmiStatusErrorHandlingBuilder {
        static void generate(MablApiBuilder builder, String method, ComponentVariableFmi2Api instance, IMablScope scope,
                MablApiBuilder.FmiStatus... statusesToFail) {
            if (statusesToFail == null || statusesToFail.length == 0) {
                return;
            }

            Function<MablApiBuilder.FmiStatus, PExp> checkStatusEq =
                    s -> newEqual(((IMablScope) scope).getFmiStatusVariable().getReferenceExp().clone(),
                            builder.getFmiStatusConstant(s).getReferenceExp().clone());

            PExp exp = checkStatusEq.apply(statusesToFail[0]);

            for (int i = 1; i < statusesToFail.length; i++) {
                exp = newOr(exp, checkStatusEq.apply(statusesToFail[i]));
            }

            ScopeFmi2Api thenScope = scope.enterIf(new PredicateFmi2Api(exp)).enterThen();

            // thenScope.add(newAAssignmentStm(builder.getGlobalExecutionContinue().getDesignator().clone(), newABoolLiteralExp(false)));

            for (MablApiBuilder.FmiStatus status : statusesToFail) {
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
