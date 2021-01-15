package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablFmu2Variable;
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

    private final IMablScope scope;
    private final MablApiBuilder builder;

    public AMaBLVariableCreator(IMablScope scope, MablApiBuilder builder) {
        this.scope = scope;
        this.builder = builder;
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

    public static AMablFmu2Variable createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, ModelDescription modelDescription, URI uriPath,
            IMablScope scope) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        String uniqueName = nameGenerator.getName(name);

        PStm var = newVariable(uniqueName, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(modelDescription.getGuid()), newAStringLiteralExp(path)));
        scope.add(var);

        AMablFmu2Variable fmuVar =
                new AMablFmu2Variable(builder, new ModelDescriptionContext(modelDescription), var, MableAstFactory.newANameType("FMI2"), scope,
                        dynamicScope, newAIdentifierStateDesignator(newAIdentifier(uniqueName)), newAIdentifierExp(uniqueName));

        return fmuVar;
    }

    // CreateFMU is a root-level function and therefore located in the VariableCreator.
    @Override
    public AMablFmu2Variable createFMU(String name, ModelDescription modelDescription,
            URI uriPath) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        return createFMU(builder, builder.nameGenerator, builder.getDynamicScope(), name, modelDescription, uriPath, scope);
    }


    public AMablVariable createVariableForPort(AMablPort port) {
        return createVariableForPort(builder.nameGenerator, port, scope, builder.getDynamicScope());
    }
}
