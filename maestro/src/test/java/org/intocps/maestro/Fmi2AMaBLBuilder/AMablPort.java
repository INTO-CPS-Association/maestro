package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AMablPort implements Fmi2Builder.Port {
    public final AMablFmi2ComponentAPI aMablFmi2ComponentAPI;
    public final ModelDescription.ScalarVariable scalarVariable;
    public AMablVariable relatedVariable;
    private List<AMablPort> companionInputPorts;
    private AMablPort sourcePort;

    public AMablPort(AMablFmi2ComponentAPI aMablFmi2ComponentAPI, ModelDescription.ScalarVariable scalarVariable) {

        this.aMablFmi2ComponentAPI = aMablFmi2ComponentAPI;
        this.scalarVariable = scalarVariable;
    }

    @Override
    public String getName() {
        return this.scalarVariable.getName();
    }

    @Override
    public Long getPortReferenceValue() {
        return this.scalarVariable.getValueReference();
    }

    @Override
    public void linkTo(Fmi2Builder.Port... receiver) {
        this.companionInputPorts = Arrays.stream(receiver).map(x -> (AMablPort) x).collect(Collectors.toList());
        this.companionInputPorts.forEach(x -> x.addCompanionPort(this));
    }

    private void addCompanionPort(AMablPort aMablPort) {
        if (this.scalarVariable.causality == ModelDescription.Causality.Input) {
            if (this.sourcePort != null && this.sourcePort != aMablPort) {
                this.sourcePort.removeCompanionPort(this);
            }
            this.sourcePort = aMablPort;
        }
        if (this.scalarVariable.causality == ModelDescription.Causality.Output) {
            if (!this.companionInputPorts.stream().anyMatch(x -> x == aMablPort)) {
                this.companionInputPorts.add(aMablPort);
            }
        }
    }

    public AMablPort getSourcePort() {
        return this.sourcePort;
    }

    private void removeCompanionPort(AMablPort aMablPort) {
        if (this.scalarVariable.causality == ModelDescription.Causality.Output) {
            this.companionInputPorts.removeIf(x -> x == aMablPort);
        }
        if (this.scalarVariable.causality == ModelDescription.Causality.Input) {
            this.sourcePort = null;
        }

    }

    @Override
    public void breakLink(Fmi2Builder.Port... receiver) {
        AMablBuilder.breakLink(this, receiver);

    }

    public String toLexName() {
        return this.aMablFmi2ComponentAPI.getParent().getName() + "_" + this.aMablFmi2ComponentAPI.getName() + "_" + this.getName();
    }
}
