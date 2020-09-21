package org.intocps.maestro.plugin.prologverifier.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class VDMChecker {

    public void CheckFMUS(List<String> files) throws IOException {
        files.forEach(o -> {
            Path path = getPathToVDMCheck();
            ProcessBuilder pb = new ProcessBuilder(path + "/VDMCheck2.sh", o);
            Map<String, String> env = pb.environment();
            try {
                Process p = pb.start();
                try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private Path getPathToVDMCheck() {
        var currentPath = Paths.get("").toAbsolutePath().getParent().normalize().toString();
        var pluginString = "plugins";
        if (currentPath.contains("plugins")) {
            pluginString = "";
        }
        return Paths.get(currentPath, pluginString, "prologverifier", "src", "main", "resources", "vdmcheck");
    }
}
