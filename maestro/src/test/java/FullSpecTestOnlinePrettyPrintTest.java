import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.INode;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class FullSpecTestOnlinePrettyPrintTest extends OnlineTestFmusTest {

    final File directory;
    private final String name;

    public FullSpecTestOnlinePrettyPrintTest(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "online").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }

    @Test
    public void test() throws Exception {

        File config = new File(directory, "config.json");
        String s = "target/" + config.getAbsolutePath().substring(
                config.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") +
                        ("src" + "/test" + "/resources/").length());

        File workingDir = new File(s.replace('/', File.separatorChar)).getParentFile();
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }


        try (InputStream configStream = config.exists() ? new FileInputStream(config) : null) {

            for (INode spec : MableSpecificationGenerator.parse(getSpecificationFiles())) {
                download(collectFmus(spec, false));
            }

            long startTime = System.nanoTime();
            Instant start = Instant.now();
            IErrorReporter reporter = new ErrorReporter();
            FmiSimulationEnvironment environment = FmiSimulationEnvironment.of(new File(directory, "env.json"), reporter);
            if (reporter.getErrorCount() > 0) {
                reporter.printErrors(new PrintWriter(System.err, true));
                Assert.fail();
            }
            reporter.printWarnings(new PrintWriter(System.err, true));
            ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, false, environment).generate(getSpecificationFiles(), configStream);

            long stopTime = System.nanoTime();
            Instant end = Instant.now();

            System.out.println("############################################################");
            System.out.println("##################### Pretty Print #########################");
            System.out.println("############################################################");

            collectFmus(doc, true);

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
            new MableInterpreter(new DefaultExternalValueFactory(workingDir)).execute(reparsedDoc);

        }

    }

    private List<File> getSpecificationFiles() {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }
}
