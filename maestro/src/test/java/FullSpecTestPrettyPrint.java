import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class FullSpecTestPrettyPrint {

    final File directory;
    private final String name;

    public FullSpecTestPrettyPrint(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "full").toFile().listFiles()))
                .map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
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


            long startTime = System.nanoTime();
            Instant start = Instant.now();


            ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, false, UnitRelationship.of(new File(directory, "env.json")))
                    .generate(getSpecificationFiles(), configStream);

            long stopTime = System.nanoTime();
            Instant end = Instant.now();

            System.out.println("############################################################");
            System.out.println("##################### Pretty Print #########################");
            System.out.println("############################################################");

            String sourceCode = PrettyPrinter.print(doc);
            System.out.println(sourceCode);


            ARootDocument reparsedDoc = MableSpecificationGenerator.parse(CharStreams.fromString(sourceCode));
            new MableInterpreter(new DefaultExternalValueFactory(workingDir)).execute(reparsedDoc);

        }

    }

    private List<File> getSpecificationFiles() {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }
}