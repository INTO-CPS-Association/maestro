package org.intocps.maestro.framework.fmi2;

import maestro.MaestroCheck;
import maestro.OnFailError;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.LexToken;
import org.intocps.maestro.core.messages.IErrorReporter;
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

            onFailErrors.forEach(onFailError -> {
                reporter.warning(onFailError.errno, onFailError.message, new LexToken(path + File.separator + "modelDescription" + ".xml", 0,
                        0));
                logger.trace(onFailError.toString());
            });

            return onFailErrors.isEmpty();

        } catch (Exception e) {
            logger.error("An unknown exception occured during Fmi2FmUValidator: ", e);
            return false;
        }
    }
}
