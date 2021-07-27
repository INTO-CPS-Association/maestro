package org.intocps.maestro.cli;

import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.codegen.mabl2cpp.MablCppCodeGenerator;
import org.intocps.maestro.core.Framework;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "export", description = "Specification export", mixinStandardHelpOptions = true)
public class ExportCmd implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "The valid exporters: ${COMPLETION-CANDIDATES}")
    ExportType type;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;

    @CommandLine.Option(names = "-runtime", description = "Path to a runtime file which should be included in the export")
    File runtime;

    @CommandLine.Parameters(index = "1..*", description = "One or more specification files")
    List<File> files;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the export will be stored")
    File output;
    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;

    @Override
    public Integer call() throws Exception {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = false;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        if (!util.parse(files)) {
            return -1;
        }

        if (!util.typecheck()) {
            return -1;
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return -1;
            }
        }

        if (type == ExportType.Cpp) {
            if (runtime != null && runtime.exists() && !runtime.getParentFile().equals(output)) {
                Files.copy(runtime.toPath(), new File(output, runtime.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter("spec.mabl"));
            writer.write(PrettyPrinter.print(util.mabl.getMainSimulationUnit()));
            writer.close();
            new MablCppCodeGenerator(output).generate(util.mabl.getMainSimulationUnit(), util.typeCheckResult.getValue());
        }

        return 0;
    }

    enum ExportType {
        Cpp
    }
}
