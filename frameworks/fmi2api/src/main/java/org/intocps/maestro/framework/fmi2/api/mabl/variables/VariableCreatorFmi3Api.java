package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AErrorStm;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class VariableCreatorFmi3Api {

    private final IMablScope scope;
    private final MablApiBuilder builder;

    public VariableCreatorFmi3Api(IMablScope scope, MablApiBuilder builder) {
        this.scope = scope;
        this.builder = builder;
    }


    public static PType fmitypetomabltype(Fmi2ModelDescription.Types type) {
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

    //    public static VariableFmi2Api createVariableForPort(TagNameGenerator nameGenerator, PortFmi2Api port, IMablScope scope,
    //            Fmi2Builder.DynamicActiveScope<PStm> dynamicScope) {
    //        var name = nameGenerator.getName(port.toLexName());
    //        var type = MableAstFactory.newAArrayType(fmitypetomabltype(port.scalarVariable.type.type));
    //        var size = 1;
    //        PStm stm = MableBuilder.newVariable(name, type, size);
    //        scope.add(stm);
    //        VariableFmi2Api variable = new VariableFmi2Api(stm, type, scope, dynamicScope,
    //                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(0)),
    //                newAArrayIndexExp(newAIdentifierExp(name), Arrays.asList(newAIntLiteralExp(0))));
    //        return variable;
    //    }

    public static FmuVariableFmi3Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, URI uriPath, IMablScope scope) throws Exception {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        IFmu fmu = FmuFactory.create(null, URI.create(path));
        //check schema. The constructor checks the schema
        Fmi3ModelDescription modelDescription = new Fmi3ModelDescription(fmu.getModelDescription());

        return createFMU(builder, nameGenerator, dynamicScope, name, modelDescription, uriPath, scope);
    }

    public static FmuVariableFmi3Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, String loaderName, String[] args, IMablScope scope) throws Exception {

        if (loaderName.equals("FMI3")) {
            return createFMU(builder, nameGenerator, dynamicScope, name, URI.create(args[0]), scope);
        } else if (loaderName.equals("JFMI3")) {
            return createFMU(builder, nameGenerator, dynamicScope, name, args[0], scope);
        }
        return null;
    }


    public static FmuVariableFmi3Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, String className, IMablScope scope) throws Exception {

        ALoadExp loadExp = newALoadExp(Arrays.asList(newAStringLiteralExp("JFMI3"), newAStringLiteralExp(className)));

        IFmu fmu = (IFmu) VariableCreatorFmi2Api.class.getClassLoader().loadClass(className).getConstructor().newInstance();
        final ModelDescriptionContext3 ctxt = new ModelDescriptionContext3(new Fmi3ModelDescription(fmu.getModelDescription()));
        return createFmu(builder, nameGenerator, dynamicScope, name, scope, loadExp, () -> ctxt,
                "FMU load failed on fmu: '%s' for classpath: '" + className + "'");
    }

    public static FmuVariableFmi3Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, Fmi3ModelDescription modelDescription, URI uriPath,
            IMablScope scope) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        ALoadExp loadExp = newALoadExp(Arrays.asList(newAStringLiteralExp("FMI3"), newAStringLiteralExp(modelDescription.getInstantiationToken()),
                newAStringLiteralExp(path)));

        final ModelDescriptionContext3 ctxt = new ModelDescriptionContext3(modelDescription);

        return createFmu(builder, nameGenerator, dynamicScope, name, scope, loadExp, () -> ctxt,
                "FMU load failed on fmu: '%s' for uri: '" + uriPath + "'");
    }


    private static FmuVariableFmi3Api createFmu(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, IMablScope scope, ALoadExp loadExp, Supplier<ModelDescriptionContext3> modelDescriptionSupplier,
            String loadErrorMsgWithNameStringArgument) {
        String uniqueName = nameGenerator.getName(name);

        PStm var = newVariable(uniqueName, newANameType("FMI3"), newNullExp());
        var assign = newAAssignmentStm(newAIdentifierStateDesignator(uniqueName), loadExp);

        var enclosingTryScope = scope.findParentScope(TryMaBlScope.class);
        if (enclosingTryScope == null) {
            throw new IllegalArgumentException("Call to load FMU is only allowed within a try scope");
        }

        enclosingTryScope.parent().addBefore(enclosingTryScope.getDeclaration(), var);
        scope.add(assign);

        FmuVariableFmi3Api fmuVar = new FmuVariableFmi3Api(name, builder, modelDescriptionSupplier.get(), var, MableAstFactory.newANameType("FMI3"),
                enclosingTryScope.parent(), dynamicScope, newAIdentifierStateDesignator(newAIdentifier(uniqueName)), newAIdentifierExp(uniqueName));

        enclosingTryScope.getFinallyBody().addAfterOrTop(null, newIf(newNotEqual(fmuVar.getReferenceExp().clone(), newNullExp()),
                newABlockStm(newExpressionStm(newUnloadExp(List.of(fmuVar.getReferenceExp().clone()))),
                        newAAssignmentStm(fmuVar.getDesignator().clone(), newNullExp())), null));

        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ScopeFmi2Api thenScope = scope.enterIf(new PredicateFmi2Api(newEqual(fmuVar.getReferenceExp().clone(), newNullExp()))).enterThen();

            builder.getLogger().error(thenScope, loadErrorMsgWithNameStringArgument, name);

            thenScope.add(new AErrorStm(newAStringLiteralExp(String.format(loadErrorMsgWithNameStringArgument, name))));

            thenScope.leave();
        }
        return fmuVar;
    }
}
