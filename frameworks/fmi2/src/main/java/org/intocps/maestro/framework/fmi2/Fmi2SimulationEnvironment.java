package org.intocps.maestro.framework.fmi2;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.newLexIdentifier;

public class Fmi2SimulationEnvironment implements ISimulationEnvironment {
    final static Logger logger = LoggerFactory.getLogger(Fmi2SimulationEnvironment.class);
    private final Map<String, String> instanceLexToInstanceName = new HashMap<>();
    private final Map<String, List<String>> instanceNameToLogLevels = new HashMap<>();
    Map<LexIdentifier, Set<Relation>> variableToRelations = new HashMap<>();
    Map<String, ComponentInfo> instanceNameToInstanceComponentInfo = new HashMap<>();
    HashMap<String, Fmi2ModelDescription> fmuKeyToModelDescription = new HashMap<>();
    Map<String, URI> fmuToUri = null;
    Map<String, Variable> variables = new HashMap<>();
    Map<String, List<org.intocps.maestro.framework.fmi2.RelationVariable>> globalVariablesToLogForInstance = new HashMap<>();
    private String faultInjectionConfigurationPath;

    protected Fmi2SimulationEnvironment(Fmi2SimulationEnvironmentConfiguration msg) throws Exception {
        initialize(msg);
    }

