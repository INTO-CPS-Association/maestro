package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

import java.util.ArrayList;
import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class PortFmi2Api implements Fmi2Builder.Port<Fmi2ModelDescription.ScalarVariable> {

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
    public String getQualifiedName() {
        return this.aMablFmi2ComponentAPI.getOwner().getFmuIdentifier() + "." + this.aMablFmi2ComponentAPI.getEnvironmentName() + "." +
                this.getName();
    }

    @Override
    public Fmi2ModelDescription.ScalarVariable getSourceObject() {
        return this.scalarVariable;
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
    public void linkTo(Fmi2Builder.Port<Fmi2ModelDescription.ScalarVariable>... receivers) throws PortLinkException {

        if (receivers == null || receivers.length == 0) {
            return;
        }

        if (this.scalarVariable.causality != Fmi2ModelDescription.Causality.Output) {
            throw new PortLinkException("Can only link output ports. This port is: " + this.scalarVariable.causality, this);
        }

        for (Fmi2Builder.Port<Fmi2ModelDescription.ScalarVariable> receiver : receivers) {
            PortFmi2Api receiverPort = (PortFmi2Api) receiver;

            if (receiverPort.scalarVariable.causality != Fmi2ModelDescription.Causality.Input) {
                throw new PortLinkException("Receivers must be input ports. This receiver is: " + receiverPort.scalarVariable.causality,
                        receiverPort);
            }

            // HEJ: TBD - This check fails with "already linked" in expansion since both rbmq fmus connect to single actuation
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
        if (sourcePort != null) {
            //delete this from the source port
            sourcePort.targetPorts.remove(this);
        }
        sourcePort = null;
    }

    @Override
    public boolean isLinked() {
        return isLinkedAsInputConsumer() || isLinkedAsOutputProvider();
    }

    @Override
    public boolean isLinkedAsOutputProvider() {
        return targetPorts.isEmpty();
    }

    @Override
    public boolean isLinkedAsInputConsumer() {
        return this.sourcePort != null;
    }

    public String toLexName() {
        return this.aMablFmi2ComponentAPI.getOwner().getName() + "_" + this.aMablFmi2ComponentAPI.getName() + "_" + this.getName();
    }

    public String getMultiModelScalarVariableName() {
        return getQualifiedName();
    }

    public String getMultiModelScalarVariableNameWithoutFmu() {
        return this.aMablFmi2ComponentAPI.getEnvironmentName() + "." + this.getName();
    }

    public List<PortFmi2Api> getTargetPorts() {
        return this.targetPorts;
    }
}
