package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.cli.MaestroV1SimulationConfiguration;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.typechecker.TypeChecker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public class FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "full").toFile().listFiles()))
                .map(f -> Arguments.arguments(f.getName(), f));
    }

    private static TestJsonObject getTestJsonObject(File directory) throws java.io.IOException {
        TestJsonObject testJsonObject = null;
        File test = new File(directory, "test.json");

        if (test.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            testJsonObject = mapper.readValue(test, TestJsonObject.class);
        } else {
            testJsonObject = new TestJsonObject();
            testJsonObject.autoGenerate = false;
        }
        return testJsonObject;
    }

    private static List<String> fileToLines(InputStream filename) {
        List<String> lines = new LinkedList<>();
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static void assertResultEqualsDiff(InputStream actual, InputStream expected) {
        if (actual != null && expected != null) {
            List<String> original = fileToLines(actual);
            List<String> revised = fileToLines(expected);

            // Compute diff. Get the Patch object. Patch is the container for computed deltas.
            Patch patch = DiffUtils.diff(original, revised);

            System.err.println("Diff");
            for (Delta delta : patch.getDeltas()) {
                StringBuilder sb = new StringBuilder();
                switch (delta.getType()) {

                    case CHANGE:
                        sb.append("--- CHANGE ---\n");
                        break;
                    case DELETE:
                        sb.append("--- DELETE ---\n");
                        break;
                    case INSERT:
                        sb.append("--- INSERT ---\n");
                        break;
                }

                sb.append(original.stream().skip(delta.getOriginal().getPosition() - 1).map(l -> " " + l).limit(1).collect(Collectors.joining("\n")));
                sb.append("\n");
                sb.append(delta.getOriginal().getLines().stream().map(l -> ">" + l).limit(3).collect(Collectors.joining("\n")));
                sb.append("\n");
                sb.append("---  \n");
                sb.append(revised.stream().skip(delta.getOriginal().getPosition() - 1).map(l -> " " + l).limit(1).collect(Collectors.joining("\n")));
                sb.append("\n");
                sb.append(delta.getRevised().getLines().stream().map(l -> "<" + l).limit(3).collect(Collectors.joining("\n")));
                Assertions.fail("Expected result and actual differ: \n" + sb);
            }

        }
    }

    public static void compareCsvResults(File expectedCsvFile, File actualCsvFile) throws IOException {

        if (Boolean.parseBoolean(System.getProperty("TEST_CREATE_OUTPUT_CSV_FILES", "false")) && actualCsvFile.exists()) {
            System.out.println("Storing outputs csv file in specification directory to be used in future tests.");
            Files.copy(actualCsvFile.toPath(), expectedCsvFile.toPath(), REPLACE_EXISTING);
        }

        boolean actualOutputsCsvExists = actualCsvFile.exists();
        boolean expectedOutputsCsvExists = expectedCsvFile.exists();
        if (actualOutputsCsvExists && expectedOutputsCsvExists) {
            assertResultEqualsDiff(new FileInputStream(expectedCsvFile), new FileInputStream(actualCsvFile));
        } else {

            StringBuilder sb = new StringBuilder();

            sb.append("Cannot compare CSV files.\n");
            if (!actualOutputsCsvExists) {
                sb.append("The actual outputs csv file does not exist.\n");
            }
            if (!expectedOutputsCsvExists) {
                sb.append("The expected outputs csv file does not exist.\n");
            }
            System.out.println(sb.toString());

        }
    }

    static File getWorkingDirectory(File base, Class cls) throws IOException {
        String s = Paths.get("target", cls.getSimpleName()).toString() + File.separatorChar + base.getAbsolutePath().substring(
                base.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") + ("src" + "/test" + "/resources/").length());

        File workingDir = new File(s.replace('/', File.separatorChar));
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        return workingDir;
    }

    protected void compareCSVs(File expectedCsvFile, File actualCsvFile) throws IOException {
        compareCsvResults(expectedCsvFile, actualCsvFile);
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void test(String name, File directory) throws Exception {

        File workingDirectory = getWorkingDirectory(directory, this.getClass());

        IErrorReporter reporter = new ErrorReporter();
        Mabl mabl = new Mabl(directory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(getMablVerbose());

        Map.Entry<ARootDocument, Map<INode, PType>> res = generateSpec(mabl, directory, workingDirectory);
        postProcessSpec(name, directory, workingDirectory, mabl, res.getKey(), res.getValue());
    }

    protected boolean getMablVerbose() {
        return true;
    }

    protected void postProcessSpec(String name, File directory, File workingDirectory, Mabl mabl, ARootDocument spec,
            Map<INode, PType> types) throws Exception {
        interpretSpec(directory, workingDirectory, mabl, spec, types);
    }

    protected void interpretSpec(File directory, File workingDirectory, Mabl mabl, ARootDocument spec, Map<INode, PType> types) throws Exception {
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(types, name),
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(spec);

        compareCSVs(new File(directory, "expectedoutputs.csv"), new File(workingDirectory, "outputs.csv"));
    }

    @NotNull
    private Map.Entry<ARootDocument, Map<INode, PType>> generateSpec(Mabl mabl, File directory, File workingDirectory) throws Exception {
        File specFolder = new File(workingDirectory, "specs");
        specFolder.mkdirs();

        TestJsonObject testJsonObject = getTestJsonObject(directory);
        boolean useTemplate = testJsonObject != null && testJsonObject.autoGenerate;


        for (String lib : Arrays.asList("CSV", "DataWriter", "FMI2", "Logger", "Math", "ArrayUtil")) {
            FileUtils.copyInputStreamToFile(TypeChecker.class.getResourceAsStream("/org/intocps/maestro/typechecker/" + lib + ".mabl"),
                    new File(specFolder, lib + ".mabl"));
        }
        List<File> inputs = Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
        for (File input : inputs) {
            FileUtils.copyFile(input, new File(specFolder, input.getName()));
        }


        mabl.parse(getSpecificationFiles(specFolder));
        postParse(mabl);
        if (useTemplate) {
            MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder =
                                        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder();
            MaestroV1SimulationConfiguration configV1 = MaestroV1SimulationConfiguration.parse(new File(directory, "env.json"),new File(directory,
                    "config.json"));

            configV1.configure(builder);

            if (!testJsonObject.useLogLevels) {
                builder.setLogLevels(null);
            }

            MaBLTemplateConfiguration configuration = builder.build();

            mabl.generateSpec(configuration);
        }

        mabl.expand();
        var tcRes = mabl.typeCheck();
        mabl.verify(Framework.FMI2);


        if (mabl.getReporter().getErrorCount() > 0) {
            mabl.getReporter().printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        if (mabl.getReporter().getWarningCount() > 0) {
            mabl.getReporter().printWarnings(new PrintWriter(System.out, true));
        }

        mabl.dump(workingDirectory);
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_FILENAME).exists(), "Spec file must exist");
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_RUNTIME_FILENAME).exists(), "Spec file must exist");
        return Map.entry(mabl.getMainSimulationUnit(), tcRes.getValue());
    }

    protected void postParse(Mabl mabl) throws AnalysisException {

    }

    protected List<File> getSpecificationFiles(File specFolder) {

        return Arrays.stream(Objects.requireNonNull(specFolder.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }
}
