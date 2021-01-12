package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class AMablPort implements Fmi2Builder.Port {
    public final AMablFmi2ComponentAPI aMablFmi2ComponentAPI;
    public final ModelDescription.ScalarVariable scalarVariable;
    public AMablVariable relatedVariable;

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
        AMablBuilder.addLink(this, receiver);

    }

    @Override
    public void breakLink(Fmi2Builder.Port... receiver) {
        AMablBuilder.breakLink(this, receiver);

    }

    public String toLexName() {
        return this.aMablFmi2ComponentAPI.getParent().getName() + "_" + this.aMablFmi2ComponentAPI.getName() + "_" + this.getName();
    }
}
