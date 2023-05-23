package org.intocps.maestro.cli;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.*;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.intocps.maestro.typechecker.TypeChecker;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    ITransitionManager tm;

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
        new MableInterpreter(createValueFactory(workingDirectory, config), tm).execute(mabl.getMainSimulationUnit());
    }

    public void interpret(File config) throws Exception {
        InputStream c = new FileInputStream(config);
        new MableInterpreter(createValueFactory(workingDirectory, c), tm).execute(mabl.getMainSimulationUnit());
    }

    private DefaultExternalValueFactory createValueFactory(File workingDirectory,
            InputStream c) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(typeCheckResult.getValue(), name), c);
    }

    public boolean generateSpec(ScenarioConfiguration scenarioConfiguration) throws Exception {
        mabl.generateSpec(scenarioConfiguration);
        return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
    }

    public void setTransitionPath(Path transitionPath, int transitionCheckFrequency, int transitionMinStep) {

        ITransitionManager.ISpecificationProvider specificationProvider = new DirectorySpecificationProvider(transitionPath.toFile(), (f) -> {
            Mabl m = new Mabl(f.getParentFile(), f.getParentFile(), mabl.getSettings());
            try {
                m.parse(List.of(f));
                Map.Entry<Boolean, Map<INode, PType>> res = m.typeCheck();
                if (!res.getKey()) {
                    return null;
                }
                return m.getMainSimulationUnit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, transitionCheckFrequency, transitionMinStep);

        tm = new TransitionManager(specificationProvider) {

            @Override
            public void transfer(Interpreter interpreter, ITTransitionInfo info) throws AnalysisException {
                try {
                    super.transfer(new Interpreter(interpreter.getLoadFactory().changeWorkingDirectory(info.workingDirectory(), null), this), info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }


}
