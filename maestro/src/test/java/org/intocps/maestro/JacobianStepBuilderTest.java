package org.intocps.maestro;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class JacobianStepBuilderTest extends FullSpecTest {

    /**
     * This is the data provider function that is used by the super class.
     *
     * @return
     */
    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "jacobian_step_builder").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
    }

    /**
     * Compares columns of two CSV files without the need for them to be in the same order.
     * It is expected that the exact same columns exists in both files.
     *
     * @param expectedCsvFile
     * @param actualCsvFile
     * @throws IOException
     */
    @Override
    protected void compareCSVs(File expectedCsvFile, File actualCsvFile) throws IOException {
        csvCompare(expectedCsvFile, actualCsvFile);
    }

    protected static void csvCompare(File expectedCsvFile, File actualCsvFile) throws IOException{
        final String ROW_SEPARATOR = ",";
        boolean actualOutputsCsvExists = actualCsvFile.exists();
        boolean expectedOutputsCsvExists = expectedCsvFile.exists();

        if (actualOutputsCsvExists && expectedOutputsCsvExists) {

            //Map content of expectedCsvFile to list of data foreach column.
            List<String> csvFileLines = Files.readAllLines(expectedCsvFile.toPath(), StandardCharsets.UTF_8);
            Map<String, List<String>> expectedCsvFileColumnsMap = new HashMap<>();
            Map<Integer, String> columnNameToColumnIndex = new HashMap<>();
            for (int i = 0; i < csvFileLines.size(); i++) {
                int columnIndex = 0;
                for (String columnVal : csvFileLines.get(i).split(ROW_SEPARATOR)) {
                    if (i == 0) {
                        expectedCsvFileColumnsMap.put(columnVal.strip(), new ArrayList<>()); //Strip accidental white spaces
                        columnNameToColumnIndex.put(columnIndex, columnVal.strip());
                    } else {
                        expectedCsvFileColumnsMap.get(columnNameToColumnIndex.get(columnIndex)).add(columnVal.strip());
                    }
                    columnIndex++;
                }
            }

            //Map content of actualCsvFile to list of data foreach column.
            columnNameToColumnIndex = new HashMap<>();
            csvFileLines = Files.readAllLines(actualCsvFile.toPath(), StandardCharsets.UTF_8);
            Map<String, List<String>> actualCsvFileColumnsMap = new HashMap<>();
            for (int i = 0; i < csvFileLines.size(); i++) {
                int columnIndex = 0;
                for (String columnVal : csvFileLines.get(i).split(ROW_SEPARATOR)) {
                    if (i == 0) {
                        actualCsvFileColumnsMap.put(columnVal, new ArrayList<>());
                        columnNameToColumnIndex.put(columnIndex, columnVal);
                    } else {
                        actualCsvFileColumnsMap.get(columnNameToColumnIndex.get(columnIndex)).add(columnVal);
                    }
                    columnIndex++;
                }
            }

            String assertionMsg = "";

            // Validate that columns are equal
            if (actualCsvFileColumnsMap.keySet().size() != expectedCsvFileColumnsMap.keySet().size()) {
                assertionMsg = "CSV files do not contain the same amount of columns";
            } else if (!actualCsvFileColumnsMap.keySet().containsAll(expectedCsvFileColumnsMap.keySet())) {
                assertionMsg = "CSV files do not contain the same columns";
            } else {
                // Validate that columns contains the same elements
                for (Map.Entry<String, List<String>> entry : expectedCsvFileColumnsMap.entrySet()) {
                    List<String> expected = entry.getValue();
                    List<String> actual = actualCsvFileColumnsMap.get(entry.getKey());

                    if (expected.size() != actual.size()) {
                        assertionMsg = "The length of column " + entry.getKey() + " differs between expected and actual";
                        break;
                    } else {
                        int mismatchedLines = 0;
                        for (int i = 0; i < expected.size(); i++) {
                            if (!expected.get(i).equals(actual.get(i))) {
                                if (assertionMsg.isEmpty()) {
                                    assertionMsg = "Mismatch between values on row " + (i + 2) + " for column '" + entry.getKey() + "'. Actual: " +
                                            actual.get(i) + " " + "expected: " + expected.get(i) + ". ";
                                }
                                mismatchedLines++;
                            }
                        }

                        if (!assertionMsg.isEmpty()) {
                            assertionMsg += "Additional " + (mismatchedLines - 1) + " rows have mismatched values.";
                            break;
                        }
                    }
                }
            }
            if(!assertionMsg.isEmpty()){
                System.out.println(assertionMsg);
                Assertions.fail(assertionMsg);
            }

        } else {

            StringBuilder sb = new StringBuilder();

            sb.append("Cannot compare CSV files.\n");
            if (!actualOutputsCsvExists) {
                sb.append("The actual outputs csv file does not exist.\n");
            }
            if (!expectedOutputsCsvExists) {
                sb.append("The expected outputs csv file does not exist.\n");
            }
            System.out.println(sb);
            Assertions.fail(sb.toString());
        }
    }
}
