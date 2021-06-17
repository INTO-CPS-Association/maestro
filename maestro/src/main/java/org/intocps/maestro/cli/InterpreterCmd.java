package org.intocps.maestro.cli;

import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import picocli.CommandLine;

import java.io.File;
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

    @CommandLine.Option(names = "-runtime", description = "Path to a runtime file which should be included in the export")
    File runtime;

    @CommandLine.Parameters(description = "One or more specification files")
    List<File> files;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the export will be stored")
    File output;
    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;

    @Override
    public Integer call() throws Exception {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = false;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        if (!util.parse(files)) {
            return -1;
        }
        if (!util.expand()) {
            return 1;
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!util.typecheck()) {
            return -1;
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return -1;
            }
        }

        if (runtime != null) {
            util.interpret(runtime);
        } else {
            util.interpret();

        }
        return 0;
    }
}
