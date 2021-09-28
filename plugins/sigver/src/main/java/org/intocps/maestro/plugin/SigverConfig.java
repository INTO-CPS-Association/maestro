package org.intocps.maestro.plugin;

import java.util.Map;

public class SigverConfig implements IPluginConfiguration {
    public String masterModel;
    public Map<String, Object> parameters;
    public Double relTol;
    public Double absTol;
    public Integer convergenceAttempts;
}
