package org.intocps.maestro.cli;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.*;
import org.intocps.maestro.template.ScenarioConfiguration;
import picocli.CommandLine;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MablCliUtil {
    static final Predicate<File> mableFileFilter = f -> f.getName().toLowerCase().endsWith(".mabl");
    protected final Mabl mabl;
    final IErrorReporter reporter = new ErrorReporter();
    private final File workingDirectory;
    public boolean verbose;
    public Map.Entry<Boolean, Map<INode, PType>> typeCheckResult;
    ITTransitionManager tm;
    MablCliUtilTransfer utilTransfer;

    public MablCliUtil(File workingDirectory, File debugOutputFolder, Mabl.MableSettings settings) {
        this.workingDirectory = workingDirectory;
        this.mabl = new Mabl(workingDirectory, debugOutputFolder, settings);
        this.mabl.setReporter(reporter);
    }

    /**
     * Returns true if there are any errors
     *
     * @param verbose
     * @param reporter
     * @return
     */
    public static boolean hasErrorAndPrintErrorsAndWarnings(boolean verbose, IErrorReporter reporter) {
        if (reporter.getWarningCount() > 0) {
            if (verbose) {
                reporter.printWarnings(new PrintWriter(System.out, true));
            }
        }
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            return true;
        }

        return false;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        mabl.setVerbose(verbose);
    }

    public boolean parse(List<File> files) throws Exception {
        List<File> sourceFiles = Stream.concat(
                files.stream().filter(File::isDirectory).flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles(mableFileFilter::test)))),
                files.stream().filter(File::isFile).filter(mableFileFilter)).collect(Collectors.toList());

        if (sourceFiles.isEmpty()) {
            return true;
        }

        mabl.parse(sourceFiles);

        return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
    }

    public boolean expand() throws Exception {
        mabl.expand();
        return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
    }

    public boolean typecheck() {
        this.typeCheckResult = mabl.typeCheck();
        if (!typeCheckResult.getKey()) {
            if (reporter.getErrorCount() > 0) {
                return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
            }
            return false;
        }
        return true;
    }

    public boolean verify(Framework framework) {
        if (!mabl.verify(framework)) {
            return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
        }

        // verify can be true but there can still be warnings.
        hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
        return true;
    }

    public void interpret() throws Exception {
        InputStream config = IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8);
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, config), tm).execute(mabl.getMainSimulationUnit());
    }

    public void interpret(File config) throws Exception {
        InputStream c = new FileInputStream(config);
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, c), tm).execute(mabl.getMainSimulationUnit());
    }

    public boolean generateSpec(ScenarioConfiguration scenarioConfiguration) throws Exception {
        mabl.generateSpec(scenarioConfiguration);
        return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
    }

    public void setTransitionPath(Path transitionPath) {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = false;
        settings.preserveFrameworkAnnotations = true;
        settings.inlineFrameworkConfig = false;
        utilTransfer = new MablCliUtilTransfer(transitionPath.toFile(), transitionPath.toFile(), settings);
        utilTransfer.setVerbose(true);
        File transitionSpec = transitionPath.resolve("spec.mabl").toFile();

        ITTransitionManager.ISpecificationProvider specificationProvider = new ITTransitionManager.ISpecificationProvider() {
            final Map<String, ARootDocument> candidates = new HashMap<>();
            final Set<String> removedCandidates = new HashSet<>();

            @Override
            public Map<String, ARootDocument> get() {
                if (transitionSpec.exists() && !removedCandidates.contains(transitionSpec.getAbsolutePath())) {
                    try {
                        utilTransfer.parse(Arrays.asList(transitionSpec));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    candidates.put(transitionSpec.getAbsolutePath(), utilTransfer.getMainSimulationUnit());
                }
                return candidates;
            }

            @Override
            public Map<String, ARootDocument> get(String name) {
                return get();
            }

            @Override
            public void remove(ARootDocument specification) {
                candidates.entrySet().stream().filter(map -> map.getValue().equals(specification)).map(Map.Entry::getKey).findFirst()
                        .ifPresent(key -> {candidates.remove(key); removedCandidates.add(key);});
            }
        };

        tm = new TransitionManager() {
            @Override
            public ISpecificationProvider getSpecificationProvider() {
                return specificationProvider;
            }

            @Override
            public void transfer(Interpreter interpreter, ITTransitionInfo info) throws AnalysisException {
                try {
                    InputStream config = IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8);
                    super.transfer(new Interpreter(new DefaultExternalValueFactory(transitionPath.toFile(), config), this), info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    class MablCliUtilTransfer extends MablCliUtil {
        public MablCliUtilTransfer(File workingDirectory, File debugOutputFolder, Mabl.MableSettings settings) {
            super(workingDirectory, debugOutputFolder, settings);
        }

        public ARootDocument getMainSimulationUnit() {
            return super.mabl.getMainSimulationUnit();
        }
    }
}
