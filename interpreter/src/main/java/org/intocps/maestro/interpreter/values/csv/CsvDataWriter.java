package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.datawriter.DataFileRotater;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class CsvDataWriter implements IDataListener {
    final List<String> filter;
    private final DataFileRotater dataFileRotater;
    HashMap<UUID, CsvDataWriterInstance> instances = new HashMap<>();

    public CsvDataWriter(File outputFile, List<String> filter) {
        this.dataFileRotater = new DataFileRotater(outputFile);
        this.filter = filter;
    }


    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        CsvDataWriterInstance instance = new CsvDataWriterInstance();
        instance.headersOfInterest = filter == null ? headers : headers.stream().filter(filter::contains).collect(Collectors.toList());
        // Discover the headers of interest and store the index of these
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (filter == null || instance.headersOfInterest.contains(header)) {
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
            Value d = dataPoint.get(i).deref();

            Object value = null;
            if (d instanceof IntegerValue) {
                value = ((IntegerValue) d).getValue();
            } else if (d instanceof RealValue) {
                value = ((RealValue) d).getValue();
            } else if (d instanceof BooleanValue) {
                value = ((BooleanValue) d).getValue();
            } else if (d instanceof StringValue) {
                value = ((StringValue) d).getValue();
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

