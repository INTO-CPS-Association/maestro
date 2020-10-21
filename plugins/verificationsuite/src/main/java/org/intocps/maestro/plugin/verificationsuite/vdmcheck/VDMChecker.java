package org.intocps.maestro.plugin.verificationsuite.vdmcheck;

//public class VDMChecker {
//
//    public void CheckFMUS(List<String> files) throws IOException {
//        files.forEach(o -> {
//            Path path = getPathToVDMCheck();
//            ProcessBuilder pb = new ProcessBuilder(path + "/VDMCheck2.sh", o);
//            Map<String, String> env = pb.environment();
//            try {
//                Process p = pb.start();
//                try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
//                    String line;
//
//                    while ((line = reader.readLine()) != null) {
//                        System.out.println(line);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//
//    private Path getPathToVDMCheck() {
//        var currentPath = Paths.get("").toAbsolutePath().getParent().normalize().toString();
//        var pluginString = "plugins";
//        if (currentPath.contains("plugins")) {
//            pluginString = "";
//        }
//        return Paths.get(currentPath, pluginString, "verificationsuite", "src", "main", "resources", "vdmcheck");
//    }
//}
