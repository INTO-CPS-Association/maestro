package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

import java.util.ArrayList;
import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class PortFmi2Api implements Fmi2Builder.Port {

    public final ComponentVariableFmi2Api aMablFmi2ComponentAPI;
    public final Fmi2ModelDescription.ScalarVariable scalarVariable;
    private final List<PortFmi2Api> targetPorts = new ArrayList<>();
    private VariableFmi2Api sharedAsVariable;
    private PortFmi2Api sourcePort;

    public PortFmi2Api(ComponentVariableFmi2Api aMablFmi2ComponentAPI, Fmi2ModelDescription.ScalarVariable scalarVariable) {

        this.aMablFmi2ComponentAPI = aMablFmi2ComponentAPI;
        this.scalarVariable = scalarVariable;
    }

    @Override
    public String toString() {
        return "Port( '" + aMablFmi2ComponentAPI.getName() + "." + scalarVariable.getName() + "' , '" + scalarVariable.getType().type + "')";
    }

    public VariableFmi2Api getSharedAsVariable() {
        return sharedAsVariable;
    }

    public void setSharedAsVariable(VariableFmi2Api sharedAsVariable) {
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

        if (this.scalarVariable.causality != Fmi2ModelDescription.Causality.Output) {
            throw new PortLinkException("Can only link output ports. This port is: " + this.scalarVariable.causality, this);
        }

        for (Fmi2Builder.Port receiver : receivers) {
            PortFmi2Api receiverPort = (PortFmi2Api) receiver;

            if (receiverPort.scalarVariable.causality != Fmi2ModelDescription.Causality.Input) {
                throw new PortLinkException("Receivers must be input ports. This receiver is: " + receiverPort.scalarVariable.causality,
                        receiverPort);
            }

            if (receiverPort.getSourcePort() != null) {
                throw new PortLinkException("Cannot port already linked please break link first", receiver);
            }
            receiverPort.sourcePort = this;
            if (!this.targetPorts.contains(receiverPort)) {
                this.targetPorts.add(receiverPort);
            }
        }
    }

    public PortFmi2Api getSourcePort() {
        return this.sourcePort;
    }

    @Override
    public void breakLink() {
        sourcePort = null;
    }

    public String toLexName() {
        return this.aMablFmi2ComponentAPI.getOwner().getName() + "_" + this.aMablFmi2ComponentAPI.getName() + "_" + this.getName();
    }

    public String getMultiModelScalarVariableName() {
        return this.aMablFmi2ComponentAPI.getOwner().getFmuIdentifier() + "." + this.aMablFmi2ComponentAPI.getName() + "." + this.getName();
    }

    public List<PortFmi2Api> getTargetPorts() {
        return this.targetPorts;
    }
}
