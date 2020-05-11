package org.intocps.maestro.plugin.env;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


// The relations provided are related to the FMI Component and not to the individual input/output.
public class UnitRelationship implements ISimulationEnvironment {
    Map<LexIdentifier, Set<Relation>> variableToRelations = new HashMap<>();
    Map<String, ComponentInfo> instanceNameToInstanceComponentInfo = new HashMap<>();
    public UnitRelationship(List<ModelConnection> connections, HashMap<String, ModelDescription> fmuKeyToModelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, EnvironmentException {

        Set<ModelConnection.ModelInstance> instancesFromConnections = new HashSet<>();
        for(ModelConnection.ModelInstance instance : instancesFromConnections){
            instanceNameToInstanceComponentInfo.put(instance.instanceName, new ComponentInfo(fmuKeyToModelDescription.get(instance.key)));
        }

        for(ModelConnection.ModelInstance instance : instancesFromConnections )
        {
            LexIdentifier instanceLexIdentifier = new LexIdentifier(instance.instanceName, null);
            Set<Relation> instanceRelations = getOrCrateRelationsForLexIdentifier(instanceLexIdentifier);

            List<ModelDescription.ScalarVariable> instanceOutputScalarVariablesPorts =
                    instanceNameToInstanceComponentInfo.get(instance.key).modelDescription.getScalarVariables().stream()
                            .filter(x -> x.causality == ModelDescription.Causality.Output).collect(Collectors.toList());

            for(ModelDescription.ScalarVariable outputScalarVariable : instanceOutputScalarVariablesPorts)
            {
                Variable outputVariable = new Variable(new RelationVariable(outputScalarVariable, instanceLexIdentifier));

                // dependantInputs are the inputs on which the current output depends on internally
                Map<LexIdentifier, Variable> dependantInputs = new HashMap<>();
                for(ModelDescription.ScalarVariable inputScalarVariable : outputScalarVariable.outputDependencies.keySet()){
                    Variable inputVariable = new Variable(new RelationVariable(inputScalarVariable, instanceLexIdentifier));
                    dependantInputs.put(instanceLexIdentifier, inputVariable);
                    // TODO: Add relation from each input to the given output?
                }
                if(dependantInputs.size() != 0){
                    Relation r = new Relation();
                    r.source = outputVariable;
                    r.targets = dependantInputs;
                    r.direction = Relation.Direction.OutputToInput;
                    r.internalOrExternal = Relation.InternalOrExternal.Internal;
                    instanceRelations.add(r);
                }

                // externalInputTargets are the inputs that depends on the current output based on the provided connections.
                List<ModelConnection.Variable> externalInputTargets = connections.stream().filter(conn -> conn.from.instance.equals(instance) && conn.from.variable.equals(outputScalarVariable.name)).map(conn -> conn.to).collect(Collectors.toList());
                if(externalInputTargets.size() != 0)
                {
                    // externalInputs are all the external Inputs that depends on the current output
                    Map<LexIdentifier, Variable> externalInputs = new HashMap<>();
                    for(ModelConnection.Variable modelConnToVar : externalInputTargets)
                    {
                        ModelDescription md = instanceNameToInstanceComponentInfo.get(modelConnToVar.instance.key).modelDescription;
                        Optional<ModelDescription.ScalarVariable> toScalarVariable = md.getScalarVariables().stream().filter(sv -> sv.name.equals(modelConnToVar.variable)).findFirst();
                        if(toScalarVariable.isPresent())
                        {
                            LexIdentifier inputInstanceLexIdentifier = new LexIdentifier(modelConnToVar.instance.instanceName, null);
                            Variable inputVariable = new Variable(new RelationVariable(toScalarVariable.get(), inputInstanceLexIdentifier));
                            externalInputs.put(inputInstanceLexIdentifier, inputVariable);

                            //Add relation from the input to the given output
                            Set<Relation> inputInstanceRelations = getOrCrateRelationsForLexIdentifier(instanceLexIdentifier);
                            Relation r = new Relation();
                            r.source = inputVariable;
                            r.targets = new HashMap<LexIdentifier, Variable>(){{
                                put(inputInstanceLexIdentifier, inputVariable);
                            }};
                            r.internalOrExternal = Relation.InternalOrExternal.External;
                            r.direction = Relation.Direction.InputToOutput;
                            inputInstanceRelations.add(r);
                        }
                        {
                            throw new EnvironmentException("Failed to find the scalar variable " + modelConnToVar.variable + " at " + modelConnToVar.instance + " when building the dependencies tree");
                        }
                    }

                    Relation r = new Relation();
                    r.source = outputVariable;
                    r.targets = externalInputs;
                    r.direction = Relation.Direction.OutputToInput;
                    r.internalOrExternal = Relation.InternalOrExternal.External;
                    instanceRelations.add(r);
                }
            }
        }
    }

    Set<Relation> getOrCrateRelationsForLexIdentifier(LexIdentifier instanceLexIdentifier){
        if(variableToRelations.containsKey(instanceLexIdentifier))
        {
            return variableToRelations.get(instanceLexIdentifier);
        }
        else {
            Set<Relation> relations = new HashSet<>();
            variableToRelations.put(instanceLexIdentifier, relations);
            return relations;
        }
    }

    /**
     * Finds all the relations for the given FMU Component LexIdentifiers
     * @param identifiers FMU Component LexIdentifiers
     * @return
     */
    @Override
    public Set<UnitRelationship.Relation> getRelations(List<LexIdentifier> identifiers) {

        // a, b

        Map<LexIdentifier, Object> internalMapping = identifiers.stream().collect(Collectors.toMap(Function.identity(), id -> {

            //check context to find which FMU in the internal map that id points to in terms of FMU.modelinstance
            return null;

        }));

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
        Set<Relation> returnValues = identifiers.stream().map(lexId -> variableToRelations.get(lexId.getText())).flatMap(x -> x.stream()).collect(Collectors.toSet());
        return returnValues;
    }

    /**
     * Finds all the relations for the given FMU Component LexIdentifiers
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

    public <T extends FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework) {
        return (T) this.instanceNameToInstanceComponentInfo.get(identifier.getText());
    }

    public interface FrameworkVariableInfo {
    }

    public interface FrameworkUnitInfo {
    }

    public static class Relation {

        Variable source;

        InternalOrExternal internalOrExternal;

        Direction direction;

        Map<LexIdentifier, Variable> targets;

        enum InternalOrExternal{
            Internal,
            External
        }

        enum Direction {
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
    }


}
