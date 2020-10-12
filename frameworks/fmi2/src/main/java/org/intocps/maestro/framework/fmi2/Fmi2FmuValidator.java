package org.intocps.maestro.framework.fmi2;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.VDMSL;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.Interpreter;
import fmi2vdm.FMI2SaxHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.vdm.annotations.ast.ASTOnFailAnnotation;
import org.intocps.maestro.framework.fmi2.vdm.annotations.in.INOnFailAnnotation;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Fmi2FmuValidator implements IFmuValidator {

    /**
     * This methos
     *
     * @param path
     * @param reporter
     * @throws Exception
     */
    @Override
    public boolean validate(String id, URI path, IErrorReporter reporter) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FMI2SaxHandler handler = new FMI2SaxHandler(path.getPath(), "var");
            IFmu fmu = FmuFactory.create(null, path);
            try {
                //check schema
                ModelDescription md = new ModelDescription(fmu.getModelDescription());

                //convert to VDM
                saxParser.parse(fmu.getModelDescription(), handler);


                String vdm = handler.getFMIModelDescription().toVDM("\t");

                String annotationsSearchClassPath = System.getProperty("vdmj.annotations",
                        "com.fujitsu.vdmj.ast.annotations");//"org.intocps.maestro.framework.fmi2.vdm.annotations.ast")
                if (!annotationsSearchClassPath.contains(ASTOnFailAnnotation.class.getPackage().getName())) {
                    annotationsSearchClassPath = annotationsSearchClassPath + ";" + ASTOnFailAnnotation.class.getPackage().getName();
                }
                //We need to lock on VDMJ as it uses lots of static references and thus cannot run in parallel
                synchronized (VDMJ.class) {
                    synchronized (Settings.class) {
                        System.setProperty("vdmj.annotations", annotationsSearchClassPath);
                        System.setProperty("vdmj.mappingpath", "/maestro/fmi2/vdm");
                        VDMJ controller = new VDMSL();
                        controller.setQuiet(true);
                        Settings.dialect = Dialect.VDM_SL;
                        Settings.verbose = false;
                        Settings.annotations = true;

                        String[] fmi2StaticModelFiles =
                                new String[]{"CoSimulation_4.3.1.vdmsl", "DefaultExperiment_2.2.5.vdmsl", "FMIModelDescription_2.2.1" + ".vdmsl",
                                        "LogCategories_2.2.4.vdmsl", "Misc.vdmsl", "ModelExchange_3.3.1.vdmsl", "ModelStructure_2.2.8.vdmsl",
                                        "ModelVariables_2.2" + ".7.vdmsl", "TypeDefinitions_2.2.3.vdmsl", "UnitDefinitions_2.2.2.vdmsl",
                                        "VariableNaming_2.2.9.vdmsl", "VendorAnnotations_2.2.6.vdmsl"};

                        List<File> specFiles = Arrays.stream(fmi2StaticModelFiles).map(specPath -> {
                            File tmp;
                            try {
                                tmp = File.createTempFile("fmi2Spec", "");
                                try (OutputStream dest = new FileOutputStream(tmp); InputStream in = ClassLoader
                                        .getSystemResourceAsStream(specPath)) {
                                    if (in != null) {
                                        IOUtils.copy(in, dest);
                                    }
                                }
                                return tmp;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }).collect(Collectors.toList());

                        File modelSped = File.createTempFile("fmi2", "");
                        FileUtils.write(modelSped, vdm, StandardCharsets.UTF_8);

                        specFiles.add(modelSped);
                        synchronized (INOnFailAnnotation.class) {
                            controller.parse(specFiles);
                            if (controller.typeCheck() == ExitStatus.EXIT_OK) {

                                INOnFailAnnotation.failures.clear();
                                Interpreter interpreter = controller.getInterpreter();
                                interpreter.init();
                                interpreter.execute("isValidFMIModelDescription(var)");

                                boolean success = INOnFailAnnotation.failures.isEmpty();
                                INOnFailAnnotation.failures.forEach(msg -> reporter.warning(0, msg + " URI: " + path, null));

                                //clean up
                                INOnFailAnnotation.failures.clear();

                                for (File specFile : specFiles) {
                                    FileUtils.deleteQuietly(specFile);
                                }
                                //FIXME we need to have better control over this
                                return true;
                            } else {

                                reporter.warning(999, "Internal error could not check spec for: " + path, null);
                                return false;
                            }
                        }
                    }
                }
            } finally {

                fmu.unLoad();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
