package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.INode;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class IntermediateSpecWriter {
    final File directory;
    final boolean enabled;
    int count = 0;

    IntermediateSpecWriter(File directory, boolean enabled) {
        this.directory = directory;
        this.enabled = enabled;
    }

    public void write(INode spec) {
        if (!enabled) {
            return;
        }
        try {
            int index = count++;
            FileUtils.write(new File(directory, "spec" + String.format("%05d", index) + "-numbered.mabl"), PrettyPrinter.printLineNumbers(spec),
                    StandardCharsets.UTF_8);
            FileUtils.write(new File(directory, "spec" + String.format("%05d", index) + ".mabl"), PrettyPrinter.print(spec), StandardCharsets.UTF_8);
        } catch (IOException | AnalysisException e) {
            e.printStackTrace();
        }
    }
}
