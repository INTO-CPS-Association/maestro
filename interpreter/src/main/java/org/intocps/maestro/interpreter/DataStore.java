package org.intocps.maestro.interpreter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DataStore {
    private static final DataStore dataStore = new DataStore();
    private Path sessionDirectory;

    private DataStore() {
        this.sessionDirectory = Paths.get("");
    }

    public static DataStore GetInstance() {
        return dataStore;
    }

    public Path getSessionDirectory() {
        return sessionDirectory;
    }

    public void setSessionDirectory(Path sessionDirectory) {
        this.sessionDirectory = sessionDirectory;
    }
}
