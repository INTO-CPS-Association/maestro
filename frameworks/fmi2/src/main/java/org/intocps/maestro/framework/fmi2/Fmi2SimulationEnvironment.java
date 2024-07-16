package org.intocps.maestro.framework.fmi2;


import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.MultiModel;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.core.*;
import org.intocps.maestro.parser.template.MablSwapConditionParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public class Fmi2SimulationEnvironment implements ISimulationEnvironment, ISimulationEnvironmentTransfer {
    final static Logger logger = LoggerFactory.getLogger(Fmi2SimulationEnvironment.class);
    private final Map<String, String> instanceLexToInstanceName = new HashMap<>();
    private final Map<String, List<String>> instanceNameToLogLevels = new HashMap<>();
    Map<LexIdentifier, Set<Relation>> variableToRelations = new HashMap<>();
    Map<String, FrameworkUnitInfo> instanceNameToInstanceComponentInfo = new HashMap<>();
    HashMap<String, ModelDescription> fmuKeyToModelDescription = new HashMap<>();
    Map<String, URI> fmuToUri = null;
    Map<String, RelationVariable> variables = new HashMap<>();
    Map<String, List<RelationVariable>> globalVariablesToLogForInstance = new HashMap<>();
    Map<String, String> instanceToModelTransfer = new HashMap<>();
    Map<String, ModelSwapInfo> instanceToModelSwap = new HashMap<>();
    private String faultInjectionConfigurationPath;


    protected Fmi2SimulationEnvironment(Fmi2SimulationEnvironmentConfiguration msg, ModelDescriptionResolver resolver) throws Exception {
        initialize(msg, resolver);
    }

    public static Fmi2SimulationEnvironment of(File file, IErrorReporter reporter) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return of(is, reporter);
        }
    }

    public static Fmi2SimulationEnvironment of(Fmi2SimulationEnvironmentConfiguration msg, IErrorReporter reporter,
            ModelDescriptionResolver resolver) throws Exception {
        return new Fmi2SimulationEnvironment(msg, resolver);
    }

    public static Fmi2SimulationEnvironment of(Fmi2SimulationEnvironmentConfiguration msg, IErrorReporter reporter) throws Exception {
        return of(msg, reporter, new FileModelDescriptionResolver());
    }

    public static Fmi2SimulationEnvironment of(InputStream inputStream, IErrorReporter reporter) throws Exception {
        return of(Fmi2SimulationEnvironmentConfiguration.createFromJsonString(new String(inputStream.readAllBytes())), reporter);
    }

    public static List<ModelConnection> buildConnections(Map<String, List<String>> connections) throws Exception {
        List<ModelConnection> list = new Vector<>();
        if (connections == null) {
            return new ArrayList<>();
        }
        for (Map.Entry<String, List<String>> entry : connections.entrySet()) {
            for (String input : entry.getValue()) {
                list.add(new ModelConnection(ModelConnection.Variable.parse(entry.getKey()), ModelConnection.Variable.parse(input)));
            }
        }

        return list;
    }

    private static Map<String, List<org.intocps.maestro.framework.fmi2.RelationVariable>> getEnvironmentVariablesToLog(
            Map<String, List<String>> variablesToLogMap,
            Map<String, List<org.intocps.maestro.framework.fmi2.RelationVariable>> globalVariablesToLogForInstance) {

        Function<String, String> extractInstance = x -> x.split("}.")[1];
        Map<String, List<org.intocps.maestro.framework.fmi2.RelationVariable>> t = variablesToLogMap.entrySet().stream().collect(
                Collectors.toMap(entry -> extractInstance.apply(entry.getKey()),
                        entry -> globalVariablesToLogForInstance.get(extractInstance.apply(entry.getKey())).stream()
                                .filter(x -> entry.getValue().contains(x.getName())).collect(Collectors.toList())));
        return t;

    }

    @Override
    public List<RelationVariable> getConnectedOutputs() {
        return getInstances().stream().flatMap(instance -> this.getRelations(new LexIdentifier(instance.getKey(), null)).stream()
                .filter(relation -> (relation.getOrigin() == Relation.InternalOrExternal.External) &&
                        (relation.getDirection() == Relation.Direction.OutputToInput)).map(x -> x.getSource())).collect(Collectors.toList());

    }

    @Override
    public Set<Map.Entry<String, String>> getModelTransfers() {
        return this.instanceToModelTransfer.entrySet();
    }

    public ModelSwapInfo getModelSwapInfoByInstanceName(String name) {
        return this.instanceToModelSwap.get(name);
    }

    public Set<Map.Entry<String, ModelSwapInfo>> getModelSwaps() {
        return this.instanceToModelSwap.entrySet();
    }

    public Set<Relation> getModelSwapRelations() {
        Set<Relation> relations = new HashSet<>();
        for (Map.Entry<String, ModelSwapInfo> entry : this.instanceToModelSwap.entrySet()) {
            for (Set<Relation> relation : entry.getValue().swapRelations.values()) {
                relations.addAll(relation);
            }
        }
        return relations;
    }

    public void setLexNameToInstanceNameMapping(String lexName, String instanceName) {
        this.instanceLexToInstanceName.put(lexName, instanceName);
    }

    public FrameworkUnitInfo getInstanceByLexName(String lexName) {
        if (!this.instanceNameToInstanceComponentInfo.containsKey(lexName)) {
            throw new RuntimeException("Unable to locate instance named " + lexName + " in the simulation environment.");
        }
        return this.instanceNameToInstanceComponentInfo.get(lexName);
    }

    /**
     * Retrieves all variables that should be logged for the given instance. This is a combination of specified logvariables and connected outputs
     *
     * @param instanceName
     * @return
     */
    @Override
    public List<RelationVariable> getVariablesToLog(String instanceName) {
        List<RelationVariable> vars = this.globalVariablesToLogForInstance.get(instanceName);
        if (vars == null) {
            return new ArrayList<>();
        } else {
            return vars;
        }
    }

    public Set<Map.Entry<String, ModelDescription>> getFmusWithModelDescriptions() {
        return this.fmuKeyToModelDescription.entrySet();
    }

    @Override
    public Set<? extends Map.Entry<String, ? extends FrameworkUnitInfo>> getInstances() {
        return this.instanceNameToInstanceComponentInfo.entrySet();
    }

    public Set<Map.Entry<String, URI>> getFmuToUri() {
        return this.fmuToUri.entrySet();
    }

    public URI getUriFromFMUName(String fmuName) {
        return this.fmuToUri.get(fmuName);
    }


    ModelSwapInfo convert(MultiModel.ModelSwap swap) throws Exception {
        MultiModel.ModelSwap modelSwap = swap;
        return new ModelSwapInfo(modelSwap.swapInstance,
                modelSwap.swapCondition == null ? null : MablSwapConditionParserUtil.parse(CharStreams.fromString(modelSwap.swapCondition)),
                modelSwap.stepCondition == null ? null : MablSwapConditionParserUtil.parse(CharStreams.fromString(modelSwap.stepCondition)),
                modelSwap.swapConnections = swap.swapConnections);
    }


    private void addInstance(ModelConnection.Variable var, Fmi2SimulationEnvironmentConfiguration msg) throws Exception {

        if (!instanceNameToInstanceComponentInfo.containsKey(var.instance.instanceName)) {
            ModelDescription md = fmuKeyToModelDescription.get(var.instance.key);
            if (md instanceof Fmi2ModelDescription) {
                ComponentInfo instanceComponentInfo = new ComponentInfo((Fmi2ModelDescription) md, var.instance.key);
                if (msg.faultInjectInstances != null && msg.faultInjectInstances.containsKey(var.instance.instanceName)) {
                    instanceComponentInfo.setFaultInject(msg.faultInjectInstances.get(var.instance.instanceName));
                }
                if (msg.modelSwaps != null && msg.modelSwaps.containsKey(var.instance.instanceName)) {
                    instanceToModelSwap.put(var.instance.instanceName, convert(msg.modelSwaps.get(var.instance.instanceName)));
                }
                if (msg.modelTransfers != null && msg.modelTransfers.containsKey(var.instance.instanceName)) {
                    instanceToModelTransfer.put(var.instance.instanceName, msg.modelTransfers.get(var.instance.instanceName));
                }
                instanceNameToInstanceComponentInfo.put(var.instance.instanceName, instanceComponentInfo);
            } else if (md instanceof Fmi3ModelDescription) {
                instanceNameToInstanceComponentInfo.put(var.instance.instanceName, new InstanceInfo((Fmi3ModelDescription) md, var.instance.key));
            } else {
                logger.warn("Cannot add instance as model description type is unknown: {}", var.instance.key);
            }
        }
    }

    private void initialize(Fmi2SimulationEnvironmentConfiguration msg, ModelDescriptionResolver resolver) throws Exception {
        // Remove { } around fmu name.
        Map<String, URI> fmuToURI = msg.getFmuFiles();

        // Build map from fmuKey to ModelDescription
        this.fmuToUri = fmuToURI;
        List<ModelConnection> connections = buildConnections(msg.getConnections());
        List<ModelConnection> swapConnections = buildConnections(msg.getModelSwapConnections());

        HashMap<String, ModelDescription> fmuKeyToModelDescription = buildFmuKeyToFmuMD(fmuToURI, resolver);
        this.fmuKeyToModelDescription = fmuKeyToModelDescription;

        if (msg.faultInjectConfigurationPath != null && !msg.faultInjectConfigurationPath.isEmpty()) {
            if ((new File(msg.faultInjectConfigurationPath).exists())) {
                this.faultInjectionConfigurationPath = msg.faultInjectConfigurationPath;
            } else {
                throw new EnvironmentException("Failed to find the fault injection configuration file: " + msg.faultInjectConfigurationPath);
            }
        }

        // Build map from InstanceName to InstanceComponentInfo
        Set<ModelConnection.ModelInstance> instancesFromConnections = new HashSet<>();
        for (ModelConnection instance : Stream.concat(connections.stream(), swapConnections.stream()).collect(Collectors.toList())) {
            instancesFromConnections.add(instance.from.instance);
            instancesFromConnections.add(instance.to.instance);

            addInstance(instance.from, msg);
            addInstance(instance.to, msg);

        }
        // Build relations
        this.variableToRelations = buildRelations(msg, connections, instancesFromConnections);

        instanceToModelSwap.forEach((key, value) -> {
            try {
                value.swapRelations = buildRelations(msg, buildConnections(value.swapConnections), instancesFromConnections);
                value.swapRelations.entrySet().forEach(e -> {
                    e.getValue().removeIf(r -> r.origin.name().equals("Internal"));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private Map<LexIdentifier, Set<Relation>> buildRelations(Fmi2SimulationEnvironmentConfiguration msg, List<ModelConnection> connections,
            Set<ModelConnection.ModelInstance> instancesFromConnections) throws XPathExpressionException, InvocationTargetException, IllegalAccessException, EnvironmentException {
        Map<LexIdentifier, Set<Relation>> idToRelations = new HashMap<>();
        for (ModelConnection.ModelInstance instance : instancesFromConnections) {
            LexIdentifier instanceLexIdentifier = new LexIdentifier(instance.instanceName, null);


            // Add the instance to the globalVariablesToLogForInstance map.
            List<RelationVariable> globalVariablesToLogForGivenInstance;
            if (this.globalVariablesToLogForInstance.containsKey(instance.instanceName)) {
                globalVariablesToLogForGivenInstance = this.globalVariablesToLogForInstance.get(instance.instanceName);
            } else {
                globalVariablesToLogForGivenInstance = new ArrayList<>();
            }
            this.globalVariablesToLogForInstance.putIfAbsent(instance.instanceName, globalVariablesToLogForGivenInstance);

            {
                ModelDescription md = this.fmuKeyToModelDescription.get(instance.key);
                if (md instanceof Fmi2ModelDescription) {
                    buildFmi2Relation((Fmi2ModelDescription) md, instance, instanceLexIdentifier, idToRelations, connections,
                            globalVariablesToLogForGivenInstance);
                } else if (md instanceof Fmi3ModelDescription) {
                    buildFmi3Relation((Fmi3ModelDescription) md, instance, instanceLexIdentifier, idToRelations, connections,
                            globalVariablesToLogForGivenInstance);
                }
            }

            // Create a globalLogVariablesMap that is a merge between connected outputs, logVariables and livestream.
            HashMap<String, List<String>> globalLogVariablesMaps = new HashMap<>();
            if (msg != null && msg.logVariables != null) {
                globalLogVariablesMaps.putAll(msg.logVariables);
            }
            if (msg != null && msg.livestream != null) {
                msg.livestream.forEach((k, v) -> globalLogVariablesMaps.merge(k, v, (v1, v2) -> {
                    Set<String> set = new TreeSet<>(v1);
                    set.addAll(v2);
                    return new ArrayList<>(set);
                }));
            }


            List<RelationVariable> variablesToLogForInstance = new ArrayList<>();
            String logVariablesKey = instance.key + "." + instance.instanceName;
            if (globalLogVariablesMaps.containsKey(logVariablesKey)) {
                for (String s : globalLogVariablesMaps.get(logVariablesKey)) {

                    ModelDescription md = this.fmuKeyToModelDescription.get(instance.key);

                    RelationVariable rvar = null;
                    if (md instanceof Fmi2ModelDescription) {
                        Fmi2ModelDescription.ScalarVariable sv =
                                ((Fmi2ModelDescription) md).getScalarVariables().stream().filter(x -> x.name.equals(s)).findFirst().get();
                        rvar = new RelationVariable<>(sv, sv.getName(), instanceLexIdentifier, sv.getValueReference(),
                                new RelationVariable.RelationFmi2Type(sv.getType()));
                    } else if (md instanceof Fmi3ModelDescription) {
                        Fmi3ModelDescription.Fmi3ScalarVariable sv =
                                ((Fmi3ModelDescription) md).getScalarVariables().stream().filter(x -> x.getVariable().getName().equals(s)).findFirst()
                                        .get();
                        rvar = new RelationVariable<>(sv, sv.getVariable().getName(), instanceLexIdentifier,
                                sv.getVariable().getValueReferenceAsLong(),
                                new RelationVariable.RelationFmi3Type(sv.getVariable().getTypeIdentifier()));
                    }


                    variablesToLogForInstance.add(rvar);


                }
                if (this.globalVariablesToLogForInstance.containsKey(instance.instanceName)) {
                    List<RelationVariable> existingRVs = this.globalVariablesToLogForInstance.get(instance.instanceName);
                    for (RelationVariable rv : variablesToLogForInstance) {
                        if (!existingRVs.contains(rv)) {
                            existingRVs.add(rv);
                        }
                    }
                } else {
                    this.globalVariablesToLogForInstance.put(instance.instanceName, variablesToLogForInstance);
                }

            }
        }
        return idToRelations;
    }

    private void buildFmi2Relation(Fmi2ModelDescription md2, ModelConnection.ModelInstance instance, LexIdentifier instanceLexIdentifier,
            Map<LexIdentifier, Set<Relation>> idToRelations, List<ModelConnection> connections,
            List<RelationVariable> globalVariablesToLogForGivenInstance) throws XPathExpressionException, InvocationTargetException, IllegalAccessException, EnvironmentException {


        List<Fmi2ModelDescription.ScalarVariable> instanceOutputScalarVariablesPorts =
                md2.getScalarVariables().stream().filter(x -> x.causality == Fmi2ModelDescription.Causality.Output).collect(Collectors.toList());

        Set<Relation> instanceRelations = idToRelations.computeIfAbsent(instanceLexIdentifier, key -> new HashSet<>());

        for (Fmi2ModelDescription.ScalarVariable outputScalarVariable : instanceOutputScalarVariablesPorts) {
            RelationVariable outputVariable = getOrCreateVariable(
                    new org.intocps.maestro.framework.fmi2.RelationVariable<>(outputScalarVariable, outputScalarVariable.getName(),
                            instanceLexIdentifier), instanceLexIdentifier);

            // dependantInputs are the inputs on which the current output depends on internally
            Map<LexIdentifier, RelationVariable> dependantInputs = new HashMap<>();
            for (Fmi2ModelDescription.ScalarVariable inputScalarVariable : outputScalarVariable.outputDependencies.keySet()) {
                if (inputScalarVariable.causality == Fmi2ModelDescription.Causality.Input) {
                    RelationVariable inputVariable = getOrCreateVariable(
                            new org.intocps.maestro.framework.fmi2.RelationVariable<>(outputScalarVariable, outputScalarVariable.getName(),
                                    instanceLexIdentifier), instanceLexIdentifier);
                    dependantInputs.put(instanceLexIdentifier, inputVariable);
                }
                // TODO: Add relation from each input to the given output?
            }
            if (!dependantInputs.isEmpty()) {
                Relation r = new Relation();
                r.source = outputVariable;
                r.targets = dependantInputs;
                r.direction = Relation.Direction.OutputToInput;
                r.origin = Relation.InternalOrExternal.Internal;
                instanceRelations.add(r);
            }

            // externalInputTargets are the inputs that depend on the current output based on the provided connections.
            List<ModelConnection.Variable> externalInputTargets =
                    connections.stream().filter(conn -> conn.from.instance.equals(instance) && conn.from.variable.equals(outputScalarVariable.name))
                            .map(conn -> conn.to).collect(Collectors.toList());
            if (!externalInputTargets.isEmpty()) {
                // Log the current output as there is an input depending on it.
                globalVariablesToLogForGivenInstance.add(outputVariable);
                // externalInputs are all the external Inputs that depends on the current output
                Map<LexIdentifier, RelationVariable> externalInputs = new HashMap<>();
                for (ModelConnection.Variable modelConnToVar : externalInputTargets) {
                    FrameworkUnitInfo frameworkUnitInfo = instanceNameToInstanceComponentInfo.get(modelConnToVar.instance.instanceName);

                    if (frameworkUnitInfo instanceof ComponentInfo) {


                        Optional<Fmi2ModelDescription.ScalarVariable> toScalarVariable =
                                ((ComponentInfo) frameworkUnitInfo).getModelDescription().getScalarVariables().stream()
                                        .filter(sv -> sv.name.equals(modelConnToVar.variable)).findFirst();
                        if (toScalarVariable.isPresent()) {
                            LexIdentifier inputInstanceLexIdentifier = new LexIdentifier(modelConnToVar.instance.instanceName, null);
                            RelationVariable inputVariable = getOrCreateVariable(
                                    new org.intocps.maestro.framework.fmi2.RelationVariable<>(toScalarVariable.get(),
                                            toScalarVariable.get().getName(), inputInstanceLexIdentifier), inputInstanceLexIdentifier);
                            externalInputs.put(inputInstanceLexIdentifier, inputVariable);

                            //Add relation from the input to the given output
                            Set<Relation> inputInstanceRelations = idToRelations.computeIfAbsent(inputInstanceLexIdentifier, key -> new HashSet<>());
                            Relation r = new Relation();
                            r.source = inputVariable;
                            r.targets = new HashMap<>() {{
                                put(instanceLexIdentifier, outputVariable);
                            }};
                            r.origin = Relation.InternalOrExternal.External;
                            r.direction = Relation.Direction.InputToOutput;
                            inputInstanceRelations.add(r);
                        } else {
                            throw new EnvironmentException(
                                    "Failed to find the scalar variable " + modelConnToVar.variable + " at " + modelConnToVar.instance +
                                            " when building the dependencies tree");
                        }
                    } else {
                        logger.warn("Framework unit is not a component: {}", frameworkUnitInfo.getClass().getName());
                    }
                }

                Relation r = new Relation();
                r.source = outputVariable;
                r.targets = externalInputs;
                r.direction = Relation.Direction.OutputToInput;
                r.origin = Relation.InternalOrExternal.External;
                instanceRelations.add(r);
            }
        }
    }

    private void buildFmi3Relation(Fmi3ModelDescription md2, ModelConnection.ModelInstance instance, LexIdentifier instanceLexIdentifier,
            Map<LexIdentifier, Set<Relation>> idToRelations, List<ModelConnection> connections,
            List<RelationVariable> globalVariablesToLogForGivenInstance) throws EnvironmentException {
        List<Fmi3ModelDescription.Fmi3ScalarVariable> instanceOutputScalarVariablesPorts =
                md2.getScalarVariables().stream().filter(x -> x.getVariable().getCausality() == Fmi3Causality.Output).collect(Collectors.toList());

        Set<Relation> instanceRelations = idToRelations.computeIfAbsent(instanceLexIdentifier, key -> new HashSet<>());

        for (Fmi3ModelDescription.Fmi3ScalarVariable outputScalarVariable : instanceOutputScalarVariablesPorts) {
            RelationVariable outputVariable = getOrCreateVariable(
                    new org.intocps.maestro.framework.fmi2.RelationVariable<>(outputScalarVariable, outputScalarVariable.getVariable().getName(),
                            instanceLexIdentifier), instanceLexIdentifier);

            // dependantInputs are the inputs on which the current output depends on internally
            Map<LexIdentifier, RelationVariable> dependantInputs = new HashMap<>();
            for (Fmi3ModelDescription.Fmi3ScalarVariable inputScalarVariable : outputScalarVariable.getOutputDependencies().keySet()) {
                if (inputScalarVariable.getVariable().getCausality() == Fmi3Causality.Input) {
                    RelationVariable inputVariable = getOrCreateVariable(
                            new org.intocps.maestro.framework.fmi2.RelationVariable<>(inputScalarVariable,
                                    inputScalarVariable.getVariable().getName(), instanceLexIdentifier), instanceLexIdentifier);
                    dependantInputs.put(instanceLexIdentifier, inputVariable);
                }
                // TODO: Add relation from each input to the given output?
            }
            if (!dependantInputs.isEmpty()) {
                Relation r = new Relation();
                r.source = outputVariable;
                r.targets = dependantInputs;
                r.direction = Relation.Direction.OutputToInput;
                r.origin = Relation.InternalOrExternal.Internal;
                instanceRelations.add(r);
            }

            // externalInputTargets are the inputs that depend on the current output based on the provided connections.
            List<ModelConnection.Variable> externalInputTargets = connections.stream()
                    .filter(conn -> conn.from.instance.equals(instance) && conn.from.variable.equals(outputScalarVariable.getVariable().getName()))
                    .map(conn -> conn.to).collect(Collectors.toList());
            if (!externalInputTargets.isEmpty()) {
                // Log the current output as there is an input depending on it.
                globalVariablesToLogForGivenInstance.add(outputVariable);
                // externalInputs are all the external Inputs that depends on the current output
                Map<LexIdentifier, RelationVariable> externalInputs = new HashMap<>();
                for (ModelConnection.Variable modelConnToVar : externalInputTargets) {
                    FrameworkUnitInfo frameworkUnitInfo = instanceNameToInstanceComponentInfo.get(modelConnToVar.instance.instanceName);

                    if (frameworkUnitInfo instanceof InstanceInfo) {


                        Optional<Fmi3ModelDescription.Fmi3ScalarVariable> toScalarVariable =
                                ((InstanceInfo) frameworkUnitInfo).getModelDescription().getScalarVariables().stream()
                                        .filter(sv -> sv.getVariable().getName().equals(modelConnToVar.variable)).findFirst();
                        if (toScalarVariable.isPresent()) {
                            LexIdentifier inputInstanceLexIdentifier = new LexIdentifier(modelConnToVar.instance.instanceName, null);
                            RelationVariable inputVariable = getOrCreateVariable(
                                    new org.intocps.maestro.framework.fmi2.RelationVariable<>(toScalarVariable.get(),
                                            toScalarVariable.get().getVariable().getName(), inputInstanceLexIdentifier), inputInstanceLexIdentifier);
                            externalInputs.put(inputInstanceLexIdentifier, inputVariable);

                            //Add relation from the input to the given output
                            Set<Relation> inputInstanceRelations = idToRelations.computeIfAbsent(inputInstanceLexIdentifier, key -> new HashSet<>());
                            Relation r = new Relation();
                            r.source = inputVariable;
                            r.targets = new HashMap<>() {{
                                put(instanceLexIdentifier, outputVariable);
                            }};
                            r.origin = Relation.InternalOrExternal.External;
                            r.direction = Relation.Direction.InputToOutput;
                            inputInstanceRelations.add(r);
                        } else {
                            throw new EnvironmentException(
                                    "Failed to find the scalar variable " + modelConnToVar.variable + " at " + modelConnToVar.instance +
                                            " when building the dependencies tree");
                        }
                    } else {
                        logger.warn("Framework unit is not a component: {}", frameworkUnitInfo.getClass().getName());
                    }
                }

                Relation r = new Relation();
                r.source = outputVariable;
                r.targets = externalInputs;
                r.direction = Relation.Direction.OutputToInput;
                r.origin = Relation.InternalOrExternal.External;
                instanceRelations.add(r);
            }
        }
    }

    public Map<String, List<String>> getLogLevels() {
        return Collections.unmodifiableMap(this.instanceNameToLogLevels);
    }

    private HashMap<String, ModelDescription> buildFmuKeyToFmuMD(Map<String, URI> fmus, ModelDescriptionResolver resolver) throws Exception {
        HashMap<String, ModelDescription> fmuKeyToFmuWithMD = new HashMap<>();
        for (Map.Entry<String, URI> entry : fmus.entrySet()) {
            String key = entry.getKey();
            URI value = entry.getValue();
            fmuKeyToFmuWithMD.put(key, resolver.apply(key, value));
        }

        return fmuKeyToFmuWithMD;
    }


    RelationVariable getOrCreateVariable(RelationVariable relationVariable, LexIdentifier instanceLexIdentifier) {
        if (variables.containsKey(relationVariable.getName() + instanceLexIdentifier)) {
            return variables.get(relationVariable.getName() + instanceLexIdentifier);
        } else {
            //            RelationVariable variable = new RelationVariable(relationVariable, relationVariable.getName(), instanceLexIdentifier);
            variables.put(relationVariable.getName() + instanceLexIdentifier, relationVariable);
            return relationVariable;
        }
    }


    /**
     * Finds all the relations for the given FMU Component LexIdentifiers
     *
     * @param identifiers FMU Component LexIdentifiers
     * @return
     */
    @Override
    public Set<Relation> getRelations(List<LexIdentifier> identifiers) {

        // All the
        /*
         * use mapping of a.#1 -> b.#4 and produce a relation for these
         *
         * Relation a.#1 Outputs to [b.#4, ....]
         * Relation b.#4 Inputs from [a.#1]
         *
         * not sure if we need to return all permutations
         * */
        //construct relational information with the framework relevant attributes
        /*
         * user of this for stepping will first look for all outputs from here and collect these or directly set or use these outputs + others and
         * then use the relation to set these*/
        return identifiers.stream().filter(id -> variableToRelations.containsKey(id)).map(lexId -> variableToRelations.get(lexId))
                .flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * Finds all the relations for the given FMU Component LexIdentifiers
     *
     * @param identifiers FMU Component LexIdentifiers
     * @return
     */
    @Override
    public Set<Relation> getRelations(LexIdentifier... identifiers) {
        if (identifiers == null) {
            return Collections.emptySet();
        }
        return this.getRelations(Arrays.asList(identifiers));
    }

    @Override
    public Set<Relation> getRelations(String... identifiers) {
        if (identifiers == null) {
            return Collections.emptySet();
        }
        return this.getRelations(Arrays.stream(identifiers).map(MableAstFactory::newLexIdentifier).collect(Collectors.toList()));
    }

    @Override
    public <T extends FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework) {
        return (T) this.instanceNameToInstanceComponentInfo.get(identifier.getText());
    }

    @Override
    public void check(IErrorReporter reporter) throws Exception {

        List<IFmuValidator> validators = Arrays.asList(new MaestroV1FmuValidation());

        Map<String, Boolean> validated = getFmuToUri().stream().collect(
                Collectors.toMap(Map.Entry::getKey, map -> validators.stream().allMatch(v -> v.validate(map.getKey(), map.getValue(), reporter))));

        if (validated.values().stream().anyMatch(v -> !v)) {
            throw new Exception("The following FMUs does not respected the standard: " +
                    validated.entrySet().stream().filter(map -> !map.getValue()).map(Map.Entry::getKey).collect(Collectors.joining(",", "[", "]")));
        }
    }

    public ModelDescription getModelDescription(String name) {
        return this.fmuKeyToModelDescription.get(name);
    }

    public String getFaultInjectionConfigurationPath() {
        return this.faultInjectionConfigurationPath;
    }

    public interface ModelDescriptionResolver extends BiFunction<String, URI, ModelDescription> {
    }

    public static class Relation implements FrameworkVariableInfo, IRelation {
        RelationVariable source;
        InternalOrExternal origin;
        Direction direction;
        Map<LexIdentifier, RelationVariable> targets;

        @Override
        public InternalOrExternal getOrigin() {
            return origin;
        }

        @Override
        public RelationVariable getSource() {
            return source;
        }

        @Override
        public Direction getDirection() {
            return direction;
        }

        @Override
        public Map<LexIdentifier, RelationVariable> getTargets() {
            return targets;
        }

        @Override
        public String toString() {
            return (origin == InternalOrExternal.Internal ? "I" : "E") + " " + source + " " + (direction == Direction.OutputToInput ? "->" : "<-") +
                    " " + targets.entrySet().stream().map(map -> map.getValue().toString()).collect(Collectors.joining(",", "[", "]"));
        }


        public static class RelationBuilder {

            private final RelationVariable source;
            private final Map<LexIdentifier, RelationVariable> targets;
            private InternalOrExternal origin = InternalOrExternal.External;
            private Direction direction = Direction.OutputToInput;

            public RelationBuilder(RelationVariable source, Map<LexIdentifier, RelationVariable> targets) {
                this.source = source;
                this.targets = targets;
            }

            public RelationBuilder setInternalOrExternal(InternalOrExternal origin) {
                this.origin = origin;
                return this;
            }

            public RelationBuilder setDirection(Direction direction) {
                this.direction = direction;
                return this;
            }

            public Relation build() {
                var rel = new Relation();
                rel.source = this.source;
                rel.targets = this.targets;
                rel.origin = this.origin;
                rel.direction = this.direction;
                return rel;
            }
        }
    }

    //    public static class Variable implements IVariable {
    //        public final org.intocps.maestro.framework.core.RelationVariable scalarVariable;
    //
    //        public Variable(org.intocps.maestro.framework.core.RelationVariable scalarVariable) {
    //            this.scalarVariable = scalarVariable;
    //        }
    //
    //        @Override
    //        public org.intocps.maestro.framework.core.RelationVariable getScalarVariable() {
    //            return scalarVariable;
    //        }
    //
    //        <T extends FrameworkVariableInfo> T getFrameworkInfo(Framework framework) {
    //            return (T) scalarVariable;
    //        }
    //
    //        @Override
    //        public String toString() {
    //            return scalarVariable.toString();
    //        }
    //    }


    public static class FileModelDescriptionResolver implements ModelDescriptionResolver {
        static XPath xPath = XPathFactory.newInstance().newXPath();

        double getFmiVersion(InputStream is) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return Double.parseDouble((String) xPath.compile("fmiModelDescription/@fmiVersion").evaluate(doc, XPathConstants.STRING));
        }

        @Override
        public ModelDescription apply(String s, URI uri) {
            try {
                IFmu fmu = FmuFactory.create(null, uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copy(fmu.getModelDescription(), buffer);

                double fmiVersion = getFmiVersion(new ByteArrayInputStream(buffer.toByteArray()));
                if (fmiVersion < 3) {
                    return new ExplicitModelDescription(new ByteArrayInputStream(buffer.toByteArray()));
                } else {
                    return new Fmi3ModelDescription(new ByteArrayInputStream(buffer.toByteArray()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