    public static Fmi2SimulationEnvironment of(File file, IErrorReporter reporter) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return of(is, reporter);
        }
    }

    public static Fmi2SimulationEnvironment of(Fmi2SimulationEnvironmentConfiguration msg, IErrorReporter reporter) throws Exception {
        return new Fmi2SimulationEnvironment(msg);
    }

    public static Fmi2SimulationEnvironment of(InputStream inputStream, IErrorReporter reporter) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Fmi2SimulationEnvironmentConfiguration msg = null;
        msg = mapper.readValue(inputStream, Fmi2SimulationEnvironmentConfiguration.class);
        return of(msg, reporter);
    }

    public static List<ModelConnection> buildConnections(Map<String, List<String>> connections) throws Exception {
        List<ModelConnection> list = new Vector<>();

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
        Map<String, List<org.intocps.maestro.framework.fmi2.RelationVariable>> t = variablesToLogMap.entrySet().stream().collect(Collectors
                .toMap(entry -> extractInstance.apply(entry.getKey()),
                        entry -> globalVariablesToLogForInstance.get(extractInstance.apply(entry.getKey())).stream()
                                .filter(x -> entry.getValue().contains(x.scalarVariable.name)).collect(Collectors.toList())));
        return t;

    }

    @Override
    public List<RelationVariable> getConnectedOutputs() {
        return getInstances().stream().flatMap(instance -> {
            Stream<RelationVariable> relationOutputs = this.getRelations(new LexIdentifier(instance.getKey(), null)).stream()
                    .filter(relation -> (relation.getOrigin() == Relation.InternalOrExternal.External) &&
                            (relation.getDirection() == Relation.Direction.OutputToInput)).map(x -> x.getSource().scalarVariable);
            return relationOutputs;
        }).collect(Collectors.toList());

    }

    public void setLexNameToInstanceNameMapping(String lexName, String instanceName) {
        this.instanceLexToInstanceName.put(lexName, instanceName);
    }

    public ComponentInfo getInstanceByLexName(String lexName) {
        if(!this.instanceNameToInstanceComponentInfo.containsKey(lexName)){
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
    public List<org.intocps.maestro.framework.fmi2.RelationVariable> getVariablesToLog(String instanceName) {
        List<org.intocps.maestro.framework.fmi2.RelationVariable> vars = this.globalVariablesToLogForInstance.get(instanceName);
        if (vars == null) {
            return new ArrayList<>();
        } else {
            return vars;
        }
    }

    public Set<Map.Entry<String, Fmi2ModelDescription>> getFmusWithModelDescriptions() {
        return this.fmuKeyToModelDescription.entrySet();
    }

    @Override
    public Set<Map.Entry<String, ComponentInfo>> getInstances() {
        return this.instanceNameToInstanceComponentInfo.entrySet();
    }

    public Set<Map.Entry<String, URI>> getFmuToUri() {
        return this.fmuToUri.entrySet();
    }

    public URI getUriFromFMUName(String fmuName) {
        return this.fmuToUri.get(fmuName);
    }

    private void initialize(Fmi2SimulationEnvironmentConfiguration msg) throws Exception {
        // Remove { } around fmu name.
        Map<String, URI> fmuToURI = msg.getFmuFiles();

        // Build map from fmuKey to ModelDescription
        this.fmuToUri = fmuToURI;
        List<ModelConnection> connections = buildConnections(msg.connections);
        HashMap<String, Fmi2ModelDescription> fmuKeyToModelDescription = buildFmuKeyToFmuMD(fmuToURI);
        this.fmuKeyToModelDescription = fmuKeyToModelDescription;

        if (msg.faultInjectConfigurationPath != null && msg.faultInjectConfigurationPath.length() > 0) {
            if ((new File(msg.faultInjectConfigurationPath).exists())) {
                this.faultInjectionConfigurationPath = msg.faultInjectConfigurationPath;
            } else {
                throw new EnvironmentException("Failed to find the fault injection configuration file: " + msg.faultInjectConfigurationPath);
            }
        }

        // Build map from InstanceName to InstanceComponentInfo
        Set<ModelConnection.ModelInstance> instancesFromConnections = new HashSet<>();
        for (ModelConnection instance : connections) {
            instancesFromConnections.add(instance.from.instance);
            instancesFromConnections.add(instance.to.instance);
            if (!instanceNameToInstanceComponentInfo.containsKey(instance.from.instance.instanceName)) {
                ComponentInfo instanceComponentInfo =
                        new ComponentInfo(fmuKeyToModelDescription.get(instance.from.instance.key), instance.from.instance.key);
                if (msg.faultInjectInstances != null && msg.faultInjectInstances.containsKey(instance.from.instance.instanceName)) {
                    instanceComponentInfo.setFaultInject(msg.faultInjectInstances.get(instance.from.instance.instanceName));
                }
                instanceNameToInstanceComponentInfo.put(instance.from.instance.instanceName, instanceComponentInfo);
            }
            if (!instanceNameToInstanceComponentInfo.containsKey(instance.to.instance.instanceName)) {
                ComponentInfo instanceComponentInfo =
                        new ComponentInfo(fmuKeyToModelDescription.get(instance.to.instance.key), instance.to.instance.key);
                if (msg.faultInjectInstances != null && msg.faultInjectInstances.containsKey(instance.to.instance.instanceName)) {
                    instanceComponentInfo.setFaultInject(msg.faultInjectInstances.get(instance.to.instance.instanceName));
                }
                instanceNameToInstanceComponentInfo.put(instance.to.instance.instanceName, instanceComponentInfo);
            }
        }


        // Build relations
        for (ModelConnection.ModelInstance instance : instancesFromConnections) {
            LexIdentifier instanceLexIdentifier = new LexIdentifier(instance.instanceName, null);
            Set<Relation> instanceRelations = getOrCreateRelationsForLexIdentifier(instanceLexIdentifier);

            List<Fmi2ModelDescription.ScalarVariable> instanceOutputScalarVariablesPorts =
                    instanceNameToInstanceComponentInfo.get(instance.instanceName).modelDescription.getScalarVariables().stream()
                            .filter(x -> x.causality == Fmi2ModelDescription.Causality.Output).collect(Collectors.toList());

            // Add the instance to the globalVariablesToLogForInstance map.
            ArrayList<org.intocps.maestro.framework.fmi2.RelationVariable> globalVariablesToLogForGivenInstance = new ArrayList<>();
            this.globalVariablesToLogForInstance.putIfAbsent(instance.instanceName, globalVariablesToLogForGivenInstance);

            for (Fmi2ModelDescription.ScalarVariable outputScalarVariable : instanceOutputScalarVariablesPorts) {
                Variable outputVariable = getOrCreateVariable(outputScalarVariable, instanceLexIdentifier);

                // dependantInputs are the inputs on which the current output depends on internally
                Map<LexIdentifier, Variable> dependantInputs = new HashMap<>();
                for (Fmi2ModelDescription.ScalarVariable inputScalarVariable : outputScalarVariable.outputDependencies.keySet()) {
                    if (inputScalarVariable.causality == Fmi2ModelDescription.Causality.Input) {
                        Variable inputVariable = getOrCreateVariable(inputScalarVariable, instanceLexIdentifier);
                        dependantInputs.put(instanceLexIdentifier, inputVariable);
                    }
                    // TODO: Add relation from each input to the given output?
                }
                if (dependantInputs.size() != 0) {
                    Relation r = new Relation();
                    r.source = outputVariable;
                    r.targets = dependantInputs;
                    r.direction = Relation.Direction.OutputToInput;
                    r.origin = Relation.InternalOrExternal.Internal;
                    instanceRelations.add(r);
                }

                // externalInputTargets are the inputs that depend on the current output based on the provided connections.
                List<ModelConnection.Variable> externalInputTargets = connections.stream()
                        .filter(conn -> conn.from.instance.equals(instance) && conn.from.variable.equals(outputScalarVariable.name))
                        .map(conn -> conn.to).collect(Collectors.toList());
                if (externalInputTargets.size() != 0) {
                    // Log the current output as there is an input depending on it.
                    globalVariablesToLogForGivenInstance.add(outputVariable.scalarVariable);
                    // externalInputs are all the external Inputs that depends on the current output
                    Map<LexIdentifier, Variable> externalInputs = new HashMap<>();
                    for (ModelConnection.Variable modelConnToVar : externalInputTargets) {
                        Fmi2ModelDescription md = instanceNameToInstanceComponentInfo.get(modelConnToVar.instance.instanceName).modelDescription;
                        Optional<Fmi2ModelDescription.ScalarVariable> toScalarVariable =
                                md.getScalarVariables().stream().filter(sv -> sv.name.equals(modelConnToVar.variable)).findFirst();
                        if (toScalarVariable.isPresent()) {
                            LexIdentifier inputInstanceLexIdentifier = new LexIdentifier(modelConnToVar.instance.instanceName, null);
                            Variable inputVariable = getOrCreateVariable(toScalarVariable.get(), inputInstanceLexIdentifier);
                            externalInputs.put(inputInstanceLexIdentifier, inputVariable);

                            //Add relation from the input to the given output
                            Set<Relation> inputInstanceRelations = getOrCreateRelationsForLexIdentifier(inputInstanceLexIdentifier);
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
                    }

                    Relation r = new Relation();
                    r.source = outputVariable;
                    r.targets = externalInputs;
                    r.direction = Relation.Direction.OutputToInput;
                    r.origin = Relation.InternalOrExternal.External;
                    instanceRelations.add(r);
                }
            }

            // Create a globalLogVariablesMap that is a merge between connected outputs, logVariables and livestream.
            HashMap<String, List<String>> globalLogVariablesMaps = new HashMap<>();
            if (msg.logVariables != null) {
                globalLogVariablesMaps.putAll(msg.logVariables);
            }
            if (msg.livestream != null) {
                msg.livestream.forEach((k, v) -> globalLogVariablesMaps.merge(k, v, (v1, v2) -> {
                    Set<String> set = new TreeSet<>(v1);
                    set.addAll(v2);
                    return new ArrayList<>(set);
                }));
            }


            List<org.intocps.maestro.framework.fmi2.RelationVariable> variablesToLogForInstance = new ArrayList<>();
            String logVariablesKey = instance.key + "." + instance.instanceName;
            if (globalLogVariablesMaps.containsKey(logVariablesKey)) {
                for (String s : globalLogVariablesMaps.get(logVariablesKey)) {
                    variablesToLogForInstance.add(new org.intocps.maestro.framework.fmi2.RelationVariable(
                            this.fmuKeyToModelDescription.get(instance.key).getScalarVariables().stream().filter(x -> x.name.equals(s)).findFirst()
                                    .get(), instanceLexIdentifier));


                }
                if (this.globalVariablesToLogForInstance.containsKey(instance.instanceName)) {
                    List<org.intocps.maestro.framework.fmi2.RelationVariable> existingRVs =
                            this.globalVariablesToLogForInstance.get(instance.instanceName);
                    for (org.intocps.maestro.framework.fmi2.RelationVariable rv : variablesToLogForInstance) {
                        if (existingRVs.contains(rv) == false) {
                            existingRVs.add(rv);
                        }
                    }
                } else {
                    this.globalVariablesToLogForInstance.put(instance.instanceName, variablesToLogForInstance);
                }

            }
        }
    }

    public Map<String, List<String>> getLogLevels() {
        return Collections.unmodifiableMap(this.instanceNameToLogLevels);
    }

    private HashMap<String, Fmi2ModelDescription> buildFmuKeyToFmuMD(Map<String, URI> fmus) throws Exception {
        HashMap<String, Fmi2ModelDescription> fmuKeyToFmuWithMD = new HashMap<>();
        for (Map.Entry<String, URI> entry : fmus.entrySet()) {
            String key = entry.getKey();
            URI value = entry.getValue();
            IFmu fmu = FmuFactory.create(null, value);
            Fmi2ModelDescription md = new ExplicitModelDescription(fmu.getModelDescription());
            fmuKeyToFmuWithMD.put(key, md);
        }

        return fmuKeyToFmuWithMD;
    }

    Variable getOrCreateVariable(Fmi2ModelDescription.ScalarVariable inputScalarVariable, LexIdentifier instanceLexIdentifier) {
        if (variables.containsKey(inputScalarVariable.name + instanceLexIdentifier)) {
            return variables.get(inputScalarVariable.name + instanceLexIdentifier);
        } else {
            Variable variable = new Variable(new org.intocps.maestro.framework.fmi2.RelationVariable(inputScalarVariable, instanceLexIdentifier));
            variables.put(inputScalarVariable.name + instanceLexIdentifier, variable);
            return variable;
        }
    }

    Set<Relation> getOrCreateRelationsForLexIdentifier(LexIdentifier instanceLexIdentifier) {
        if (variableToRelations.containsKey(instanceLexIdentifier)) {
            return variableToRelations.get(instanceLexIdentifier);
        } else {
            Set<Relation> relations = new HashSet<>();
            variableToRelations.put(instanceLexIdentifier, relations);
            return relations;
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
        return this.getRelations(Arrays.stream(identifiers).map(x -> newLexIdentifier(x)).collect(Collectors.toList()));
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

    public Fmi2ModelDescription getModelDescription(String name) {
        return this.fmuKeyToModelDescription.get(name);
    }

    public String getFaultInjectionConfigurationPath() {
        return this.faultInjectionConfigurationPath;
    }

    public static class Relation implements FrameworkVariableInfo, IRelation {
        Variable source;
        InternalOrExternal origin;
        Direction direction;
        Map<LexIdentifier, Variable> targets;

        @Override
        public InternalOrExternal getOrigin() {
            return origin;
        }

        @Override
        public Variable getSource() {
            return source;
        }

        @Override
        public Direction getDirection() {
            return direction;
        }

        @Override
        public Map<LexIdentifier, Variable> getTargets() {
            return targets;
        }

        @Override
        public String toString() {
            return (origin == InternalOrExternal.Internal ? "I" : "E") + " " + source + " " + (direction == Direction.OutputToInput ? "->" : "<-") +
                    " " + targets.entrySet().stream().map(map -> map.getValue().toString()).collect(Collectors.joining(",", "[", "]"));
        }


        public static class RelationBuilder {

            private final Variable source;
            private final Map<LexIdentifier, Variable> targets;
            private InternalOrExternal origin = InternalOrExternal.External;
            private Direction direction = Direction.OutputToInput;

            public RelationBuilder(Variable source, Map<LexIdentifier, Variable> targets) {
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

    public static class Variable implements IVariable {
        public final org.intocps.maestro.framework.fmi2.RelationVariable scalarVariable;

        public Variable(org.intocps.maestro.framework.fmi2.RelationVariable scalarVariable) {
            this.scalarVariable = scalarVariable;
        }

        @Override
        public org.intocps.maestro.framework.fmi2.RelationVariable getScalarVariable() {
            return scalarVariable;
        }

        <T extends FrameworkVariableInfo> T getFrameworkInfo(Framework framework) {
            return (T) scalarVariable;
        }

        @Override
        public String toString() {
            return scalarVariable.toString();
        }
    }
}
