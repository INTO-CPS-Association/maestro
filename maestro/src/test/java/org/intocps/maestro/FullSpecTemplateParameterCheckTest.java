package org.intocps.maestro;

import org.junit.Assert;
import org.junit.jupiter.params.provider.Arguments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FullSpecTemplateParameterCheckTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "full").toFile().listFiles()))
                .filter(p -> p.getName().equals("initialize_singleWaterTank_parameters")).map(f -> Arguments.arguments(f.getName(), f));
    }

    @Override
    protected void compareCSVs(File expectedCsvFile, File actualCsvFile) throws IOException {
        super.compareCSVs(expectedCsvFile, actualCsvFile);
        final String signalName = "{x2}.wtInstance.level";
        List<Double> records = new ArrayList<>();
        Integer levelColumnId = null;
        try (BufferedReader br = new BufferedReader(new FileReader(actualCsvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (levelColumnId == null) {
                    for (int i = 0; i < values.length; i++) {
                        if (signalName.equals(values[i])) {
                            levelColumnId = i;
                            break;
                        }
                    }
                } else {
                    records.add(Double.parseDouble(values[levelColumnId]));
                }
            }
        }

        final double min = 3.0;
        final double max = 5.0;
        boolean inRange = false;
        final double delta = 0.2;
        for (int i = 0; i < records.size(); i++) {
            double v = records.get(i);
            if (!inRange) {
                inRange = v > min;
            } else {
                if (v < min - delta) {
                    Assert.fail(String.format("Parameter %s is below the specified value %f",signalName,v));
                }else if(v> max+delta)
                {
                    Assert.fail(String.format("Parameter %s is above the specified value %f",signalName,v));
                }
            }
        }

    }
}
