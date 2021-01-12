package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
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

public abstract class AMaBLVariableCreator implements Fmi2Builder.VariableCreator {
    private final Fmi2SimulationEnvironment simEnv;
    private final Supplier<AMaBLScope> scopeSupplier;
    AMaBLScope.ScopeVariables scopeVariables;


    public AMaBLVariableCreator(Fmi2SimulationEnvironment simEnv, Supplier<AMaBLScope> scopeSupplier) {
        this.simEnv = simEnv;
        this.scopeSupplier = scopeSupplier;

    }

    @Override
    public AMablVariable<Fmi2Builder.MBoolean> createBoolean(String label) {
        var name = label;
        var type = newABoleanPrimitiveType();

        scopeSupplier.get().addStatement(newVariable(label, type));
        AMablVariable var = new AMablVariable(name, type, scopeSupplier.get(), new AMaBLVariableLocation.BasicPosition());
        scopeSupplier.get().addVariable(var);
        return var;
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

    @Override
    public AMablFmu2Api createFMU(String name, ModelDescription modelDescription,
            URI uriPath) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }
        AMablFmu2Api aMablFmu2Api = new AMablFmu2Api(name, this.simEnv, new ModelDescriptionContext(modelDescription));

        AMablVariable<AMablFmu2Api> variable =
                new AMablVariable<>(name, MableAstFactory.newANameType("FMI2"), this.scopeSupplier.get(), new AMaBLVariableLocation.BasicPosition());

        aMablFmu2Api.setVariable(variable);
        PStm pStm = newVariable(name, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(modelDescription.getGuid()), newAStringLiteralExp(path)));
        this.scopeSupplier.get().addStatement(pStm);
        this.scopeSupplier.get().addVariable(aMablFmu2Api, variable);
        return aMablFmu2Api;
    }

    @Override
    public AMablFmu2Api createFMU(String name, URI uri) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        return this.createFMU(name, simEnv.getModelDescription("name"), uri);
    }

    public AMablVariable<AMablPort> createVariableForPort(AMaBLScope scope, AMablPort port) {
        // Create the variable statement
        var name = createLexIdentifier(port);
        var arType = newAArrayType(FMITypeToMablType(port.scalarVariable.type.type));
        var arSize = 1;
        AVariableDeclaration var = newAVariableDeclaration(name, arType, arSize, null);
        PStm stm = newALocalVariableStm(var);
        scope.addStatement(stm);
        PType type = FMITypeToMablType(port.scalarVariable.type.type);
        AMablVariable<AMablPort> variable =
                new AMablVariable<>(name.getText(), newAArrayType(type), scope, new AMaBLVariableLocation.ArrayPosition(0));
        return variable;
    }

    private AMaBLPrimitiveTypes FMITypeToAMaBLType(ModelDescription.Types type) {
        switch (type) {
            case Boolean:
                return AMaBLPrimitiveTypes.MBOOLEAN;

            case Real:
                return AMaBLPrimitiveTypes.MDOUBLE;

            case Integer:
                return AMaBLPrimitiveTypes.MINT;

            case String:
                return AMaBLPrimitiveTypes.MSTRING;

            case Enumeration:
                return AMaBLPrimitiveTypes.MINT;
        }
        return null;
    }


    private LexIdentifier createLexIdentifier(AMablPort port) {
        return new LexIdentifier(port.toLexName(), null);

    }

    public PType FMITypeToMablType(ModelDescription.Types type) {
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

    public AMablVariable createVariableForPort(AMablPort port) {
        var name = port.toLexName();
        var type = MableAstFactory.newAArrayType(FMITypeToMablType(port.scalarVariable.type.type));
        var size = 1;
        PStm stm = MableBuilder.newVariable(name, type, size);
        scopeSupplier.get().addStatement(stm);
        AMablVariable variable = new AMablVariable(name, type, scopeSupplier.get(), new AMaBLVariableLocation.ArrayPosition(0));
        return variable;

    }
}
