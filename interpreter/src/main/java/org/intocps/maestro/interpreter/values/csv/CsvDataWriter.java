package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.RealValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.datawriter.DataFileRotater;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvDataWriter implements IDataListener {
    private final DataFileRotater dataFileRotater;
    HashMap<UUID, CsvDataWriterInstance> instances = new HashMap<>();
    FmiSimulationEnvironment environment;

    public CsvDataWriter(File outputFile) {
        this.dataFileRotater = new DataFileRotater(outputFile);
        this.environment = (FmiSimulationEnvironment) DataStore.GetInstance().getSimulationEnvironment();
    }

    /**
     * The headers of interest for the CSVWriter are all connected outputs and those in logVariables
     *
     * @param env
     * @return
     */
    public static List<String> calculateHeadersOfInterest(ISimulationEnvironment env) {

        FmiSimulationEnvironment environment = (FmiSimulationEnvironment) env;

        Set<Map.Entry<String, ComponentInfo>> instances = environment.getInstances();

        List<String> hoi = instances.stream().flatMap(instance -> {
            Stream<org.intocps.maestro.framework.fmi2.RelationVariable> relationOutputs =
                    environment.getRelations(new LexIdentifier(instance.getKey(), null)).stream()
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

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        CsvDataWriterInstance instance = new CsvDataWriterInstance();
        instance.headersOfInterest = calculateHeadersOfInterest(this.environment);
        // Discover the headers of interest and store the index of these
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (instance.headersOfInterest.contains(header)) {
                instance.indicesOfInterest.add(i);
            }
        }

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(dataFileRotater.getNextOutputFile()));
            instance.printWriter = writer;
            instances.put(uuid, instance);
            instance.println("time," + String.join(",", instance.headersOfInterest));
        } catch (FileNotFoundException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {
        List<String> data = new Vector<>();
        data.add(Double.toString(time));

        for (Integer i : instances.get(uuid).indicesOfInterest) {
            Value d = dataPoint.get(i);

            Object value = null;
            if (d instanceof IntegerValue) {
                value = ((IntegerValue) d).getValue();
            } else if (d instanceof RealValue) {
                value = ((RealValue) d).getValue();
            } else if (d instanceof BooleanValue) {
                value = ((BooleanValue) d).getValue();
            }

            data.add(value.toString());
        }

        this.instances.get(uuid).println(String.join(",", data));
    }

    @Override
    public void close() {
        this.instances.forEach((id, instance) -> instance.close());
        this.instances.clear();
    }

    static class CsvDataWriterInstance {
        public final List<Integer> indicesOfInterest = new ArrayList<>();
        public List<String> headersOfInterest;
        public PrintWriter printWriter;


        public void println(String data) {
            this.printWriter.println(data);
        }

        public void close() {
            this.printWriter.flush();
            this.printWriter.close();
        }
    }


}

