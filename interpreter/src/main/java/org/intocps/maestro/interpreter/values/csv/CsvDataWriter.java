package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.NumericValue;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
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
    static final String CSV_DATA_WRITER_PRECISION = "CSV_DATA_WRITER_PRECISION";

    static final String floatFormatter = System.getProperty(CSV_DATA_WRITER_PRECISION) != null ? ("%." + System.getProperty(
            CSV_DATA_WRITER_PRECISION) + "f") : null;
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
        data.add(floatFormatter == null ? Double.toString(time) : String.format(Locale.US, floatFormatter, time));

        for (Integer i : instances.get(uuid).indicesOfInterest) {
            Value d = dataPoint.get(i).deref();

            Object value = null;
            if (d.isNumericDecimal()) {
                if (floatFormatter == null) {
                    value = ((NumericValue) d).doubleValue();
                } else {
                    value = String.format(Locale.US, floatFormatter, ((NumericValue) d).doubleValue());
                }
            } else if (d.isNumeric()) {
                value = ((NumericValue) d).intValue();
            } else if (d instanceof BooleanValue) {
                value = ((BooleanValue) d).getValue();
            } else if (d instanceof StringValue) {
                value = ((StringValue) d).getValue();
            }

            data.add(value.toString());
        }
        CsvDataWriterInstance instance = this.instances.get(uuid);
        instance.println(String.join(",", data));
        instance.flush();
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

        public void flush() {
            this.printWriter.flush();
        }

        public void close() {
            this.printWriter.flush();
            this.printWriter.close();
        }
    }


}

