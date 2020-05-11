package org.intocps.maestro.multimodelparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.topologicalsorting.TarjanGraph;
import org.intocps.topologicalsorting.data.DependencyResult;
import org.intocps.topologicalsorting.data.Edge11;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import scala.jdk.javaapi.CollectionConverters;

public class MultiModelParser {
    final ObjectMapper mapper = new ObjectMapper();

    public MultiModelMessage ParseMultiModel(InputStream is) throws Exception {
        MultiModelMessage msg = null;
        try {
            msg = mapper.readValue(is, MultiModelMessage.class);
            Map<String, URI> fmuToURI = msg.getFmuFiles();
            List<ModelConnection> connections = buildConnections(msg.connections);
            //List<ModelParameter> parameters = buildParameters(msg.parameters);
            HashMap<String, ModelDescription> fmuKeyToModelDescription = buildFmuKeyToFmuMD(fmuToURI);
            HashMap<String, Integer> fmusToInstancesCount = fmusToInstancesCount(msg.fmus.keySet(), connections);
            OutputsAndInputsToOutputs outputsAndInputToOutputs = constructOutputsAndInputToOutputMap(connections, fmuKeyToModelDescription);
            Set<ModelConnection.ModelInstance> allInstances = Stream.concat(
                    outputsAndInputToOutputs.inputToOutputs.keySet().stream(), outputsAndInputToOutputs.outputs.keySet().stream())
                    .collect(Collectors.toSet());

            DependencyResult<Port> topologicalSorting = performTopologicalSorting(outputsAndInputToOutputs.inputToOutputs, allInstances, fmuKeyToModelDescription);
            DirectedGraph<Port, LabelledEdge> topologicalSorting2 = performLegacyTopologicalSorting(outputsAndInputToOutputs.inputToOutputs, allInstances, fmuKeyToModelDescription);
            CycleDetector<Port, LabelledEdge> cycleDetector = new CycleDetector<>(topologicalSorting2);
            if(cycleDetector.detectCycles())
            {
                System.out.println("Detected cycle: " + cycleDetector.findCycles());
            }
            System.out.println("FInished parsing");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }

    /**
     * Calculates the number of instances per FMU
     * @param fmuKeys
     * @param connections
     * @return
     */
    private static HashMap<String, Integer> fmusToInstancesCount(Set<String> fmuKeys, List<ModelConnection> connections) {
        HashMap<String, Integer> fmusToInstancesCount = new HashMap<>();
        for(String key : fmuKeys) {
            Integer instancesCountForKey = connections.stream().filter(conn -> conn.from.instance.key == key || conn.to.instance.key == key).map(conn -> {
                Set<String> instances = new HashSet<>();
                if (conn.from.instance.key == key ){
                    instances.add(conn.from.instance.instanceName);
                }
                if (conn.to.instance.key == key ){
                    instances.add(conn.to.instance.instanceName);
                }
                return instances;
            }).collect(Collectors.toSet()).size();
            fmusToInstancesCount.put(key, instancesCountForKey);
        }
        return fmusToInstancesCount;
    }

    public static List<ModelParameter> buildParameters(Map<String, Object> parameters) throws InvalidVariableStringException {
        List<ModelParameter> list = new Vector<>();

        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                list.add(new ModelParameter(ModelConnection.Variable.parse(entry.getKey()), entry.getValue()));
            }
        }
        return list;
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
        for(Map.Entry<String, URI> entry : fmus.entrySet()) {
            String key = entry.getKey();
            URI value = entry.getValue();
            IFmu fmu = FmuFactory.create(null,value);
            ModelDescription md = new ModelDescription(fmu.getModelDescription());
            fmuKeyToFmuWithMD.put(key, md);
        }

