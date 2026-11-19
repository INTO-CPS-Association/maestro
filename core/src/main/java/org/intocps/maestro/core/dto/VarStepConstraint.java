package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Variable-step constraint. Select the constraint kind using the type property.")

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = VarStepConstraint.BoundedDifferenceConstraint.class, name = "boundeddifference"),
        @JsonSubTypes.Type(value = VarStepConstraint.ZeroCrossingConstraint.class, name = "zerocrossing"),
        @JsonSubTypes.Type(value = VarStepConstraint.SamplingConstraint.class, name = "samplingrate"),
        @JsonSubTypes.Type(value = VarStepConstraint.FmuMaxStepSizeConstraint.class, name = "fmumaxstepsize")})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class VarStepConstraint {
    @JsonProperty
    @Schema(description = "Constraint kind: boundeddifference, zerocrossing, samplingrate, or fmumaxstepsize.")
    public String type;
    public abstract void validate() throws Exception;

    @Schema(description = "Sampling-rate constraint (type samplingrate).")
    public static class SamplingConstraint extends VarStepConstraint {
        @Schema(description = "Base of the sampling-rate constraint.")
        Integer base;
        @Schema(description = "Sampling rate relative to the base.")
        Integer rate;
        @Schema(description = "Start time of the sampling constraint.")
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

    @Schema(description = "FMU maximum-step-size constraint (type fmumaxstepsize).")
    public static class FmuMaxStepSizeConstraint extends VarStepConstraint {

        @Override
        public void validate() throws Exception {

        }
    }

    @Schema(description = "Bounded-difference constraint (type boundeddifference).")
    public static class BoundedDifferenceConstraint extends VarStepConstraint {
        @Schema(description = "Qualified signal names monitored by the constraint.")
        List<String> ports;
        @Schema(description = "Relative tolerance for the bounded difference.")
        Double reltol;
        @Schema(description = "Absolute tolerance for the bounded difference.")
        Double abstol;
        @Schema(description = "Safety factor used when adapting the step size.")
        Double safety;
        @Schema(description = "Whether to skip discrete signals.")
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

    @Schema(description = "Zero-crossing constraint (type zerocrossing).")
    public static class ZeroCrossingConstraint extends VarStepConstraint {
        @Schema(description = "Qualified signal names monitored for zero crossings.")
        List<String> ports;
        @Schema(description = "Prediction order for zero-crossing detection.")
        Integer order;
        @Schema(description = "Absolute tolerance for zero-crossing detection.")
        Double abstol;
        @Schema(description = "Safety factor used when adapting the step size.")
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
