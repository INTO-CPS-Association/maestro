package org.intocps.maestro.interpreter.values.modeltransition;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.InterpreterTransitionException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterConfigValue;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  Singleton store that keeps map of transition names and values
 */
class ModelTransitionStore {
    private static ModelTransitionStore instance = null;
    private Map<String, Value> values = null;
    private String transitionPath;
    private Path transitionFile;
    private Path transitionLog;
    private int transitionCount;

    private ModelTransitionStore(String transitionPath) {
        values = new HashMap<>();
        transitionCount = 1;
        this.transitionPath = transitionPath;
        transitionFile = Paths.get(transitionPath + "/" + transitionPath + "_" + transitionCount + ".mabl");
        transitionLog = Paths.get(transitionPath + "/log.txt");
        try {
            if (Files.exists(transitionLog)) {
                Files.delete(transitionLog);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModelTransitionStore getInstance(String transitionPath) {
        if (instance == null) {
            instance = new ModelTransitionStore(transitionPath);
        }
        return instance;
    }

    public void put(String id, Value val) {
       values.put(id, val);
    }

    public Value get(String id) {
        return values.get(id);
    }

    public void writeTransitionLog() {
        try {
            Files.write(instance.transitionLog, (Integer.toString(transitionCount) + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNextTransitionFile() {
        transitionCount++;
        transitionFile = Paths.get(transitionPath + "/" + transitionPath + "_" + transitionCount + ".mabl");
    }

    public Path getNextTransitionFile() {
       return transitionFile;
    }
}

public class ModelTransitionValue extends ModuleValue {

    public ModelTransitionValue(String transitionPath) {
        super(createMembers(ModelTransitionStore.getInstance(transitionPath)));
    }

    private static Map<String, Value> createMembers(ModelTransitionStore instance) {
        Map<String, Value> members = new HashMap<>();

        members.put("setValue", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 2);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            Value val = args.get(1);

            instance.put(id, val);
            return new VoidValue();
        }));

        members.put("getValue", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 1);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            return instance.get(id);
        }));

        members.put("doTransition", new FunctionValue.ExternalFunctionValue(fcargs -> {
            Path transitionFile = instance.getNextTransitionFile();
            instance.writeTransitionLog();
            instance.setNextTransitionFile();
            throw new InterpreterTransitionException(transitionFile.toFile());
        }));

        members.put("nextTransition", new FunctionValue.ExternalFunctionValue(fcargs -> {
            Path transitionFile = instance.getNextTransitionFile();
            return new BooleanValue(Files.exists(transitionFile));
        }));

        return members;
    }
}
