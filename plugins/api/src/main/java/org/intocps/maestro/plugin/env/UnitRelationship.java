package org.intocps.maestro.plugin.env;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


// The relations provided are related to the FMI Component and not to the individual input/output.
public class UnitRelationship implements ISimulationEnvironment {
    Map<LexIdentifier, Set<Relation>> variableToRelations = new HashMap<>();
    Map<String, ComponentInfo> instanceNameToInstanceComponentInfo = new HashMap<>();

    public UnitRelationship(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        EnvironmentMessage msg = null;
        try {
            msg = mapper.readValue(is, EnvironmentMessage.class);
            Map<String, URI> fmuToURI = msg.getFmuFiles();
            List<ModelConnection> connections = buildConnections(msg.connections);
            HashMap<String, ModelDescription> fmuKeyToModelDescription = buildFmuKeyToFmuMD(fmuToURI);

            Set<ModelConnection.ModelInstance> instancesFromConnections = new HashSet<>();
            for (ModelConnection instance : connections) {
                instancesFromConnections.add(instance.from.instance);
                instancesFromConnections.add(instance.to.instance);
                if (!instanceNameToInstanceComponentInfo.containsKey(instance.from.instance.instanceName)) {
                    instanceNameToInstanceComponentInfo
                            .put(instance.from.instance.instanceName, new ComponentInfo(fmuKeyToModelDescription.get(instance.from.instance.key)));
                }
                if (!instanceNameToInstanceComponentInfo.containsKey(instance.to.instance.instanceName)) {
                    instanceNameToInstanceComponentInfo
                            .put(instance.to.instance.instanceName, new ComponentInfo(fmuKeyToModelDescription.get(instance.to.instance.key)));
                }
            }

            for (ModelConnection.ModelInstance instance : instancesFromConnections) {
                LexIdentifier instanceLexIdentifier = new LexIdentifier(instance.instanceName, null);
                Set<Relation> instanceRelations = getOrCrateRelationsForLexIdentifier(instanceLexIdentifier);

                List<ModelDescription.ScalarVariable> instanceOutputScalarVariablesPorts = instanceNameToInstanceComponentInfo
                        .get(instance.instanceName).modelDescription.getScalarVariables().stream()
                        .filter(x -> x.causality == ModelDescription.Causality.Output).collect(Collectors.toList());

                for (ModelDescription.ScalarVariable outputScalarVariable : instanceOutputScalarVariablesPorts) {
                    Variable outputVariable = new Variable(new RelationVariable(outputScalarVariable, instanceLexIdentifier));

                    // dependantInputs are the inputs on which the current output depends on internally
                    Map<LexIdentifier, Variable> dependantInputs = new HashMap<>();
                    for (ModelDescription.ScalarVariable inputScalarVariable : outputScalarVariable.outputDependencies.keySet()) {
                        Variable inputVariable = new Variable(new RelationVariable(inputScalarVariable, instanceLexIdentifier));
                        dependantInputs.put(instanceLexIdentifier, inputVariable);
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

                    // externalInputTargets are the inputs that depends on the current output based on the provided connections.
                    List<ModelConnection.Variable> externalInputTargets = connections.stream()
                            .filter(conn -> conn.from.instance.equals(instance) && conn.from.variable.equals(outputScalarVariable.name))
                            .map(conn -> conn.to).collect(Collectors.toList());
                    if (externalInputTargets.size() != 0) {
                        // externalInputs are all the external Inputs that depends on the current output
                        Map<LexIdentifier, Variable> externalInputs = new HashMap<>();
                        for (ModelConnection.Variable modelConnToVar : externalInputTargets) {
                            ModelDescription md = instanceNameToInstanceComponentInfo.get(modelConnToVar.instance.instanceName).modelDescription;
                            Optional<ModelDescription.ScalarVariable> toScalarVariable = md.getScalarVariables().stream()
                                    .filter(sv -> sv.name.equals(modelConnToVar.variable)).findFirst();
                            if (toScalarVariable.isPresent()) {
                                LexIdentifier inputInstanceLexIdentifier = new LexIdentifier(modelConnToVar.instance.instanceName, null);
                                Variable inputVariable = new Variable(new RelationVariable(toScalarVariable.get(), inputInstanceLexIdentifier));
                                externalInputs.put(inputInstanceLexIdentifier, inputVariable);

                                //Add relation from the input to the given output
                                Set<Relation> inputInstanceRelations = getOrCrateRelationsForLexIdentifier(inputInstanceLexIdentifier);
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
                                        "Failed to find the scalar variable " + modelConnToVar.variable + " at " + modelConnToVar.instance + " when building the dependencies tree");
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static UnitRelationship of(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return new UnitRelationship(is);
        }
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

    private HashMap<String, ModelDescription> buildFmuKeyToFmuMD(Map<String, URI> fmus) throws Exception {
        HashMap<String, ModelDescription> fmuKeyToFmuWithMD = new HashMap<>();
        for (Map.Entry<String, URI> entry : fmus.entrySet()) {
            String key = entry.getKey();
            URI value = entry.getValue();
            IFmu fmu = FmuFactory.create(null, value);
            ModelDescription md = new ModelDescription(fmu.getModelDescription());
            fmu.unLoad();
            fmuKeyToFmuWithMD.put(key, md);
        }

        return fmuKeyToFmuWithMD;
    }

    Set<Relation> getOrCrateRelationsForLexIdentifier(LexIdentifier instanceLexIdentifier) {
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
    public Set<UnitRelationship.Relation> getRelations(List<LexIdentifier> identifiers) {

        // a, b

        //        Map<LexIdentifier, Object> internalMapping = identifiers.stream().collect(Collectors.toMap(Function.identity(), id -> {

        //check context to find which FMU in the internal map that id points to in terms of FMU.modelinstance
        //            return null;

        //        }));

        //a -> FMUA, b-> FMUB

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
    public <T extends FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework) {
        return (T) this.instanceNameToInstanceComponentInfo.get(identifier.getText());
    }

    public interface FrameworkVariableInfo {
    }

    public interface FrameworkUnitInfo {
    }

    public static class Relation {

        Variable source;
        InternalOrExternal origin;
        Direction direction;
        Map<LexIdentifier, Variable> targets;

        public InternalOrExternal getOrigin() {
            return origin;
        }

        public Variable getSource() {
            return source;
        }

        public Direction getDirection() {
            return direction;
        }

        public Map<LexIdentifier, Variable> getTargets() {
            return targets;
        }

        @Override
        public String toString() {
            return (origin == InternalOrExternal.Internal ? "I" : "E") + " " + source + " " + (direction == Direction.OutputToInput ? "->" : "<-") + " " + targets
                    .entrySet().stream().map(map -> map.getValue().toString()).collect(Collectors.joining(",", "[", "]"));
        }

        public enum InternalOrExternal {
            Internal,
            External
        }

        public enum Direction {
            OutputToInput,
            InputToOutput
        }
    }

    public class Variable {
        public final RelationVariable scalarVariable;

        public Variable(RelationVariable scalarVariable) {
            this.scalarVariable = scalarVariable;
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