        return fmuKeyToFmuWithMD;
    }

    private static DependencyResult<Port> performTopologicalSorting(
            HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputToOutputs,
            Set<ModelConnection.ModelInstance> allInstances,
            HashMap<String, ModelDescription> fmuKeyToModelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {

        List<Edge11<Port, EdgeType>> edges = new ArrayList<>();

        // Create all internal edges
        for(ModelConnection.ModelInstance instance : allInstances )
        {
            List<ModelDescription.ScalarVariable> allOutputPorts = fmuKeyToModelDescription.get(instance.key).
                    getScalarVariables().stream().filter(x -> x.causality == ModelDescription.Causality.Output).collect(Collectors.toList());

            for(ModelDescription.ScalarVariable outputScalarVariable : allOutputPorts)
            {
                Port outputPort = new Port(instance, outputScalarVariable);
                for(ModelDescription.ScalarVariable internalInputDependencyForOutput : outputScalarVariable.outputDependencies.keySet()) {
                    Port inputPort = new Port(instance, internalInputDependencyForOutput);
                    EdgeType type = EdgeType.InternalDependency;
                    edges.add(new Edge11<>(inputPort, outputPort, type));
                }
            }
        }

        // Create all external edges
        for (Map.Entry<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputEntryToOutputs : inputToOutputs.entrySet())
        {
            for(Map.Entry<ModelDescription.ScalarVariable, InstanceScalarVariable> inputScalarVariableToOutput : inputEntryToOutputs.getValue().entrySet()) {
                Port inputPort = new Port(inputEntryToOutputs.getKey(), inputScalarVariableToOutput.getKey());
                Port outputPort = new Port(inputScalarVariableToOutput.getValue().modelInstance, inputScalarVariableToOutput.getValue().scalarVariable);
                edges.add(new Edge11<>(outputPort, inputPort, EdgeType.ExternalLink));
            }
        }

        TarjanGraph<Port, EdgeType> graph = new TarjanGraph<Port, EdgeType>(CollectionConverters.asScala(edges));
        DependencyResult<Port> result = graph.topologicalSort();
        return result;
    }

    private static DirectedGraph<Port, LabelledEdge> performLegacyTopologicalSorting(
            HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputToOutputs,
            Set<ModelConnection.ModelInstance> allInstances,
            HashMap<String, ModelDescription> fmuKeyToModelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {

        DirectedGraph<Port, LabelledEdge> g = new DefaultDirectedGraph<Port, LabelledEdge>(LabelledEdge.class);



        List<Edge11<Port, EdgeType>> edges = new ArrayList<>();

        // Create all internal edges
        for(ModelConnection.ModelInstance instance : allInstances )
        {
            List<ModelDescription.ScalarVariable> allOutputPorts = fmuKeyToModelDescription.get(instance.key).
                    getScalarVariables().stream().filter(x -> x.causality == ModelDescription.Causality.Output).collect(Collectors.toList());

            for(ModelDescription.ScalarVariable outputScalarVariable : allOutputPorts)
            {

                Port outputPort = new Port(instance, outputScalarVariable);
                g.addVertex(outputPort);
                for(ModelDescription.ScalarVariable internalInputDependencyForOutput : outputScalarVariable.outputDependencies.keySet()) {
                    Port inputPort = new Port(instance, internalInputDependencyForOutput);
                    EdgeType type = EdgeType.InternalDependency;
                    g.addVertex(inputPort);
                    g.addEdge(outputPort, inputPort, new LabelledEdge(type));
                }
            }
        }

        // Create all external edges
        for (Map.Entry<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputEntryToOutputs : inputToOutputs.entrySet())
        {
            for(Map.Entry<ModelDescription.ScalarVariable, InstanceScalarVariable> inputScalarVariableToOutput : inputEntryToOutputs.getValue().entrySet()) {
                Port inputPort = new Port(inputEntryToOutputs.getKey(), inputScalarVariableToOutput.getKey());
                Port outputPort = new Port(inputScalarVariableToOutput.getValue().modelInstance, inputScalarVariableToOutput.getValue().scalarVariable);
                g.addVertex(inputPort);
                g.addVertex(outputPort);
                g.addEdge(outputPort, inputPort, new LabelledEdge(EdgeType.ExternalLink));
            }
        }


        return g;
    }



    /**
     * Constructs a Map from FMI Input ModelInstance to Map from Input Scalar Variable to related Output InstanceScalarVariable.
     * Thus, it creates a map from an input to its corresponding output.
     * @param connections
     * @param fmuKeyToFmuWithMD
     * @throws IllegalAccessException
     * @throws XPathExpressionException
     * @throws InvocationTargetException
     * @return
     */
    public static OutputsAndInputsToOutputs
    constructOutputsAndInputToOutputMap(List<ModelConnection> connections, HashMap<String, ModelDescription> fmuKeyToFmuWithMD)
            throws IllegalAccessException, XPathExpressionException, InvocationTargetException {

        HashMap<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs = new HashMap<>();
        HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputToOutputMap = new HashMap<>();
        for(ModelConnection conn : connections){
            // TODO: Validate only one instead of findFirst?
            // TODO: Validate Causality == Output?
            Optional<ModelDescription.ScalarVariable> fromVariable = fmuKeyToFmuWithMD.get(conn.from.instance.key)
                    .getScalarVariables().stream().filter(x -> x.name.equals(conn.from.variable)).findFirst();
            // TODO: Validate only one instead of findFirst?
            // TODO: Validate Causality == Input?
            Optional<ModelDescription.ScalarVariable> toVariable = fmuKeyToFmuWithMD.get(conn.to.instance.key).
                    getScalarVariables().stream().filter(x -> x.name.equals(conn.to.variable)).findFirst();

            if( fromVariable.isPresent() && toVariable.isPresent())
            {
                ModelDescription.ScalarVariable toVar = toVariable.get();
                ModelDescription.ScalarVariable fromVar = fromVariable.get();
                // Adding to inputToOutputMap
                HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable> inputSvToOutputInstanceScalarVariable = null;
                if(inputToOutputMap.containsKey(conn.to.instance))
                {
                    inputSvToOutputInstanceScalarVariable = inputToOutputMap.get(conn.to.instance);
                }
                else
                {
                    inputSvToOutputInstanceScalarVariable = new HashMap<>();
                    inputToOutputMap.put(conn.to.instance, inputSvToOutputInstanceScalarVariable);
                }
                inputSvToOutputInstanceScalarVariable.put(toVar, new InstanceScalarVariable(conn.from.instance, fromVar));

                Set<ModelDescription.ScalarVariable> outputScalarVariablesForInstance;
                // Adding to outputs
                if(outputs.containsKey(conn.from.instance))
                {
                    outputScalarVariablesForInstance = outputs.get(conn.from.instance);
                }
                else {
                    outputScalarVariablesForInstance = new HashSet<>();
                    outputs.put(conn.from.instance, outputScalarVariablesForInstance);
                }
                outputScalarVariablesForInstance.add(fromVar);
            }
        }

        OutputsAndInputsToOutputs outputsAndInputsToOutputs = new OutputsAndInputsToOutputs(inputToOutputMap, outputs);
        return outputsAndInputsToOutputs;
    }

}