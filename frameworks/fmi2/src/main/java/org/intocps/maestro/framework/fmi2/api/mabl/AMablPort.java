package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablFmi2ComponentAPI;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class AMablPort implements Fmi2Builder.Port {

    public final AMablFmi2ComponentAPI aMablFmi2ComponentAPI;
    public final ModelDescription.ScalarVariable scalarVariable;
    private AMablVariable sharedAsVariable;
    private List<AMablPort> companionInputPorts;
    private AMablPort sourcePort;

    public AMablPort(AMablFmi2ComponentAPI aMablFmi2ComponentAPI, ModelDescription.ScalarVariable scalarVariable) {

        this.aMablFmi2ComponentAPI = aMablFmi2ComponentAPI;
        this.scalarVariable = scalarVariable;
    }

    @Override
    public String toString() {
        return "Port( '" + aMablFmi2ComponentAPI.getName() + "." + scalarVariable.getName() + "' , '" + scalarVariable.getType().type + "')";
    }

    public AMablVariable getSharedAsVariable() {
        return sharedAsVariable;
    }

    public void setSharedAsVariable(AMablVariable sharedAsVariable) {
        this.sharedAsVariable = sharedAsVariable;
    }

    public PType getType() {
        switch (scalarVariable.getType().type) {

            case Boolean:
                return newBoleanType();
            case Real:
                return newRealType();
            case Integer:
                return newIntType();
            case String:
                return newStringType();
            case Enumeration:
            default:
                return null;
        }
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
    public void linkTo(Fmi2Builder.Port... receivers) throws PortLinkException {
        if (receivers == null || receivers.length == 0) {
            return;
        }

        if (this.scalarVariable.causality != ModelDescription.Causality.Output) {
            throw new PortLinkException("Can only link output ports. This port is: " + this.scalarVariable.causality, this);
        }

        for (Fmi2Builder.Port receiver : receivers) {
            AMablPort receiverPort = (AMablPort) receiver;

            if (receiverPort.scalarVariable.causality != ModelDescription.Causality.Input) {
                throw new PortLinkException("Receivers must be input ports. This receiver is: " + receiverPort.scalarVariable.causality,
                        receiverPort);
            }

            if (receiverPort.getSourcePort() != null) {
                throw new PortLinkException("Cannot port already linked please break link first", receiver);
            }
            receiverPort.sourcePort = this;
        }
        // this.companionInputPorts = Arrays.stream(receiver).map(x -> (AMablPort) x).collect(Collectors.toList());
        // this.companionInputPorts.forEach(x -> x.addCompanionPort(this));
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
    public void breakLink() {
        // MablApiBuilder.breakLink(this, receiver);
        sourcePort = null;
    }

    public String toLexName() {
        return this.aMablFmi2ComponentAPI.getParent().getName() + "_" + this.aMablFmi2ComponentAPI.getName() + "_" + this.getName();
    }
}
