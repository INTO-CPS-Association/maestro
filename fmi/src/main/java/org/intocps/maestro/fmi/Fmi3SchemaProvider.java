package org.intocps.maestro.fmi;

import org.intocps.fmi.jnifmuapi.xml.SchemaProvider;

import java.io.InputStream;

/**
 * This should be moved to the native place where Fmi3Schema is located
 */
@Deprecated
public class Fmi3SchemaProvider implements SchemaProvider {


    public InputStream getSchema() {
        return getSchema("fmi3ModelDescription.xsd");
    }

    public InputStream getSchema(String path) {


        InputStream is = org.intocps.fmi.jnifmuapi.fmi3.schemas.Fmi3Schema.class.getResourceAsStream(path);


        return is;

    }


}

