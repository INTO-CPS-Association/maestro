package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.BuilderUtil;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.PortValueMapImpl;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


@SuppressWarnings("rawtypes")
public class ComponentVariableFmi2Api extends VariableFmi2Api<Fmi2Builder.NamedVariable<PStm>> implements Fmi2Builder.Fmi2ComponentVariable<PStm> {
    final static Logger logger = LoggerFactory.getLogger(ComponentVariableFmi2Api.class);
    private final static int FMI_OK = 0;
    private final static int FMI_WARNING = 1;
    private final static int FMI_DISCARD = 2;
    private final static int FMI_ERROR = 3;
    private final static int FMI_FATAL = 4;
    private final static int FMI_PENDING = 5;
    private final static int FMI_STATUS_LAST_SUCCESSFUL = 2;
    final List<PortFmi2Api> outputPorts;
    final List<PortFmi2Api> inputPorts;
    final List<PortFmi2Api> ports;
    private final FmuVariableFmi2Api owner;
    private final String name;
    private final MablApiBuilder builder;
    private final Map<PType, ArrayVariableFmi2Api<Object>> ioBuffer = new HashMap<>();
    private final Map<PType, ArrayVariableFmi2Api<Object>> sharedBuffer = new HashMap<>();
    Predicate<Fmi2Builder.Port> isLinked = p -> ((PortFmi2Api) p).getSourcePort() != null;
    ModelDescriptionContext modelDescriptionContext;
    private DoubleVariableFmi2Api currentTimeVar = null;
    private BooleanVariableFmi2Api currentTimeStepFullStepVar = null;
    private ArrayVariableFmi2Api<Object> valueRefBuffer;
    private List<String> variabesToLog;

    public ComponentVariableFmi2Api(PStm declaration, FmuVariableFmi2Api parent, String name, ModelDescriptionContext modelDescriptionContext,
            MablApiBuilder builder, IMablScope declaringScope, PStateDesignator designator, PExp referenceExp) {
        super(declaration, newANameType("FMI2Component"), declaringScope, builder.getDynamicScope(), designator, referenceExp);
        this.owner = parent;
        this.name = name;

        this.modelDescriptionContext = modelDescriptionContext;
        this.builder = builder;

        ports = modelDescriptionContext.nameToSv.values().stream().map(sv -> new PortFmi2Api(this, sv))
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        outputPorts = ports.stream().filter(p -> p.scalarVariable.causality == ModelDescription.Causality.Output)
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());

