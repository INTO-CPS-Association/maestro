package org.intocps.maestro.framework.fmi2;

import maestro.MaestroCheck;
import maestro.OnFailError;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.vdm.annotations.ast.ASTOnFailAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Fmi2FmuValidator implements IFmuValidator {
    final static Logger logger = LoggerFactory.getLogger(Fmi2FmuValidator.class);

    @Override
    public boolean validate(String id, URI path, IErrorReporter reporter) {
        logger.warn("Fmi2FmuValidator disabled");
        try {
            logger.trace("Validating: {} at {}", id, path);

            //Set annotations class path: This is because it defines the mapping files for @OnFail.
            // If you omit this, then it will "work" but you won't get any errors returned except the "Errors Found" one.
            // It should be possible to move the mapping files to VDMCheck, but that may cause problems with the regular command-line VDMCheck

//            String annotationsSearchClassPath = System.getProperty("vdmj.annotations",
//                    "com.fujitsu.vdmj.ast.annotations");//"org.intocps.maestro.framework.fmi2.vdm.annotations.ast")
//            if (!annotationsSearchClassPath.contains(ASTOnFailAnnotation.class.getPackage().getName())) {
//                annotationsSearchClassPath = annotationsSearchClassPath + File.pathSeparator + ASTOnFailAnnotation.class.getPackage().getName();
//            }
//            System.setProperty("vdmj.annotations", annotationsSearchClassPath);
//            System.setProperty("vdmj.mappingpath", "/maestro/fmi2/vdm");

            IFmu fmu = FmuFactory.create(null, path);

            File tempDir = Files.createTempDirectory("ModelDescriptionDirTemp").toFile();
            tempDir.deleteOnExit();

            File modelDescriptionXML = Path.of(tempDir.toString(), "modelDescription.xml").toFile();

            java.nio.file.Files.copy(
                    fmu.getModelDescription(),
                    modelDescriptionXML.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            MaestroCheck checker = new MaestroCheck();
            List<OnFailError> onFailErrors = checker.check(modelDescriptionXML);

            for (OnFailError err : onFailErrors) {
                logger.trace(err.toString());
            }

            return onFailErrors.isEmpty();

        } catch (Exception e) {
            logger.error("An unknown exception occured during Fmi2FmUValidator: ", e);
            return false;
        }
    }

//    @Override
//    public boolean validate(String id, URI path, IErrorReporter reporter) {
//        logger.warn("Fmi2FmuValidator disabled");
//        try {
//            logger.trace("Validating: {} at {}", id, path);
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            SAXParser saxParser = factory.newSAXParser();
//            FMI2SaxHandler handler = new FMI2SaxHandler(path.getPath(), "var");
//            IFmu fmu = FmuFactory.create(null, path);
//            //check schema. The constructor checks the schema
//            new ModelDescription(fmu.getModelDescription());
//
//            logger.trace("Generate VDM specification for: {}", id);
//            //convert to VDM
//            saxParser.parse(fmu.getModelDescription(), handler);
//
//            String vdm = handler.getFMIModelDescription().toVDM("\t");
//
//            String annotationsSearchClassPath = System.getProperty("vdmj.annotations",
//                    "com.fujitsu.vdmj.ast.annotations");//"org.intocps.maestro.framework.fmi2.vdm.annotations.ast")
//            if (!annotationsSearchClassPath.contains(ASTOnFailAnnotation.class.getPackage().getName())) {
//                annotationsSearchClassPath = annotationsSearchClassPath + File.pathSeparator + ASTOnFailAnnotation.class.getPackage().getName();
//            }
//            //We need to lock on VDMJ as it uses lots of static references and thus cannot run in parallel
//            synchronized (VDMJ.class) {
//                synchronized (Settings.class) {
//                    System.setProperty("vdmj.annotations", annotationsSearchClassPath);
//                    System.setProperty("vdmj.mappingpath", "/maestro/fmi2/vdm");
//                    VDMJ controller = new VDMSL();
//                    controller.setQuiet(true);
//                    Settings.dialect = Dialect.VDM_SL;
//                    Settings.verbose = false;
//                    Settings.annotations = true;
//
//                    String[] fmi2StaticModelFiles =
//                            new String[]{"CoSimulation_4.3.1.vdmsl", "DefaultExperiment_2.2.5.vdmsl", "FMIModelDescription_2.2.1" + ".vdmsl",
//                                    "LogCategories_2.2.4.vdmsl", "Misc.vdmsl", "ModelExchange_3.3.1.vdmsl", "ModelStructure_2.2.8.vdmsl",
//                                    "ModelVariables_2.2" + ".7.vdmsl", "TypeDefinitions_2.2.3.vdmsl", "UnitDefinitions_2.2.2.vdmsl",
//                                    "VariableNaming_2.2.9.vdmsl", "VendorAnnotations_2.2.6.vdmsl"};
//
//                    logger.trace("Copying static standard specification for id: {}", id);
//                    List<File> specFiles = null;
//                    try {
//                        specFiles = Arrays.stream(fmi2StaticModelFiles).map(specPath -> {
//                            File tmp;
//                            try {
//                                tmp = File.createTempFile("fmi2Spec", "");
//                                try (OutputStream dest = new FileOutputStream(tmp); InputStream in = this.getClass().getClassLoader()
//                                        .getResourceAsStream(specPath)) {
//                                    if (in != null) {
//                                        IOUtils.copy(in, dest);
//                                    }
//                                }
//                                return tmp;
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                return null;
//                            }
//                        }).collect(Collectors.toList());
//
//                        File modelSped = File.createTempFile("fmi2", "");
//                        FileUtils.write(modelSped, vdm, StandardCharsets.UTF_8);
//
//                        specFiles.add(modelSped);
//                        synchronized (INOnFailAnnotation.class) {
//                            if (controller.parse(specFiles) == ExitStatus.EXIT_OK) {
//                                if (controller.typeCheck() == ExitStatus.EXIT_OK) {
//
//                                    INOnFailAnnotation.failures.clear();
//                                    Interpreter interpreter = controller.getInterpreter();
//                                    logger.trace("Initialize VDM interpreter and execute validation for id: {}", id);
//                                    interpreter.init();
//                                    Value result = interpreter.execute("isValidFMIModelDescription(var)").deref();
//
//                                    boolean success = false;
//                                    if (result instanceof BooleanValue) {
//                                        success = result.boolValue(null);
//                                    }
//
//                                    boolean hasAnnotations = !INOnFailAnnotation.failures.isEmpty();
//                                    logger.trace("Specification for id '{}', compliant = {}, annotation = {}", id, success, hasAnnotations);
//                                    INOnFailAnnotation.failures.forEach(msg -> reporter
//                                            .warning(0, msg, new LexToken(path.toString() + File.separator + "modelDescription" + ".xml", 0, 0)));
//
//                                    //clean up
//                                    INOnFailAnnotation.failures.clear();
//
//                                    for (File specFile : specFiles) {
//                                        FileUtils.deleteQuietly(specFile);
//                                    }
//                                    //FIXME we need to have better control over this
//                                    return true;
//                                } else {
//                                    logger.trace("Specification for id '{}' did not type check", id);
//                                    reporter.warning(999, "Internal error could not check spec for: " + path, null);
//                                    return false;
//                                }
//                            } else {
//                                logger.trace("Specification could not parse id '{}'", id);
//                                reporter.warning(999, "Internal error could not check spec for: " + path, null);
//                                return false;
//                            }
//                        }
//                    } catch (Exception e) {
//                        if (specFiles != null) {
//                            specFiles.forEach(f -> {
//                                FileUtils.deleteQuietly(f);
//                            });
//                        }
//                        logger.error("An exception occured during Fmi2FmUValidator: ", e);
//                        return false;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("An unknown exception occured during Fmi2FmUValidator: ", e);
//            return false;
//        }
//    }


}
