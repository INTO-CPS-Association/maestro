package org.intocps.maestro.MaBLTemplateGenerator;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.IStepAlgorithm;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;

import java.util.List;
import java.util.Map;

public class MaBLTemplateConfiguration {
    private Map<String, List<String>> logLevels;
    private IStepAlgorithm stepAlgorithm;
    private FmiSimulationEnvironment unitRelationShip;
    private Framework framework;
    private Pair<Framework, String> frameworkConfig;
    private Pair<Boolean, String> initialize;

    private MaBLTemplateConfiguration() {
    }

    public Framework getFramework() {
        return framework;
    }

    public Pair<Framework, String> getFrameworkConfig() {
        return frameworkConfig;
    }

    public IStepAlgorithm getAlgorithm() {
        return this.stepAlgorithm;
    }

    public FmiSimulationEnvironment getUnitRelationship() {
        return unitRelationShip;
    }

    public Pair<Boolean, String> getInitialize() {
        return this.initialize;
    }

    public Map<String, List<String>> getLogLevels() {
        return this.logLevels;
    }

    public boolean getLoggingOn() {
        return this.unitRelationShip.getEnvironmentMessage().loggingOn;
    }

    public boolean getVisible() {
        return this.unitRelationShip.getEnvironmentMessage().visible;
    }

    public static class MaBLTemplateConfigurationBuilder {
        private IStepAlgorithm stepAlgorithm = null;
        private FmiSimulationEnvironment unitRelationship = null;
        private Pair<Boolean, String> initialize = Pair.of(false, null);
        private Map<String, List<String>> logLevels;
        private Framework framework;
        private Pair<Framework, String> frameworkConfig;

        public static MaBLTemplateConfigurationBuilder getBuilder() {
            return new MaBLTemplateConfigurationBuilder();
        }

        public MaBLTemplateConfigurationBuilder setFramework(Framework framework) {
            this.framework = framework;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setFrameworkConfig(Framework framework, String config) {
            this.frameworkConfig = Pair.of(framework, config);
            return this;
        }

        public MaBLTemplateConfigurationBuilder setUnitRelationship(FmiSimulationEnvironment unitRelationship) {
            this.unitRelationship = unitRelationship;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setStepAlgorithm(IStepAlgorithm stepAlgorithm) {
            if (stepAlgorithm != null) {
                this.stepAlgorithm = stepAlgorithm;
            }
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
            config.stepAlgorithm = this.stepAlgorithm;
            config.unitRelationShip = this.unitRelationship;
            config.initialize = this.initialize;
            config.logLevels = this.logLevels;
            config.framework = this.framework;
            config.frameworkConfig = this.frameworkConfig;
            return config;
        }
    }
}
