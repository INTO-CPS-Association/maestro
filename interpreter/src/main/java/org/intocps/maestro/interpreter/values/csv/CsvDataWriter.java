package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.RealValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

public class CsvDataWriter implements IDataListener {
    private final String baseFilename = "csvResult";
    HashMap<UUID, PrintWriter> printers = new HashMap<>();
    private Integer currentFileNumber = 0;

    private String createFilename() {
        currentFileNumber++;
        return DataStore.GetInstance().getSessionDirectory().resolve(baseFilename + currentFileNumber + ".csv").toString();
    }

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(createFilename())));
            printers.put(uuid, writer);
            writer.println("time," + String.join(",", headers));
        } catch (FileNotFoundException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {
        List<String> data = new Vector<>();
        data.add(Double.toString(time));

        for (Value d : dataPoint) {
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

        this.printers.get(uuid).println(String.join(",", data));
    }

    @Override
    public void close() {
        this.printers.forEach((id, pw) -> {
            pw.flush();
            pw.close();
        });
        this.printers.clear();
    }
}
