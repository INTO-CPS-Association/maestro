package org.intocps.maestro.cli;

import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.interpreter.InterpreterTransitionException;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "interpret", description = "Interpret a specification using the build in Java interpreter. Remember to place all " +
        "necessary runtime extensions in the classpath", mixinStandardHelpOptions = true)
public class InterpreterCmd implements Callable<Integer> {

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;

    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;

    @CommandLine.Option(names = {"--no-typecheck"}, description = "Perform type check", negatable = true)
    boolean useTypeCheck = true;

    @CommandLine.Option(names = {"--no-expand"}, description = "Perform expand", negatable = true)
    boolean useExpand = true;

    @CommandLine.Option(names = "-runtime", description = "Path to a runtime file which should be included in the export")
    File runtime;

    @CommandLine.Parameters(description = "One or more specification files")
    List<File> files;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the export will be stored")
    File output;
    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;

    @CommandLine.Option(names = "-wait", description = "Wait the specified seconds before processing. Intended for allowing a debugger or profiler " +
            "to be attached before the processing starts.")
    int wait = 0;

    @CommandLine.Option(names = "-transition", description = "Path to a directory with a transition specification")
    Path transitionPath;

    @CommandLine.Option(names = {"-thz", "--transition-check-frequency"}, description = "The interval which transition spec will be " + "checked at.")
    int transitionCheckFrequency;
    @CommandLine.Option(names = {"-tms", "--transition-minimum-step"},
            description = "The minimum step per for each none empty offering of " + "candidates. It reset once a candidate is removed" +
                    "checked at.")
    int transitionMinStep;

    private static void waitForProfiling(int wait) throws InterruptedException {
        System.out.printf("Initial wait activated, waiting... %d", wait);
        for (int i = 0; i < wait; i++) {
            Thread.sleep(1000);
            System.out.printf("\rInitial wait activated, waiting... %d", wait - i);
        }
    }

    @Override
    public Integer call() throws Exception {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = false;

        if (wait > 0) {
            waitForProfiling(wait);
        }

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        if (!util.parse(files)) {
            return -1;
        }
        if (useExpand) {
            if (!util.expand()) {
                return 1;
            }
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (useTypeCheck) {
            if (!util.typecheck()) {
                return -1;
            }
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return -1;
            }
        }

        try {
            if (transitionPath != null) {
                util.setTransitionPath(transitionPath, transitionCheckFrequency, transitionMinStep);
            }
            if (runtime != null) {
                util.interpret(runtime);
            } else {
                util.interpret();
            }
        } catch (InterpreterTransitionException e) {
            files.clear();
            files.add(e.getTransitionFile());
            useTypeCheck = false; //XXX: remove
            this.call();
        }
        return 0;
    }
}
