package org.intocps.maestro.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JacobianStepConfig implements IPluginConfiguration {
    public Set<String> variablesOfInterest = new HashSet<>();
    public boolean stabilisation = false;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
    public boolean simulationProgramDelay;
    public boolean setGetDerivatives;
    public String variableStepAlgorithm;


    public static class VariableStepAlgorithm{

        final Double[] size;
        final Double initsize;
        final Map<String, IVarStepConstraint> constraints;

        public VariableStepAlgorithm(Double[] size, Double initsize, Map<String, IVarStepConstraint> constraints) {
            this.size = size;
            this.initsize = initsize;
            this.constraints = constraints;
        }
    }

    public interface IVarStepConstraint {

    }

    public static class SamplingConstraint implements IVarStepConstraint {
        Integer base;
        Integer rate;
        Integer startTime;

        public SamplingConstraint() {
        }

        public SamplingConstraint(Integer base, Integer rate, Integer startTime) {
            this.base = base;
            this.rate = rate;
            this.startTime = startTime;
        }

        public Integer getBase() {
            return base;
        }

        public Integer getRate() {
            return rate;
        }

        public Integer getStartTime() {
            return startTime;
        }

    }


    public static class FmuMaxStepSizeConstraint implements IVarStepConstraint {

    }


    public static class BoundedDifferenceConstraint implements IVarStepConstraint {
        List<String> ports;
        Double reltol;
        Double abstol;
        Double safety;
        Boolean skipDiscrete;

        public BoundedDifferenceConstraint() {
        }

        public BoundedDifferenceConstraint(List<String> ports, Double reltol, Double abstol, Double safety, Boolean skipDiscrete) {
            this.ports = ports;
            this.reltol = reltol;
            this.abstol = abstol;
            this.safety = safety;
            this.skipDiscrete = skipDiscrete;
        }

        public List<String> getPorts() {
            return ports;
        }

        public Double getReltol() {
            return reltol;
        }

        public Double getAbstol() {
            return abstol;
        }

        public Double getSafety() {
            return safety;
        }

        public Boolean getSkipDiscrete() {
            return skipDiscrete;
        }

    }

    public static class ZeroCrossingConstraint implements IVarStepConstraint {
        List<String> ports;
        Integer order;
        Double abstol;
        Double safety;

        public ZeroCrossingConstraint() {
        }

        public ZeroCrossingConstraint(List<String> ports, Integer order, Double abstol, Double safety) {
            this.ports = ports;
            this.order = order;
            this.abstol = abstol;
            this.safety = safety;
        }

        public List<String> getPorts() {
            return ports;
        }

        public Integer getOrder() {
            return order;
        }

        public Double getAbstol() {
            return abstol;
        }

        public Double getSafety() {
            return safety;
        }

    }
}
