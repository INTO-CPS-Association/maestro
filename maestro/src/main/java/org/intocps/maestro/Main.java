package org.intocps.maestro;

import org.intocps.maestro.cli.ExportCmd;
import org.intocps.maestro.cli.ImportCmd;
import org.intocps.maestro.cli.InterpreterCmd;
import org.intocps.maestro.cli.MablCmdVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "mabl", mixinStandardHelpOptions = true, versionProvider = MablCmdVersionProvider.class,
        description = "Mable for co-simulating models", usageHelpAutoWidth = true,
        subcommands = {InterpreterCmd.class, ExportCmd.class, ImportCmd.class}, headerHeading = "@|bold,underline Usage|@:%n%n",
        synopsisHeading = "%n", descriptionHeading = "%n@|bold,underline Description|@:%n%n",
        parameterListHeading = "%n@|bold,underline Parameters|@:%n", optionListHeading = "%n@|bold,underline Options|@:%n")
public class Main implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        System.exit(exitCode);
    }

    public static boolean argumentHandler(String... args) {
        return 0 == new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        return null;
    }

}
