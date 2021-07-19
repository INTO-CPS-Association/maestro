package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.codegen.mabl2cpp.MablCppCodeGenerator;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.util.CMakeUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//@Disabled("Problem with cmake generated files and their permissions")
public class FullSpecCppTest extends FullSpecTest {
    public static final List<String> CACHE_FOLDERS = Arrays.asList("libzip", "rapidjson", "intocpsfmi-src");
    static final File baseProjectPath = Paths.get("target", FullSpecCppTest.class.getSimpleName(), "_base").toFile();

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
            File build = new File(baseProjectPath, "build");
            cMakeUtil.generate(baseProjectPath, build, null);
            if (CMakeUtil.hasMake()) {
                cMakeUtil.make(build);
            }
        }
    }

    @Override
    protected void postProcessSpec(File directory, File workingDirectory, Mabl mabl, ARootDocument spec) throws Exception {
        //        if (true || true) {
        //            return;
        //        }
        mabl.optimize();
        Map.Entry<Boolean, Map<INode, PType>> tc = mabl.typeCheck();
        Assumptions.assumeTrue(tc.getKey(), "TC should pass");

        File projectFolder = generateCpp(directory, workingDirectory, mabl, spec, tc.getValue());
        CMakeUtil cMakeUtil = new CMakeUtil().setVerbose(true);
        if (CMakeUtil.hasCmake()) {
            copyCache(baseProjectPath, projectFolder);
            File build = new File(projectFolder, "build");
            cMakeUtil.generate(projectFolder, build, projectFolder);
            if (CMakeUtil.hasMake()) {
                cMakeUtil.make(build, "install", "clean");
            }
        }
    }

    private void copyCache(File baseProjectPath, File projectFolder) throws IOException {

        //        for (Map.Entry<File, File> folderPair : CACHE_FOLDERS.stream().map(n -> Map.entry(new File(baseProjectPath, n), new File(projectFolder, n)))
        //                .collect(Collectors.toList())) {
        //            if (folderPair.getKey().exists() && !folderPair.getValue().exists()) {
        //                System.out.println("Linking ");
        //                Files.createSymbolicLink(folderPair.getValue().toPath(), folderPair.getKey().getAbsoluteFile().toPath());
        //                //                FileUtils.copyDirectory(folderPair.getKey(), folderPair.getValue());
        //            }
        //        }
    }

    private File generateCpp(File directory, File workingDirectory, Mabl mabl, ARootDocument spec,
            Map<INode, PType> tc) throws AnalysisException, IOException {
        File output = new File(workingDirectory, "cpp");
        output.mkdirs();
        new MablCppCodeGenerator(output).generate(spec, tc);
        return output;
    }
}
