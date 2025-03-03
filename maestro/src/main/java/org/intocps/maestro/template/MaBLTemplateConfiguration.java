package org.intocps.maestro.template;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.IPluginConfiguration;

import java.util.List;
import java.util.Map;

public class MaBLTemplateConfiguration {
    private Map<String, List<String>> logLevels;
    private Fmi2SimulationEnvironment simulationEnvironment;
    private Framework framework;
    private Pair<Framework, Fmi2SimulationEnvironmentConfiguration> frameworkConfig;
    private Pair<Boolean, String> initialize;
    private boolean loggingOn = false;
    private boolean visible = false;
    private String faultInjectionConfigurationPath;
    private IPluginConfiguration stepAlgorithmConfig;


    public Integer getWebsocketPort() {
        return websocketPort;
    }

    private Integer websocketPort;

    private MaBLTemplateConfiguration() {
    }

    public IPluginConfiguration getStepAlgorithmConfig() {
        return stepAlgorithmConfig;
    }

    public String getFaultInjectionConfigurationPath() {
        return faultInjectionConfigurationPath;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public Framework getFramework() {
        return this.framework;
    }

    public Pair<Framework, Fmi2SimulationEnvironmentConfiguration> getFrameworkConfig() {
        return this.frameworkConfig;
    }

    public Fmi2SimulationEnvironment getUnitRelationship() {
        return this.simulationEnvironment;
    }

    public Pair<Boolean, String> getInitialize() {
        return this.initialize;
    }

    public Map<String, List<String>> getLogLevels() {
        return this.logLevels;
    }

    public boolean getLoggingOn() {
        return this.loggingOn;
    }

    public boolean getVisible() {
        return this.visible;
    }

    public static class MaBLTemplateConfigurationBuilder {
        private boolean loggingOn = false;
        private boolean visible = false;
        private Fmi2SimulationEnvironment simulationEnvironment = null;
        private Pair<Boolean, String> initialize = Pair.of(false, null);
        private Map<String, List<String>> logLevels;
        private Framework framework;
        private Pair<Framework, Fmi2SimulationEnvironmentConfiguration> frameworkConfig;
        private IPluginConfiguration stepAlgorithmConfig;

        public Integer getWebsocketPort() {
            return websocketPort;
        }

        public void setWebsocketPort(Integer websocketPort) {
            this.websocketPort = websocketPort;
        }

        private Integer websocketPort;

        public static MaBLTemplateConfigurationBuilder getBuilder() {
            return new MaBLTemplateConfigurationBuilder();
        }

        public static String getFmuInstanceFromFmuKeyInstance(String fmuKeyInstance) {
            // 2 due to: } and .
            return fmuKeyInstance.substring(fmuKeyInstance.indexOf('}') + 2);
        }

        public boolean isLoggingOn() {
            return loggingOn;
        }

        public MaBLTemplateConfigurationBuilder setLoggingOn(boolean loggingOn) {
            this.loggingOn = loggingOn;
            return this;
        }

        public boolean isVisible() {
            return visible;
        }

        public MaBLTemplateConfigurationBuilder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setFramework(Framework framework) {
            this.framework = framework;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setStepAlgorithmConfig(IPluginConfiguration config) {
            this.stepAlgorithmConfig = config;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setFrameworkConfig(Framework framework,
                Fmi2SimulationEnvironmentConfiguration configuration) throws Exception {
            this.frameworkConfig = Pair.of(framework, configuration);
            this.simulationEnvironment = Fmi2SimulationEnvironment.of(configuration, new IErrorReporter.SilentReporter());
            return this;
        }

        public MaBLTemplateConfigurationBuilder setFrameworkConfig(Framework framework, Fmi2SimulationEnvironmentConfiguration configuration,
                Fmi2SimulationEnvironment simulationEnvironment) {
            this.frameworkConfig = Pair.of(framework, configuration);
            this.simulationEnvironment = simulationEnvironment;
            return this;
        }

        public MaBLTemplateConfigurationBuilder useInitializer(boolean useInitializer, String config) {
            this.initialize = Pair.of(useInitializer, config);
            return this;
        }

        /**
         * @param logLevels Map from instance name to log levels.
         * @return
         */
        public MaBLTemplateConfigurationBuilder setLogLevels(Map<String, List<String>> logLevels) {
            this.logLevels = logLevels;
            return this;
        }

        public MaBLTemplateConfiguration build() {
            //FIXME validate
            MaBLTemplateConfiguration config = new MaBLTemplateConfiguration();
            config.initialize = this.initialize;
            config.logLevels = this.logLevels;
            config.framework = this.framework;
            config.frameworkConfig = this.frameworkConfig;
            config.simulationEnvironment = this.simulationEnvironment;
            config.loggingOn = this.loggingOn;
            config.visible = this.visible;
            config.websocketPort = this.websocketPort;
            config.faultInjectionConfigurationPath = config.simulationEnvironment.getFaultInjectionConfigurationPath();
            config.stepAlgorithmConfig = this.stepAlgorithmConfig;

            return config;
        }
    }
}
