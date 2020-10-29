package org.intocps.maestro.interpreter.values.datawriter;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class DataFileRotater {
    private final String outputDirectory;
    private final String filename;
    private File outputFile;
    private Boolean firstRun = true;
    private Integer fileCount = 1;

    public DataFileRotater(File outputFile) {
        this.outputFile = outputFile;
        this.outputDirectory = outputFile.getAbsoluteFile().getParent();
        this.filename = FilenameUtils.removeExtension(outputFile.getName());
    }

    public File getNextOutputFile() {
        if (this.firstRun) {
            this.firstRun = false;
        } else {
            this.outputFile = new File(this.outputDirectory, this.filename + this.fileCount + ".csv");
            this.fileCount++;
        }
        return this.outputFile;
    }
}