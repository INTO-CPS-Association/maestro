package org.intocps.maestro.MaBLTemplateGenerator;

import org.intocps.maestro.core.API.IStepAlgorithm;
import org.intocps.maestro.plugin.env.UnitRelationship;

public class MaBLTemplateConfiguration {


    private IStepAlgorithm stepAlgorithm;
    private UnitRelationship unitRelationShip;
    private boolean initialize;

    private MaBLTemplateConfiguration() {
    }

    public IStepAlgorithm getAlgorithm() {
        return this.stepAlgorithm;
    }

    public UnitRelationship getUnitRelationship() {
        return unitRelationShip;
    }

    public boolean getInitialize() {
        return this.initialize;
    }

    public static class MaBLTemplateConfigurationBuilder {


        private IStepAlgorithm stepAlgorithm = null;
        private UnitRelationship unitRelationship = null;
        private boolean initialize = false;

        public static MaBLTemplateConfigurationBuilder getBuilder() {
            return new MaBLTemplateConfigurationBuilder();
        }

        public MaBLTemplateConfigurationBuilder setUnitRelationship(UnitRelationship unitRelationship) {
            this.unitRelationship = unitRelationship;
            return this;
        }

        public MaBLTemplateConfigurationBuilder setStepAlgorithm(IStepAlgorithm stepAlgorithm) {
            if (stepAlgorithm != null) {
                this.stepAlgorithm = stepAlgorithm;
            }
            return this;
        }

        public MaBLTemplateConfigurationBuilder useInitializer(boolean useInitializer) {
            this.initialize = useInitializer;
            return this;
        }

        public MaBLTemplateConfiguration build() {
            MaBLTemplateConfiguration templateConfiguration = new MaBLTemplateConfiguration();
            templateConfiguration.stepAlgorithm = this.stepAlgorithm;
            templateConfiguration.unitRelationShip = this.unitRelationship;
            templateConfiguration.initialize = this.initialize;
            return templateConfiguration;
        }
    }
}
