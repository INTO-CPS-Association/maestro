package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.InstanceVariableFmi3Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

import java.util.ArrayList;
import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class PortFmi3Api implements Fmi2Builder.Port<Fmi3ModelDescription.Fmi3ScalarVariable> {

    public final InstanceVariableFmi3Api aMablFmi3InstanceAPI;
    public final Fmi3ModelDescription.Fmi3ScalarVariable scalarVariable;
    private final List<PortFmi3Api> targetPorts = new ArrayList<>();
    private VariableFmi2Api sharedAsVariable;
    private PortFmi3Api sourcePort;


    // TODO model description fmi3
    public PortFmi3Api(InstanceVariableFmi3Api aMablFmi3InstanceAPI, Fmi3ModelDescription.Fmi3ScalarVariable scalarVariable) {

        this.aMablFmi3InstanceAPI = aMablFmi3InstanceAPI;
        this.scalarVariable = scalarVariable;
    }

    @Override
    public String toString() {
        return "Port( '" + aMablFmi3InstanceAPI.getName() + "." + scalarVariable.getVariable().getName() + "' , '" + scalarVariable.getVariable().getTypeIdentifier().name() + "')";
    }

    public VariableFmi2Api getSharedAsVariable() {
        return sharedAsVariable;
    }

    public void setSharedAsVariable(VariableFmi2Api sharedAsVariable) {
        this.sharedAsVariable = sharedAsVariable;
    }

    public PType getType() {
        switch (scalarVariable.getVariable().getTypeIdentifier()) {
//            case Boolean:
//                return newBoleanType();
//            case Real:
//                return newRealType();
//            case Integer:
//                return newIntType();
//            case String:
//                return newStringType();
//            case Enumeration:
            default:
                return null;
        }
    }

    @Override
    public String getQualifiedName() {
        return this.aMablFmi3InstanceAPI.getOwner().getFmuIdentifier() + "." + this.aMablFmi3InstanceAPI.getEnvironmentName() + "." +
                this.getName();
    }

    @Override
    public Fmi3ModelDescription.Fmi3ScalarVariable getSourceObject() {
        return this.scalarVariable;
    }

    @Override
    public String getName() {
        return this.scalarVariable.getVariable().getName();
    }

    @Override
    public Long getPortReferenceValue() {
        return this.scalarVariable.getVariable().getValueReferenceAsLong();
    }


    @Override
    public void linkTo(Fmi2Builder.Port<Fmi3ModelDescription.Fmi3ScalarVariable>... receivers) throws PortLinkException {

        if (receivers == null || receivers.length == 0) {
            return;
        }

        if (this.scalarVariable.getVariable().getCausality() != Fmi3Causality.Output) {
            throw new PortLinkException("Can only link output ports. This port is: " + this.scalarVariable.getVariable().getCausality(), this);
        }

        for (Fmi2Builder.Port receiver : receivers) {
            PortFmi3Api receiverPort = (PortFmi3Api) receiver;

            if (receiverPort.scalarVariable.getVariable().getCausality() != Fmi3Causality.Input) {
                throw new PortLinkException("Receivers must be input ports. This receiver is: " + receiverPort.scalarVariable.getVariable().getCausality(),
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

    public PortFmi3Api getSourcePort() {
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
        return this.aMablFmi3InstanceAPI.getOwner().getName() + "_" + this.aMablFmi3InstanceAPI.getName() + "_" + this.getName();
    }

    public String getMultiModelScalarVariableName() {
        return this.aMablFmi3InstanceAPI.getOwner().getFmuIdentifier() + "." + this.aMablFmi3InstanceAPI.getEnvironmentName() + "." +
                this.getName();
    }

    public String getMultiModelScalarVariableNameWithoutFmu() {
        return this.aMablFmi3InstanceAPI.getEnvironmentName() + "." + this.getName();
    }

    public List<PortFmi3Api> getTargetPorts() {
        return this.targetPorts;
    }
}

