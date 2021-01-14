package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablFmu2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class AMaBLVariableCreator implements Fmi2Builder.VariableCreator {

    //private final IBasicScopeBundle scopeBundle;
    //private final Fmi2SimulationEnvironment simEnv;
    private final IMablScope scope;
    private final MablApiBuilder builder;
    // Supplier<AMaBLScope> scopeSupplier; // In some cases this is parent scope, in other cases it is current.

    public AMaBLVariableCreator(IMablScope scope, MablApiBuilder builder) {
        //  this.scopeBundle = bundle;
        // this.scopeSupplier = scopeSupplier;
        this.scope = scope;
        this.builder = builder;
        // this.simEnv = simEnv;
    }

    //  public static AMablBooleanVariable createBoolean(String label, IMablScope scope) {
       /* var name = label;
        var type = newABoleanPrimitiveType();

        scope.add(newVariable(label, type));
        AMablVariable var = new AMablVariable(name, type, scope, new AMaBLVariableLocation.BasicPosition());
        scope.addVariable(var);

        scope.s return var;*/
    //    return (AMablBooleanVariable)scope.store(new AMablValue<>(newABoleanPrimitiveType(), null));
    //}


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

    public static AMablVariable createVariableForPort(TagNameGenerator nameGenerator, AMablPort port, IMablScope scope,
            Fmi2Builder.DynamicActiveScope<PStm> dynamicScope) {
        var name = nameGenerator.getName(port.toLexName());
        var type = MableAstFactory.newAArrayType(FMITypeToMablType(port.scalarVariable.type.type));
        var size = 1;
        PStm stm = MableBuilder.newVariable(name, type, size);
        scope.add(stm);
        AMablVariable variable = new AMablVariable(stm, type, scope, dynamicScope,
                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(0)),
                newAArrayIndexExp(newAIdentifierExp(name), Arrays.asList(newAIntLiteralExp(0))));
        return variable;
    }

    public static AMablFmu2Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope, String name,
            ModelDescription modelDescription, URI uriPath, IMablScope scope,
            Fmi2SimulationEnvironment simulationEnvironment) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        String uniqueName = nameGenerator.getName(name);

        AMablFmu2Api aMablFmu2Api = new AMablFmu2Api(uniqueName, builder, new ModelDescriptionContext(modelDescription));


        PStm var = newVariable(uniqueName, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(modelDescription.getGuid()), newAStringLiteralExp(path)));
        scope.add(var);

        AMablVariable<AMablFmu2Api> variable = new AMablVariable<>(var, MableAstFactory.newANameType("FMI2"), scope, dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(uniqueName)), newAIdentifierExp(uniqueName));
        aMablFmu2Api.setVariable(variable);

        //scope.addVariable(aMablFmu2Api, variable);
        return aMablFmu2Api;
    }

    // CreateFMU is a root-level function and therefore located in the VariableCreator.
    @Override
    public AMablFmu2Api createFMU(String name, ModelDescription modelDescription,
            URI uriPath) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        return createFMU(builder, builder.nameGenerator, builder.getDynamicScope(), name, modelDescription, uriPath, scope,
                builder.getSimulationEnvironment());
    }

   /* @Override
    public AMablBooleanVariable createBoolean(String label) {
        return createBoolean(label, scope);
    }*/


    //
    public AMablVariable createVariableForPort(AMablPort port) {
        return createVariableForPort(builder.nameGenerator, port, scope, builder.getDynamicScope());
    }
}
