package org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface;

import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.IFmuFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

public class StatementFactory implements IFmuFactory {
    @Override
    public boolean accept(URI uri) {
        return true;
    }

    @Override
    public IFmu instantiate(File file, URI uri) throws Exception {
        File fmuZipFile = new File(uri.getPath());

        return new StatementFMU(fmuZipFile, uri);
    }
}
