package org.intocps.maestro.codegen.mabl2cpp;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MablCppCodeGenerator {

    final File outputDirectory;

    public MablCppCodeGenerator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public List<File> generate(INode spec, Map<INode, PType> types) throws IOException, AnalysisException {
        this.outputDirectory.mkdirs();

        Map<String, String> sources = CppPrinter.print(spec, types);


        for (Map.Entry<String, String> source : sources.entrySet()) {
            IOUtils.write(source.getValue(), new FileOutputStream(new File(outputDirectory, source.getKey())), StandardCharsets.UTF_8);
        }


        copyLibraries(this.outputDirectory);
        createCMakeProject(this.outputDirectory);

        return sources.keySet().stream().map(name -> new File(outputDirectory, name)).collect(Collectors.toList());
    }

    private void createCMakeProject(File outputDirectory) throws IOException {
        InputStream is = this.getClass().getResourceAsStream("CMakeLists.txt");
        String cmakeLists = IOUtils.toString(is, StandardCharsets.UTF_8);
        //TODO replace what ever
        IOUtils.write(cmakeLists, new FileOutputStream(new File(outputDirectory, "CMakeLists.txt")), StandardCharsets.UTF_8);
        org.apache.commons.io.IOUtils
                .copy(this.getClass().getResourceAsStream("main.cpp"), new FileOutputStream(new File(outputDirectory, "main" + ".cpp")));
        org.apache.commons.io.IOUtils
                .copy(this.getClass().getResourceAsStream("readme.md"), new FileOutputStream(new File(outputDirectory, "readme.md")));

    }

    private void copyLibraries(File outputDirectory) throws IOException {


        String[] libraries = {"DataWriter", "Logger", "SimFmi2", "SimMath", "MEnv", "BooleanLogic", "DataWriterConfig", "unzip", "FmiComponentState",
                "VariableStep", "VariableStepConfig"};

        for (String libraryName : libraries) {
            for (String ext : new String[]{"cpp", "h"}) {
                String name = libraryName + "." + ext;
                InputStream is = this.getClass().getResourceAsStream("libs/" + name);
                File libs = new File(outputDirectory, "libs");
                libs.mkdirs();
                if (is != null) {
                    org.apache.commons.io.IOUtils.copy(is, new FileOutputStream(new File(libs, name)));
                }
            }
        }
    }
}
