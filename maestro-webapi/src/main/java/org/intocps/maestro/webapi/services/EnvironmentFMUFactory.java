package org.intocps.maestro.webapi.services;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.framework.fmi2.IFmuFactory;
import org.intocps.maestro.webapi.AbortSimulationException;

import java.io.File;
import java.net.URI;

public class EnvironmentFMUFactory implements IFmuFactory {

    public final static String EnvironmentSchemeIdentificationId = "environment";
    public final static String EnvironmentComponentIdentificationId = "global";
    public final static String EnvironmentFmuName = "~env~";

    @Override
    public boolean accept(URI uri) {
        return uri.getScheme() != null && (uri.getScheme().equals(EnvironmentSchemeIdentificationId));
    }

    @Override
    public IFmu instantiate(File sessionRoot, URI uri) throws Exception {
        if (accept(uri)) {
            if (EnvironmentFMU.getInstance() != null) {
                return EnvironmentFMU.getInstance();
            }
            {
                throw new AbortSimulationException("Environment FMU has not instantiated");
            }
        } else {
            throw new AbortSimulationException("unable to handle instantiation of: " + uri);
        }
    }
}
