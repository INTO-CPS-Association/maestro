package org.intocps.maestro.plugin;
import java.util.List;

class JacobianStepConfig implements IPluginConfiguration {
    public int endtime;
    public List<String> variablesOfInterest;
    public boolean stabilisation;
}
