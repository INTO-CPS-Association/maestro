package org.intocps.maestro.cli;

import org.intocps.maestro.Main;
import picocli.CommandLine;

import java.io.InputStream;
import java.util.Properties;

public class MablCmdVersionProvider implements CommandLine.IVersionProvider {


    @Override
    public String[] getVersion() throws Exception {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("maestro.properties");
            prop.load(coeProp);
            return new String[]{"${COMMAND-FULL-NAME} version " + prop.getProperty("version")};
        } catch (Exception e) {
            return new String[]{e.getMessage()};
        }
    }
}
