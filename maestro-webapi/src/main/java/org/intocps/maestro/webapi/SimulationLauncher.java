package org.intocps.maestro.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.stream.Stream;

public class SimulationLauncher {
    /**
     * Sun property pointing the main class and its arguments.
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";
    private final static Logger logger = LoggerFactory.getLogger(SimulationLauncher.class);

    /**
     * Restart the current Java application
     *
     * @param additionalArguments some custom code to be run before restarting
     * @return
     * @throws IOException
     */
    public static Process restartApplication(File workingDirectory, String... additionalArguments) throws IOException {
        try {
            // java binary
            String java = "java";//System.getProperty("java.home") + "/bin/java";

            ProcessBuilder pb = new ProcessBuilder(java);
            Stream.of(additionalArguments).forEach(c -> pb.command().add(c));
            if (workingDirectory != null) {
                if (!workingDirectory.mkdirs()) {
                    if (!workingDirectory.exists()) {
                        logger.warn("Working directory does not exist {}", workingDirectory);
                    }
                }
                logger.debug("Setting working directory to: '{}'", workingDirectory.getAbsoluteFile());
                pb.directory(workingDirectory.getAbsoluteFile());
            }

            // vm arguments
            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            //            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib") && !arg.contains("-javaagent") && !arg.contains("-Dserver.port=")) {
                    pb.command().add(arg);
                }
            }


            // program main and program arguments
            String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                pb.command().add("-jar");
                File jarFile = new File(mainCommand[0]);

                if (!jarFile.isAbsolute()) {
                    jarFile = jarFile.getAbsoluteFile();

                }
                pb.command().add(jarFile.getCanonicalPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                pb.environment().put("CLASSPATH", System.getProperty("java.class.path"));
                pb.command().add(mainCommand[0]);
            }
            // finally add program arguments
            for (int i = 1; i < mainCommand.length; i++) {
                pb.command().add(mainCommand[i]);
            }

            logger.debug(String.join(" ", pb.command()));

            return pb.inheritIO().start();
        } catch (Exception e) {
            // something went wrong
            throw new IOException("Error while trying to restart the application", e);
        }
    }
}
