package org.intocps.maestro;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MablRuntimeDataGenerator {
    final ISimulationEnvironment env;

    public MablRuntimeDataGenerator(ISimulationEnvironment env) {
        this.env = env;
    }

    /**
     * The headers of interest for the CSVWriter are all connected outputs and those in logVariables
     *
     * @param env
     * @return
     */
    public static List<String> getCsvWriterFilter(ISimulationEnvironment env) {

        FmiSimulationEnvironment environment = (FmiSimulationEnvironment) env;

        Set<Map.Entry<String, ComponentInfo>> instances = environment.getInstances();

        List<String> hoi = instances.stream().flatMap(instance -> {
            Stream<RelationVariable> relationOutputs = environment.getRelations(new LexIdentifier(instance.getKey(), null)).stream()
                    .filter(relation -> (relation.getOrigin() == FmiSimulationEnvironment.Relation.InternalOrExternal.External) &&
                            (relation.getDirection() == FmiSimulationEnvironment.Relation.Direction.OutputToInput))
                    .map(x -> x.getSource().scalarVariable);
            return Stream.concat(relationOutputs, environment.getCsvVariablesToLog(instance.getKey()).stream());
        }).map(x -> {
            ComponentInfo i = environment.getUnitInfo(new LexIdentifier(x.instance.getText(), null), Framework.FMI2);
            return String.format("%s.%s.%s", i.fmuIdentifier, x.instance.getText(), x.scalarVariable.getName());
        }).collect(Collectors.toList());

        return hoi;
    }

    public static List<String> getWebsocketWriterFilter(ISimulationEnvironment env) {
        FmiSimulationEnvironment environment = (FmiSimulationEnvironment) env;
        return environment.getLivestreamVariablesToLog().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(x -> entry.getKey() + "." + x)).collect(Collectors.toList());
    }

    public Object getRuntimeData() {
        Map<String, List<Map<String, Object>>> data = new HashMap<>();

        data.put("DataWriter", Arrays.asList(new HashMap<>() {
            {
                put("type", "CSV");
                put("filename", "output.csv");
                put("filter", getCsvWriterFilter(env));
            }
        }, new HashMap<>() {
            {
                put("type", "Websocket");
                put("interval", env.getEnvironmentMessage().getLivelogInterval());
                put("filter", getWebsocketWriterFilter(env));
            }

        }));

        return data;
    }
}
