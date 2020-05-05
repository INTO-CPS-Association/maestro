package org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface;

import jdk.nashorn.internal.runtime.regexp.JoniRegExp;
import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.IFmuFactory;

import java.io.File;
import java.net.URI;

public class StatementFactory implements IFmuFactory {
    @Override
    public boolean accept(URI uri) {
        return true;
    }

    @Override
    public IFmu instantiate(File file, URI uri) throws Exception {
        String rawPath = uri.getRawPath();
        System.out.println("Path: " + rawPath);
        File fmuZipFile = new File(rawPath);

        return new StatementFMU(fmuZipFile, uri);
    }
}
