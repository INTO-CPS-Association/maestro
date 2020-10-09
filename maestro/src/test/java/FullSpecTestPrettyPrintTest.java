import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.MaBLTemplateGenerator.MaBLTemplateConfiguration;
import org.intocps.maestro.MaBLTemplateGenerator.MaBLTemplateGenerator;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepSizeAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.EnvironmentMessage;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class FullSpecTestPrettyPrintTest {

    final File directory;
    private final String name;

    public FullSpecTestPrettyPrintTest(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "full").toFile().listFiles()))
                .map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }

    private static TestJsonObject getTestJsonObject(File directory) throws java.io.IOException {
        TestJsonObject testJsonObject = null;
        File test = new File(directory, "test.json");

        if (test.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            testJsonObject = mapper.readValue(test, TestJsonObject.class);
        } else {
            testJsonObject = new TestJsonObject();
            testJsonObject.useLocalSpec = true;
        }
        return testJsonObject;
    }

    private static File getWorkingDirectory(File configurationFile) {
        String s = "target/" + configurationFile.getAbsolutePath().substring(
                configurationFile.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") +
                        ("src" + "/test" + "/resources/").length());

        File workingDir = new File(s.replace('/', File.separatorChar)).getParentFile();
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        return workingDir;
    }

    private static ARootDocument generateDocumentWithTemplate(TestJsonObject testJsonObject, InputStream configStream,
            FmiSimulationEnvironment environment,
            MableSpecificationGenerator mableSpecificationGenerator) throws AnalysisException, XPathExpressionException, IOException {
        ARootDocument doc;
        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder templateBuilder =
                MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder().setUnitRelationship(environment)
                        .useInitializer(testJsonObject.initialize).setLogLevels(environment.getLogLevels());

        if (testJsonObject.simulate && environment.getEnvironmentMessage().algorithm instanceof EnvironmentMessage.FixedStepAlgorithmConfig) {
            EnvironmentMessage.FixedStepAlgorithmConfig a =
                    (EnvironmentMessage.FixedStepAlgorithmConfig) environment.getEnvironmentMessage().algorithm;
            templateBuilder.setStepAlgorithm(new FixedStepSizeAlgorithm(environment.getEnvironmentMessage().endTime, a.size));
        }

        MaBLTemplateConfiguration configuration = templateBuilder.build();
        String template = PrettyPrinter.print(MaBLTemplateGenerator.generateTemplate(configuration));
        System.out.println(template);
        doc = mableSpecificationGenerator.generateFromStreams(Arrays.asList(CharStreams.fromString(template)), configStream);
        return doc;
    }

    @Test
    public void test() throws Exception {
        File config = new File(directory, "config.json");

        try (InputStream configStream = config.exists() ? new FileInputStream(config) : null) {
            long startTime = System.nanoTime();
            Instant start = Instant.now();

            IErrorReporter reporter = new ErrorReporter();
            FmiSimulationEnvironment environment = FmiSimulationEnvironment.of(new File(directory, "env.json"), reporter);
            if (reporter.getErrorCount() > 0) {
                reporter.printErrors(new PrintWriter(System.err, true));
                Assert.fail();
            }
            MableSpecificationGenerator mableSpecificationGenerator = new MableSpecificationGenerator(Framework.FMI2, true, environment);

            TestJsonObject testJsonObject = getTestJsonObject(directory);
            ARootDocument doc = null;
            if (testJsonObject.useLocalSpec == false) {
                doc = generateDocumentWithTemplate(testJsonObject, configStream, environment, mableSpecificationGenerator);
            } else {
                doc = mableSpecificationGenerator.generate(getSpecificationFiles(), configStream);
            }

            long stopTime = System.nanoTime();
            Instant end = Instant.now();

            System.out.println("############################################################");
            System.out.println("##################### Pretty Print #########################");
            System.out.println("############################################################");

            String sourceCode = PrettyPrinter.print(doc);
            System.out.println(sourceCode);


            ARootDocument reparsedDoc = null;
            try {
                reparsedDoc = MableSpecificationGenerator.parse(CharStreams.fromString(sourceCode));
            } catch (Exception e) {
                System.out.println(PrettyPrinter.printLineNumbers(doc));
                throw e;
            }

            DataStore.GetInstance().setSimulationEnvironment(environment);
            File workingDir = getWorkingDirectory(config);
            new MableInterpreter(new DefaultExternalValueFactory(workingDir)).execute(reparsedDoc);
        }
    }

    private List<File> getSpecificationFiles() {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }
}
