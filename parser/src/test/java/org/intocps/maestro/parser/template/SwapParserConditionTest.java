package org.intocps.maestro.parser.template;

import com.codepoetics.protonpack.StreamUtils;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SwapParserConditionTest {
    public static Collection<Object[]> data() throws IOException {
        File file = Paths.get("src", "test", "resources", "swap-conditions.txt").toFile();
        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

        return StreamUtils.zipWithIndex(lines.stream()).map(line -> new Object[]{file.getName() + ":" + line.getIndex(), line.getValue(), file})
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(String name, String line, File source) throws IOException, AnalysisException {

        System.out.println("############################## Source #################################\n\n");
        System.out.println(Files.readString(source.toPath()));

        ErrorReporter reporter = new ErrorReporter();
        PExp result = MablSwapConditionParserUtil.parse(CharStreams.fromString(line, name), reporter);
        System.out.println("############################## Parsed to string #################################\n\n");
        System.out.println(result);
        Assertions.assertEquals(0, reporter.getErrors().size(), "No errors should exist");


        String ppRoot = PrettyPrinter.print(result);

        System.out.println("############################## PrettyPrint #################################\n\n");
        System.out.println(ppRoot);


    }
}
