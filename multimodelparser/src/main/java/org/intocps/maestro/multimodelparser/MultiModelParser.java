package org.intocps.maestro.multimodelparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class MultiModelParser {
    final ObjectMapper mapper = new ObjectMapper();

    public MultiModelMessage ParseMultiModel(InputStream is) throws Exception {
        MultiModelMessage msg = null;
        try {
            msg = mapper.readValue(is, MultiModelMessage.class);
            Map<String, URI> fmuToURI = msg.getFmuFiles();
            List<ModelConnection> connections = buildConnections(msg.connections);
            List<ModelParameter> parameters = buildParameters(msg.parameters);
            HashMap<String, ModelDescription> fmuKeyToFmuWithMD = buildFmuKeyToFmuMD(fmuToURI);
            HashMap<String, Integer> fmusToInstancesCount = fmusToInstancesCount(msg.fmus.keySet(), connections);
            HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>>
                    inputToOutputs = constructInputToOutputMap(connections, fmuKeyToFmuWithMD);

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
    public static HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>>
    constructInputToOutputMap(List<ModelConnection> connections, HashMap<String, ModelDescription> fmuKeyToFmuWithMD)
            throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
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
            }
        }

        return inputToOutputMap;
    }
}