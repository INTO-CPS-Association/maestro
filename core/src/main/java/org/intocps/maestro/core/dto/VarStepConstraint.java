package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;

import java.util.List;

@ApiModel(subTypes = {VarStepConstraint.BoundedDifferenceConstraint.class, VarStepConstraint.ZeroCrossingConstraint.class,
        VarStepConstraint.SamplingConstraint.class, VarStepConstraint.FmuMaxStepSizeConstraint.class}, discriminator = "type",
        description = "Simulation variable step algorithm constraint.", value = "VarStepConstraint")

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = VarStepConstraint.BoundedDifferenceConstraint.class, name = "boundeddifference"),
        @JsonSubTypes.Type(value = VarStepConstraint.ZeroCrossingConstraint.class, name = "zerocrossing"),
        @JsonSubTypes.Type(value = VarStepConstraint.SamplingConstraint.class, name = "samplingrate"),
        @JsonSubTypes.Type(value = VarStepConstraint.FmuMaxStepSizeConstraint.class, name = "fmumaxstepsize")})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class VarStepConstraint {
    @JsonProperty
    public String type;
    public abstract void validate() throws Exception;



    @ApiModel(parent = VarStepConstraint.class)
    public static class SamplingConstraint extends VarStepConstraint {
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

        @Override
        public void validate() throws Exception {

        }
    }

    @ApiModel(parent = VarStepConstraint.class)
    public static class FmuMaxStepSizeConstraint extends VarStepConstraint {

        @Override
        public void validate() throws Exception {

        }
    }

    @ApiModel(parent = VarStepConstraint.class)
    public static class BoundedDifferenceConstraint extends VarStepConstraint {
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

        @Override
        public void validate() throws Exception {

        }
    }

    @ApiModel(parent = VarStepConstraint.class)
    public static class ZeroCrossingConstraint extends VarStepConstraint {
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

        @Override
        public void validate() throws Exception {

        }
    }
}