        inputPorts = ports.stream().filter(p -> p.scalarVariable.causality == ModelDescription.Causality.Input)
                .sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).collect(Collectors.toUnmodifiableList());
    }

    public List<PortFmi2Api> getVariablesToLog() {
        return this.getPorts(this.variabesToLog.toArray(new String[0]));
    }

    public void setVariablesToLog(List<RelationVariable> variablesToLog) {
        this.variabesToLog = variablesToLog.stream().map(x -> x.scalarVariable.getName()).collect(Collectors.toList());
    }

    @Override
    public <V> void share(Fmi2Builder.Port port, Fmi2Builder.Variable<PStm, V> value) {
        Map<Fmi2Builder.Port, Fmi2Builder.Variable<PStm, V>> map = new HashMap<>();
        map.put(port, value);
        share(map);
    }

    private DoubleVariableFmi2Api getCurrentTimeVar() {
        if (currentTimeVar == null) {
            String name = builder.getNameGenerator().getName(this.name, "current", "time");
            PStm var = newVariable(name, newRealType());
            this.getDeclaredScope().addAfter(this.getDeclaringStm(), var);
            currentTimeVar = new DoubleVariableFmi2Api(var, this.getDeclaredScope(), dynamicScope, newAIdentifierStateDesignator(name),
                    newAIdentifierExp(name));
        }
        return currentTimeVar;
    }

    private BooleanVariableFmi2Api getCurrentTimeFullStepVar() {
        if (currentTimeStepFullStepVar == null) {
            String name = builder.getNameGenerator().getName(this.name, "current", "time", "full", "step");
            PStm var = newVariable(name, newBoleanType());
            this.getDeclaredScope().addAfter(this.getDeclaringStm(), var);
            currentTimeStepFullStepVar = new BooleanVariableFmi2Api(var, this.getDeclaredScope(), dynamicScope, newAIdentifierStateDesignator(name),
                    newAIdentifierExp(name));
        }
        return currentTimeStepFullStepVar;
    }

    private ArrayVariableFmi2Api<Object> getValueReferenceBuffer() {
        if (this.valueRefBuffer == null) {
            this.valueRefBuffer = createBuffer(newUIntType(), "VRef", modelDescriptionContext.valRefToSv.size());
        }
        return this.valueRefBuffer;
    }

    private ArrayVariableFmi2Api<Object> getIOBuffer(PType type) {
        if (!this.ioBuffer.containsKey(type)) {
            this.ioBuffer.put(type, createBuffer(type, "IO", modelDescriptionContext.valRefToSv.size()));
        }
        return this.ioBuffer.get(type);
    }

    private ArrayVariableFmi2Api<Object> getSharedBuffer(PType type) {
        Optional<PType> first = this.sharedBuffer.keySet().stream().filter(x -> x.toString().equals(type.toString())).findFirst();
        if (first.isEmpty()) {
            ArrayVariableFmi2Api<Object> value = createBuffer(type, "Share", 0);
            this.sharedBuffer.put(type, value);
            return value;

        } else {
            return this.sharedBuffer.get(first.get());
        }
    }

    private ArrayVariableFmi2Api<Object> createBuffer(PType type, String prefixPostfix, int length) {

        //lets find a good place to store the buffer.
        String ioBufName = builder.getNameGenerator().getName(this.name, type + "", prefixPostfix);
        PInitializer initializer = null;
        if (length > 0) {
            //Bug in the interpreter it relies on values being there
            if (type instanceof ARealNumericPrimitiveType) {
                initializer = newAArrayInitializer(IntStream.range(0, length).mapToObj(i -> newARealLiteralExp(0d)).collect(Collectors.toList()));
            } else if (type instanceof AIntNumericPrimitiveType || type instanceof AUIntNumericPrimitiveType) {
                initializer = newAArrayInitializer(IntStream.range(0, length).mapToObj(i -> newAIntLiteralExp(0)).collect(Collectors.toList()));

            } else if (type instanceof ABooleanPrimitiveType) {
                initializer = newAArrayInitializer(IntStream.range(0, length).mapToObj(i -> newABoolLiteralExp(false)).collect(Collectors.toList()));

            } else if (type instanceof AStringPrimitiveType) {
                initializer = newAArrayInitializer(IntStream.range(0, length).mapToObj(i -> newAStringLiteralExp("")).collect(Collectors.toList()));

            }
        }
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length, initializer));

        getDeclaredScope().addAfter(getDeclaringStm(), var);

        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(
                i -> new VariableFmi2Api<>(var, type, this.getDeclaredScope(), builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i)))))
                .collect(Collectors.toList());

        return new ArrayVariableFmi2Api<>(var, type, getDeclaredScope(), builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIdentifierExp(ioBufName), items);

    }

    @Override
    public void setupExperiment(boolean toleranceDefined, double tolerance, Fmi2Builder.DoubleVariable<PStm> startTime, boolean endTimeDefined,
            Fmi2Builder.DoubleVariable<PStm> endTime) {
        this.setupExperiment(toleranceDefined, tolerance, ((DoubleVariableFmi2Api) startTime).getReferenceExp().clone(), endTimeDefined,
                ((DoubleVariableFmi2Api) endTime).getReferenceExp().clone());
    }

    @Override
    public void setupExperiment(boolean toleranceDefined, double tolerance, double startTime, boolean endTimeDefined, double endTime) {
        this.setupExperiment(toleranceDefined, tolerance, newARealLiteralExp(startTime), endTimeDefined, newARealLiteralExp(endTime));
    }

    private void setupExperiment(boolean toleranceDefined, double tolerance, PExp startTime, boolean endTimeDefined, PExp endTime) {
        DynamicActiveBuilderScope scope = builder.getDynamicScope();
        this.setupExperiment(scope, toleranceDefined, tolerance, startTime, endTimeDefined, endTime);
    }

    private void setupExperiment(Fmi2Builder.Scope<PStm> scope, boolean toleranceDefined, double tolerance, PExp startTime, boolean endTimeDefined,
            PExp endTime) {
        AAssigmentStm stm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SETUPEXPERIMENT), new ArrayList<>(
                        Arrays.asList(newABoolLiteralExp(toleranceDefined), newARealLiteralExp(tolerance), startTime,
                                newABoolLiteralExp(endTimeDefined), endTime))));
        scope.add(stm);
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
    public void setupExperiment(Fmi2Builder.Scope<PStm> scope, boolean toleranceDefined, double tolerance, Fmi2Builder.DoubleVariable<PStm> startTime,
            boolean endTimeDefined, Fmi2Builder.DoubleVariable<PStm> endTime) {
        this.setupExperiment(scope, toleranceDefined, tolerance, ((DoubleVariableFmi2Api) startTime).getReferenceExp().clone(), endTimeDefined,
                ((DoubleVariableFmi2Api) endTime).getReferenceExp().clone());
    }

    @Override
    public void setupExperiment(Fmi2Builder.Scope<PStm> scope, boolean toleranceDefined, double tolerance, double startTime, boolean endTimeDefined,
            double endTime) {
        this.setupExperiment(scope, toleranceDefined, tolerance, newARealLiteralExp(startTime), endTimeDefined, newARealLiteralExp(endTime));
    }

    @Override
    public void enterInitializationMode(Fmi2Builder.Scope<PStm> scope) {
        PStm stm = stateTransitionFunction(FmiFunctionType.ENTERINITIALIZATIONMODE);
        scope.add(stm);

    }

    @Override
    public void exitInitializationMode(Fmi2Builder.Scope<PStm> scope) {
        PStm stm = stateTransitionFunction(FmiFunctionType.EXITINITIALIZATIONMODE);
        scope.add(stm);
    }

    @Override
    public Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step(Fmi2Builder.Scope<PStm> scope,
            Fmi2Builder.DoubleVariable<PStm> currentCommunicationPoint, Fmi2Builder.DoubleVariable<PStm> communicationStepSize,
            Fmi2Builder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
        return step(scope, currentCommunicationPoint, communicationStepSize, ((VariableFmi2Api) noSetFMUStatePriorToCurrentPoint).getReferenceExp());
    }

    @Override
    public Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step(Fmi2Builder.Scope<PStm> scope,
            Fmi2Builder.DoubleVariable<PStm> currentCommunicationPoint, Fmi2Builder.DoubleVariable<PStm> communicationStepSize) {
        return step(scope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
    }

    @Override
    public Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step(
            Fmi2Builder.DoubleVariable<PStm> currentCommunicationPoint, Fmi2Builder.DoubleVariable<PStm> communicationStepSize,
            Fmi2Builder.BoolVariable<PStm> noSetFMUStatePriorToCurrentPoint) {
        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint);
    }

    @Override
    public Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step(
            Fmi2Builder.DoubleVariable<PStm> currentCommunicationPoint, Fmi2Builder.DoubleVariable<PStm> communicationStepSize) {
        return step(dynamicScope, currentCommunicationPoint, communicationStepSize, newABoolLiteralExp(false));
    }

    private Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step(Fmi2Builder.Scope<PStm> scope,
            Fmi2Builder.DoubleVariable<PStm> currentCommunicationPoint, Fmi2Builder.DoubleVariable<PStm> communicationStepSize,
            PExp noSetFMUStatePriorToCurrentPoint) {

        scope.add(newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                newACallExp(this.getReferenceExp().clone(), newAIdentifier("doStep"),
                        Arrays.asList(((VariableFmi2Api) currentCommunicationPoint).getReferenceExp().clone(),
                                ((VariableFmi2Api) communicationStepSize).getReferenceExp().clone(), noSetFMUStatePriorToCurrentPoint.clone()))));

        scope.add(newIf(newNotEqual(builder.getGlobalFmiStatus().getReferenceExp().clone(), newAIntLiteralExp(FMI_OK)), newABlockStm(
                newIf(newEqual(builder.getGlobalFmiStatus().getReferenceExp().clone(), newAIntLiteralExp(FMI_DISCARD)), newABlockStm(
                        newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
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
            default:
                throw new RuntimeException("Attempting to call state transition function with non-state transition function type: " + type);
        }

        AAssigmentStm stm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
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

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(Fmi2Builder.Port... ports) {
        return get(builder.getDynamicScope(), ports);
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.Port... ports) {

        List<PortFmi2Api> selectedPorts;
        if (ports == null || ports.length == 0) {
            selectedPorts = outputPorts;
        } else {
            selectedPorts = Arrays.stream(ports).map(PortFmi2Api.class::cast).collect(Collectors.toList());
        }

        List<PortFmi2Api> sortedPorts =
                selectedPorts.stream().sorted(Comparator.comparing(Fmi2Builder.Port::getPortReferenceValue)).collect(Collectors.toList());
        ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();

        for (int i = 0; i < sortedPorts.size(); i++) {
            PortFmi2Api p = sortedPorts.get(i);
            PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
            scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
        }

        PType type = sortedPorts.get(0).getType();
        ArrayVariableFmi2Api<Object> valBuf = getIOBuffer(type);
        AAssigmentStm stm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.GET, sortedPorts.get(0)), vrefBuf.getReferenceExp().clone(),
                        newAUIntLiteralExp((long) sortedPorts.size()), valBuf.getReferenceExp().clone()));


        scope.add(stm);

        Map<PortFmi2Api, VariableFmi2Api<V>> results = new HashMap<>();

        for (int i = 0; i < sortedPorts.size(); i++) {
            results.put(sortedPorts.get(i), (VariableFmi2Api<V>) valBuf.items().get(i));
        }

        return results;
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get() {
        return get(builder.getDynamicScope(), outputPorts.toArray(Fmi2Builder.Port[]::new));
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(int... valueReferences) {
        List<Integer> accept = Arrays.stream(valueReferences).boxed().collect(Collectors.toList());
        return get(builder.getDynamicScope(),
                outputPorts.stream().filter(p -> accept.contains(p.getPortReferenceValue().intValue())).toArray(Fmi2Builder.Port[]::new));
    }

    @Override
    public <V> Map<PortFmi2Api, VariableFmi2Api<V>> get(String... names) {
        List<String> accept = Arrays.asList(names);
        return get(builder.getDynamicScope(), outputPorts.stream().filter(p -> accept.contains(p.getName())).toArray(Fmi2Builder.Port[]::new));

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

    private String createFunctionName(FmiFunctionType fun) {
        switch (fun) {
            case ENTERINITIALIZATIONMODE:
                return "enterInitializationMode";
            case EXITINITIALIZATIONMODE:
                return "exitInitializationMode";
            default:
                throw new RuntimeException("Attempting to call function that is type dependant without specifying type: " + fun);
        }

    }

    private String createFunctionName(FmiFunctionType fun, PortFmi2Api p) {
        return createFunctionName(fun, p.scalarVariable.getType().type);
    }

    private String createFunctionName(FmiFunctionType f, ModelDescription.Types type) {
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
        return (VariableFmi2Api) this.get(name).entrySet().iterator().next().getValue();
    }

    @Override
    public <V> void set(Fmi2Builder.Scope<PStm> scope, PortValueMap<V> value) {


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
    public <V> void set(Fmi2Builder.Scope<PStm> scope, PortVariableMap<PStm, V> value) {

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

    public void set(Fmi2Builder.Scope<PStm> scope, List<PortFmi2Api> selectedPorts, Function<PortFmi2Api, Map.Entry<PExp, PType>> portToValue) {

        List<PortFmi2Api> sortedPorts =
                selectedPorts.stream().sorted(Comparator.comparing(Fmi2Builder.Port::getPortReferenceValue)).collect(Collectors.toList());
        ArrayVariableFmi2Api<Object> vrefBuf = getValueReferenceBuffer();


        for (int i = 0; i < sortedPorts.size(); i++) {
            Fmi2Builder.Port p = sortedPorts.get(i);
            PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
            scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
        }

        PType type = sortedPorts.get(0).getType();
        ArrayVariableFmi2Api<Object> valBuf = getIOBuffer(type);
        for (int i = 0; i < sortedPorts.size(); i++) {
            PortFmi2Api p = sortedPorts.get(i);
            PStateDesignator designator = valBuf.items().get(i).getDesignator();


            scope.addAll(BuilderUtil.createTypeConvertingAssignment(builder, scope, designator.clone(), portToValue.apply(p).getKey().clone(),
                    portToValue.apply(p).getValue(), valBuf.type));
        }

        AAssigmentStm stm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), createFunctionName(FmiFunctionType.SET, sortedPorts.get(0)), vrefBuf.getReferenceExp().clone(),
                        newAUIntLiteralExp((long) sortedPorts.size()), valBuf.getReferenceExp().clone()));
        scope.add(stm);
    }

    @Override
    public <V> void set(PortValueMap<V> value) {
        set(builder.getDynamicScope(), value);
    }

    @Override
    public void set(Fmi2Builder.Port port, Fmi2Builder.Value value) {
        PortValueMap map = new PortValueMapImpl();
        map.put(port, value);
        set(map);
    }

    @Override
    public <V> void set(PortVariableMap<PStm, V> value) {
        set(builder.getDynamicScope(), value);
    }

    @Override
    public void setLinked(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.Port... filterPorts) {

        List<PortFmi2Api> selectedPorts = ports.stream().filter(isLinked).collect(Collectors.toList());
        if (filterPorts != null && filterPorts.length != 0) {

            List<Fmi2Builder.Port> filterList = Arrays.asList(filterPorts);

            for (Fmi2Builder.Port p : filterList) {
                if (!isLinked.test(p)) {
                    logger.warn("Filter for setLinked contains unlined port. Its ignored. {}", p);
                }
            }

            selectedPorts = selectedPorts.stream().filter(filterList::contains).collect(Collectors.toList());
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
        this.setLinked(dynamicScope, (Fmi2Builder.Port[]) null);
    }

    @Override
    public void setLinked(Fmi2Builder.Port... filterPorts) {

        this.setLinked(dynamicScope, filterPorts);
    }

    @Override
    public void setLinked(String... filterNames) {
        List<String> accept = Arrays.asList(filterNames);
        this.setLinked(dynamicScope, getPorts().stream().filter(p -> accept.contains(p.getName())).toArray(Fmi2Builder.Port[]::new));

    }

    @Override
    public void setLinked(long... filterValueReferences) {
        List<Long> accept = Arrays.stream(filterValueReferences).boxed().collect(Collectors.toList());
        this.setLinked(dynamicScope, getPorts().stream().filter(p -> accept.contains(p.getPortReferenceValue())).toArray(Fmi2Builder.Port[]::new));

    }

    @Override
    public void setInt(Map<? extends Integer, ? extends Fmi2Builder.Value<Integer>> values) {

    }

    @Override
    public void setString(Map<? extends String, ? extends Fmi2Builder.Value<String>> value) {

    }

    @Override
    public <V> void share(Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.Variable<PStm, V>> values) {
        values.entrySet().stream().collect(Collectors.groupingBy(map -> ((PortFmi2Api) map.getKey()).getType())).entrySet().stream().forEach(map -> {
            PType type = map.getKey();
            Map<Fmi2Builder.Port, Fmi2Builder.Variable> data =
                    map.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            data.keySet().stream().map(PortFmi2Api.class::cast).sorted(Comparator.comparing(PortFmi2Api::getPortReferenceValue)).forEach(port -> {
                //this is the sorted set of assignments, these can be replaced by a memcopy later
                ArrayVariableFmi2Api<Object> buffer = getSharedBuffer(type);
                if (port.getSharedAsVariable() == null) {
                    ArrayVariableFmi2Api<Object> newBuf = growBuffer(buffer, 1);
                    this.setSharedBuffer(newBuf, type);

                    // TODO: Variable is not being added to buffer items.

                    VariableFmi2Api<Object> newShared = newBuf.items().get(newBuf.items().size() - 1);
                    port.setSharedAsVariable(newShared);
                }

                PStateDesignator designator = port.getSharedAsVariable().getDesignator();
                builder.getDynamicScope().addAll(BuilderUtil.createTypeConvertingAssignment(builder, dynamicScope, designator.clone(),
                        ((VariableFmi2Api) data.get(port)).getReferenceExp().clone(), port.getType(),
                        ((VariableFmi2Api) ((VariableFmi2Api<?>) data.get(port))).type));
            });

        });
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
    public Fmi2Builder.StateVariable<PStm> getState() {

        return getState(builder.getDynamicScope());

    }

    @Override
    public Fmi2Builder.StateVariable<PStm> getState(Fmi2Builder.Scope<PStm> scope) {

        String stateName = builder.getNameGenerator().getName(name, "state");
        PStm stateVar = newVariable(stateName, newANameType("FmiComponentState"));
        scope.add(stateVar);

        StateMablVariableFmi2Api state =
                new StateMablVariableFmi2Api(stateVar, newANameType("FmiComponentState"), (IMablScope) scope, builder.getDynamicScope(),
                        newAIdentifierStateDesignator(stateName), newAIdentifierExp(stateName), builder, this);

        AAssigmentStm stm = newAAssignmentStm(builder.getGlobalFmiStatus().getDesignator().clone(),
                call(this.getReferenceExp().clone(), "getState", Collections.singletonList(newARefExp(state.getReferenceExp().clone()))));
        scope.add(stm);

        return state;
    }

    public FmuVariableFmi2Api getOwner() {
        return this.owner;
    }

    public List<PortFmi2Api> getAllConnectedOutputs() {
        return this.ports.stream().filter(x -> x.scalarVariable.causality == ModelDescription.Causality.Output && x.getTargetPorts().size() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return this.name;
    }


    public enum FmiFunctionType {
        GET,
        SET,
        ENTERINITIALIZATIONMODE,
        EXITINITIALIZATIONMODE,
        SETUPEXPERIMENT
    }


}
