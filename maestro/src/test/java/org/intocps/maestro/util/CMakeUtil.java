package org.intocps.maestro.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class CMakeUtil {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static Boolean ninjaFound;
    boolean autoNinja = false;
    private boolean verbose = true;

    public static boolean hasCmake() {

        return checkSuccessful("cmake", "--version");

    }

    private static boolean checkSuccessful(String... cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process process = null;
        try {
            process = pb.start();
            process.waitFor(10, TimeUnit.SECONDS);
            return !process.isAlive() && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public static boolean hasMake() {

        return checkSuccessful("make", "--version");

    }

    public static boolean hasNinja() {

        if (ninjaFound == null) {
            ninjaFound = checkSuccessful("ninja", "--version");
        }
        return ninjaFound;

    }

    public static boolean isMac() {

        return OS.indexOf("mac") >= 0;

    }

    public static boolean isWindows() {

        return OS.indexOf("win") >= 0;

    }

    public static boolean isUnix() {

        return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0;

    }

    public static boolean runProcess(ProcessBuilder pb, boolean verbose) throws IOException, InterruptedException, CMakeGenerateException {
        final Process p = pb.start();


        List<String> errors = new Vector<>();
        if (verbose) {
            Thread outThread = new Thread(() -> {
                BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String tmp;
                try {
                    while ((tmp = out.readLine()) != null) {
                        System.out.println(tmp);
                    }
                } catch (IOException e) {
                }
            });

            outThread.setDaemon(true);
            outThread.start();

            Thread errThread = new Thread(() -> {
                BufferedReader out = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                String tmp;
                try {
                    while ((tmp = out.readLine()) != null) {
                        System.out.println(tmp);
                        errors.add(tmp);
                    }
                } catch (IOException e) {
                }
            });

            errThread.setDaemon(true);
            errThread.start();
        }

        p.waitFor(30, TimeUnit.MINUTES);

        errors.addAll(IOUtils.readLines(p.getErrorStream(), StandardCharsets.UTF_8));

        boolean res = p.exitValue() == 0;

        if (!res) {
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String string : errors) {
                    sb.append(string);
                    sb.append("\n");
                }
                throw new CMakeGenerateException(sb.toString());
            }
        }
        return res;
    }

    String toPath(File file) {
        //        if (isWindows()) {
        //            return ("/" + file.getAbsolutePath()).replace(":", "").replace('\\', '/').replace("//", "/");
        //        } else {
        return file.getAbsolutePath();
        //        }
    }

    public boolean generate(File source, File build, File install) throws IOException, InterruptedException, CMakeGenerateException {
        String cmake = "cmake";

        if (isMac() && System.getenv("CI")==null) {
            cmake = "/usr/local/bin/cmake";
        }

        List<String> cmds = new Vector<>();
        cmds.add(cmake);

        if (autoNinja && hasNinja()) {
            cmds.add("-GNinja");
        } else if (isWindows()) {
            cmds.add("-GMSYS Makefiles");
        }

        if (install != null) {
            cmds.add("-DCMAKE_INSTALL_PREFIX=" + toPath(install));
        }


        if (build == null) {
            cmds.add(".");
        } else {
            cmds.add("-B" + toPath(build));
        }

        cmds.add("-S" + toPath(source));

        ProcessBuilder pb = new ProcessBuilder(cmds);


        pb.directory(source);

        return runProcess(pb, verbose);

    }

    public boolean make(File root, String... goal) throws IOException, InterruptedException, CMakeGenerateException {
        String make = "make";

        List<String> cmds = new Vector<>();
        if (autoNinja && hasNinja()) {
            cmds.add("ninja");
        } else {
            cmds.add(make);
            cmds.add("-j3");
        }

        cmds.add("-C");
        cmds.add(root.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmds);
        for (String string : goal) {
            pb.command().add(string);
        }

        return runProcess(pb, verbose);

    }

    public boolean run(File root, String cmd, boolean verbose) throws IOException, InterruptedException, CMakeGenerateException {

        String name = cmd;

        if (isWindows()) {
            name = name + ".exe";
        } else {
            name = "./" + name;
        }

        ProcessBuilder pb = new ProcessBuilder(name);
        pb.directory(root);

        return runProcess(pb, verbose);

    }

    public boolean isVerbose() {
        return verbose;
    }

    public CMakeUtil setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public static class CMakeGenerateException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        public final String errors;

        public CMakeGenerateException(String errors) {
            super(errors);
            this.errors = errors;
        }
    }
}
