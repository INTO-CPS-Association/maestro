package org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface;

import org.intocps.fmi.*;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;

public class StatementFMU implements IFmu {
    IFmu actualFMU;
    String fmuName;
    ModelDescription md;
    Map<String, StatementFMIComponent> instances = new HashMap<>();
    StatementContainer container = StatementContainer.getInstance();
    URI uri;

    public StatementFMU(File fmuZipFile, URI uri) throws ParserConfigurationException, SAXException, IOException, FmuInvocationException, XPathExpressionException {

        this.uri = uri;
        actualFMU = org.intocps.fmi.jnifmuapi.Factory.create(fmuZipFile);

            md = new ModelDescription(actualFMU.getModelDescription());
            fmuName = md.getModelId();
    }

    @Override
    public void load() throws FmuInvocationException, FmuMissingLibraryException {
        container.createLoadStatement(fmuName, uri);
        // Create load statement
    }

    @Override
    public IFmiComponent instantiate(String guid, String instanceName, boolean visible, boolean loggingOn, IFmuCallback iFmuCallback) throws XPathExpressionException, FmiInvalidNativeStateException {
        StatementFMIComponent comp = new StatementFMIComponent(instanceName);
        instances.put(instanceName, comp);
        container.createInstantiateStatement(fmuName, instanceName, loggingOn);
        return comp;
    }

    @Override
    public void unLoad() throws FmiInvalidNativeStateException {

    }

    @Override
    public String getVersion() throws FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public String getTypesPlatform() throws FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public InputStream getModelDescription() throws ZipException, IOException {
        return actualFMU.getModelDescription();
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
