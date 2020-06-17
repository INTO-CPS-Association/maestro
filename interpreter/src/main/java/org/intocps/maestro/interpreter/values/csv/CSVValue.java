package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CSVValue extends ExternalModuleValue {
    public CSVValue() {
        super(createCsvMembers(), null);
    }


    static String getString(Value value) {

        value = value.deref();

        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        throw new InterpreterException("Value is not string");
    }


    private static Map<String, Value> createCsvMembers() {
        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("open", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            String path = getString(fcargs.get(0));

            try {
                return new CsvFileValue(new PrintWriter(new FileOutputStream(new File(path))));
            } catch (FileNotFoundException e) {
                throw new InterpreterException(e);
            }
        }));


        componentMembers.put("close", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            if (fcargs.get(0) instanceof CsvFileValue) {
                ((CsvFileValue) fcargs.get(0)).getModule().close();
            }

            return new VoidValue();
        }));


        return componentMembers;
    }
}
