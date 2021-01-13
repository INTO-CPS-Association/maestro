package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.IBasicScopeBundle;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.function.Supplier;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class AMaBLVariableCreator implements Fmi2Builder.VariableCreator {

    private final IBasicScopeBundle scopeBundle;
    private final Fmi2SimulationEnvironment simEnv;
    Supplier<AMaBLScope> scopeSupplier; // In some cases this is parent scope, in other cases it is current.

    public AMaBLVariableCreator(IBasicScopeBundle bundle, Supplier<AMaBLScope> scopeSupplier, Fmi2SimulationEnvironment simEnv) {
        this.scopeBundle = bundle;
        this.scopeSupplier = scopeSupplier;
        this.simEnv = simEnv;
    }

    public static AMablVariable<Fmi2Builder.MBoolean> createBoolean(String label, AMaBLScope scope) {
        var name = label;
        var type = newABoleanPrimitiveType();

        scope.addStatement(newVariable(label, type));
        AMablVariable var = new AMablVariable(name, type, scope, new AMaBLVariableLocation.BasicPosition());
        scope.addVariable(var);
        return var;
    }

    public static PType FMITypeToMablType(ModelDescription.Types type) {
        switch (type) {
            case Boolean:
                return newABoleanPrimitiveType();
            case Real:
                return newARealNumericPrimitiveType();
            case Integer:
                return newAIntNumericPrimitiveType();
            case String:
                return newAStringPrimitiveType();
            default:
                throw new UnsupportedOperationException("Converting fmi type: " + type + " to mabl type is not supported.");
        }
    }

    public static AMablVariable createVariableForPort(AMablPort port, AMaBLScope scope) {
        var name = port.toLexName();
        var type = MableAstFactory.newAArrayType(FMITypeToMablType(port.scalarVariable.type.type));
        var size = 1;
        PStm stm = MableBuilder.newVariable(name, type, size);
        scope.addStatement(stm);
        AMablVariable variable = new AMablVariable(name, type, scope, new AMaBLVariableLocation.ArrayPosition(0));
        return variable;
    }

    public static AMablFmu2Api createFMU(String name, ModelDescription modelDescription, URI uriPath,
            AMaBLScope scope) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }
        AMablFmu2Api aMablFmu2Api =
                new AMablFmu2Api(name, scope.getSimulationEnvironment(), new ModelDescriptionContext(modelDescription), scope.getScopeBundle());

        AMablVariable<AMablFmu2Api> variable =
                new AMablVariable<>(name, MableAstFactory.newANameType("FMI2"), scope, new AMaBLVariableLocation.BasicPosition());

        aMablFmu2Api.setVariable(variable);
        PStm pStm = newVariable(name, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(modelDescription.getGuid()), newAStringLiteralExp(path)));
        scope.addStatement(pStm);
        scope.addVariable(aMablFmu2Api, variable);
        return aMablFmu2Api;
    }

    // CreateFMU is a root-level function and therefore located in the VariableCreator.
    @Override
    public AMablFmu2Api createFMU(String name, ModelDescription modelDescription,
            URI uriPath) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        return createFMU(name, modelDescription, uriPath, scopeSupplier.get());
    }

    @Override
    public AMablVariable<Fmi2Builder.MBoolean> createBoolean(String label) {
        return createBoolean(label, scopeSupplier.get());
    }

    @Override
    public Fmi2Builder.Variable<Fmi2Builder.MInt> createInteger(String label) {
        return null;
    }

    @Override
    public Fmi2Builder.Variable<Fmi2Builder.TimeDeltaValue> createTimeDeltaValue(String label) {
        return null;
    }

    public Fmi2Builder.Variable<Fmi2Builder.TimeDeltaValue> createTimeDeltaValue(String label, Double value) {
        return null;
    }

    @Override
    public Fmi2Builder.Variable<Fmi2Builder.MDouble> createDouble(String label) {
        return null;
    }

    @Override
    public Fmi2Builder.Variable<Fmi2Builder.Time> createTimeValue(String step_size) {
        return null;
    }

    //
    public AMablVariable createVariableForPort(AMablPort port) {
        return createVariableForPort(port, scopeSupplier.get());
    }
}
