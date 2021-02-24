package org.intocps.maestro.webapi.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaProcess {

    private JavaProcess() {
    }

    public static int exec(Class clazz, List<String> jvmArgs, List<String> args) throws IOException, InterruptedException {
        List<String> command = calculateCommand(clazz, jvmArgs, args);
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.inheritIO().start();
        process.waitFor();
        return process.exitValue();
    }

    public static List<String> calculateCommand(Class clazz, List<String> jvmArgs, List<String> args) {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = clazz.getName();

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(classpath);
        command.add(className);
        command.addAll(args);

        return command;
    }
}
