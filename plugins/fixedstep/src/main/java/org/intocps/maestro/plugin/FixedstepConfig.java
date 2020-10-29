package org.intocps.maestro.plugin;

class FixedstepConfig implements IPluginConfiguration {
    final int endTime;

    public FixedstepConfig(int endTime) {
        this.endTime = endTime;
    }
}
