package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.codegen.mabl2cpp.MablCppCodeGenerator;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.util.CMakeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FullSpecCppTest extends FullSpecTest {
    public static final List<String> CACHE_FOLDERS = Arrays.asList("libzip", "rapidjson", "intocpsfmi-src");
    static final File baseProjectPath = Paths.get("target", FullSpecCppTest.class.getSimpleName(), "_base").toFile();
    static final File baseBuild = new File(baseProjectPath, "build");

    static final File baseSimProgram = getSimProgramFile(baseProjectPath);

    private static File getSimProgramFile(File baseProjectPath) {
        String name = "sim";
        if (CMakeUtil.isWindows()) {
            name += ".exe";
        }
        return new File(new File(baseProjectPath, "bin"), name);
    }

    @BeforeAll
    public static void configureBaseProject() throws Exception {

        if (CACHE_FOLDERS.stream().allMatch(n -> new File(baseProjectPath, n).exists())) {
            return;
        }

        IErrorReporter reporter = new ErrorReporter();
        Mabl mabl = new Mabl(baseProjectPath, baseProjectPath);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);
        File spec = new File(baseProjectPath, "spec.mabl");
        FileUtils.write(spec, "simulation" + "{}", StandardCharsets.UTF_8);
        mabl.parse(Arrays.asList(spec));

        new MablCppCodeGenerator(baseProjectPath).generate(mabl.getMainSimulationUnit(), mabl.typeCheck().getValue());
        CMakeUtil cMakeUtil = new CMakeUtil().setVerbose(true);
        if (CMakeUtil.hasCmake()) {

            cMakeUtil.generate(baseProjectPath, baseBuild, baseProjectPath);
            if (CMakeUtil.hasMake()) {
                cMakeUtil.make(baseBuild);
            }
        }
    }


    @Override
    protected void postProcessSpec(String name, File directory, File workingDirectory, Mabl mabl, ARootDocument spec) throws Exception {
        mabl.optimize();
        Map.Entry<Boolean, Map<INode, PType>> tc = mabl.typeCheck();
        Assumptions.assumeTrue(tc.getKey(), "TC should pass");

        Map.Entry<File, List<File>> locations = generateCpp(directory, workingDirectory, mabl, spec, tc.getValue());

        //so recompiling with cmake is too slow so we reuse the base project for this
        //copy spec
        for (File file : locations.getValue()) {
            Path dest = new File(baseProjectPath, file.getName()).toPath();
            System.out.println("Copying: " + file.getPath() + " -> " + dest);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        }

        CMakeUtil cMakeUtil = new CMakeUtil().setVerbose(true);
        if (CMakeUtil.hasCmake()) {
            if (baseSimProgram.exists()) {
                baseSimProgram.delete();
            }

            if (new File(new File(baseBuild, "CMakeFiles"), "sim.dir").exists()) {
                for (File ofile : Objects.requireNonNull(
                        new File(new File(baseBuild, "CMakeFiles"), "sim.dir").listFiles(pathname -> pathname.getName().endsWith(".o")))) {
                    ofile.delete();
                }
            }

            if (CMakeUtil.hasMake()) {
                cMakeUtil.make(baseBuild, "install");
            }
            Assertions.assertTrue(baseSimProgram.exists(), "Sim program was not produced!");

            File simProjectSimFile = new File(new File(locations.getKey(), "bin"), baseSimProgram.getName());
            simProjectSimFile.getParentFile().mkdirs();

            Files.copy(baseSimProgram.toPath(), simProjectSimFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            //lets check if the program generated is for this spec
            String specSha1 = DigestUtils.sha1Hex(PrettyPrinter.print(spec));

            System.out.println("Check SHA1 expecting: " + specSha1);
            ProcessBuilder pb = new ProcessBuilder(simProjectSimFile.getAbsolutePath(), "-sha1", specSha1);


            Assertions.assertTrue(CMakeUtil.runProcess(pb, true), "The sha1 of the generated spec and the sim program did not match.");

            if (name.equals("initialize_jacobianstepbuilder_unfold_loop")) {
                Assumptions.assumeTrue(false);
            }

            //lets run the spec
            File runtimeFile = new File(workingDirectory, "spec.runtime.json");
            File runtimeFileTest = new File(workingDirectory, "spec.runtime.test.json");
            if (runtimeFile.exists()) {
                Map runtimeMap = new ObjectMapper().readValue(runtimeFile, HashMap.class);
                if (runtimeMap.containsKey("DataWriter")) {
                    List dw = (List) runtimeMap.get("DataWriter");
                    for (Object writerObj : dw) {
                        Map writer = (Map) writerObj;
                        if (writer.containsKey("type") && writer.get("type").toString().equals("CSV")) {
                            writer.put("filename", new File(simProjectSimFile.getParentFile(), "output.csv").getAbsolutePath());
                        }
                    }
                }
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(runtimeFileTest, runtimeMap);
            }

            pb = new ProcessBuilder(simProjectSimFile.getAbsolutePath(), "-runtime", runtimeFileTest.getAbsolutePath());
            File simulationWorkingDir =
                    directory.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
            System.out.println("Simulation working dir: " + simulationWorkingDir);
            pb.directory(simulationWorkingDir);
            Assertions.assertTrue(CMakeUtil.runProcess(pb, true), "Simulation did not complete without errors");
        }

    }


    private Map.Entry<File, List<File>> generateCpp(File directory, File workingDirectory, Mabl mabl, ARootDocument spec,
            Map<INode, PType> tc) throws AnalysisException, IOException {
        File output = new File(workingDirectory, "cpp");
        output.mkdirs();
        List<File> files = new MablCppCodeGenerator(output).generate(spec, tc);
        return Map.entry(output, files);
    }
}
