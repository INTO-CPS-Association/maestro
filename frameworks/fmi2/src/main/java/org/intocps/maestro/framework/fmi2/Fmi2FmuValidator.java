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
import java.util.List;

public class Fmi2FmuValidator implements IFmuValidator {
    final static Logger logger = LoggerFactory.getLogger(Fmi2FmuValidator.class);

//    static {
//        System.setProperty("vdmj.mapping.search_path", "/annotations");
//    }

    /**
     * returns true if validation could be performed. I.e. true does NOT indicate that no errors were found.
     *
     * @param id       validation id.
     * @param path     fmu path.
     * @param reporter error reporter.
     * @return indication if validation could be performed.
     */
    @Override
    public boolean validate(String id, URI path, IErrorReporter reporter) {
        try {
            logger.trace("Validating: {} at {}", id, path);
            IFmu fmu = FmuFactory.create(null, path);
            MaestroCheck checker = new MaestroCheck();
            List<OnFailError> onFailErrors = checker.check(fmu.getModelDescription());

            onFailErrors.forEach(onFailError -> {
                reporter.warning(onFailError.errno, onFailError.message, new LexToken(path + File.separator + "modelDescription" + ".xml", 0, 0));
            });

            return true;

        } catch (Exception e) {
            throw new RuntimeException("An exception occurred during validate in Fmi2FmUValidator", e);
        }
    }
}
