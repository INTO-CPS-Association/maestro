package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class VariableCreatorFmi2Api {

    private final IMablScope scope;
    private final MablApiBuilder builder;

    public VariableCreatorFmi2Api(IMablScope scope, MablApiBuilder builder) {
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

    public static VariableFmi2Api createVariableForPort(TagNameGenerator nameGenerator, PortFmi2Api port, IMablScope scope,
            Fmi2Builder.DynamicActiveScope<PStm> dynamicScope) {
        var name = nameGenerator.getName(port.toLexName());
        var type = MableAstFactory.newAArrayType(fmitypetomabltype(port.scalarVariable.type.type));
        var size = 1;
        PStm stm = MableBuilder.newVariable(name, type, size);
        scope.add(stm);
        VariableFmi2Api variable = new VariableFmi2Api(stm, type, scope, dynamicScope,
                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(0)),
                newAArrayIndexExp(newAIdentifierExp(name), Arrays.asList(newAIntLiteralExp(0))));
        return variable;
    }

    public static FmuVariableFmi2Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, URI uriPath, IMablScope scope) throws Exception {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        IFmu fmu = FmuFactory.create(null, URI.create(path));
        //check schema. The constructor checks the schema
        Fmi2ModelDescription modelDescription = new Fmi2ModelDescription(fmu.getModelDescription());

        return createFMU(builder, nameGenerator, dynamicScope, name, modelDescription, uriPath, scope);
    }

    public static FmuVariableFmi2Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, String loaderName, String[] args, IMablScope scope) throws Exception {

        if (loaderName.equals("FMI2")) {
            return createFMU(builder, nameGenerator, dynamicScope, name, URI.create(args[0]), scope);
        } else if (loaderName.equals("JFMI2")) {
            return createFMU(builder, nameGenerator, dynamicScope, name, args[0], scope);
        }
        return null;
    }

    public static FmuVariableFmi2Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, String className, IMablScope scope) throws Exception {
        String uniqueName = nameGenerator.getName(name);

        PStm var = newVariable(uniqueName, newANameType("FMI2"),
                newALoadExp(Arrays.asList(newAStringLiteralExp("JFMI2"), newAStringLiteralExp(className))));
        scope.add(var);

        IFmu fmu = (IFmu) VariableCreatorFmi2Api.class.getClassLoader().loadClass(className).getConstructor().newInstance();

        FmuVariableFmi2Api fmuVar =
                new FmuVariableFmi2Api(name, builder, new ModelDescriptionContext(new Fmi2ModelDescription(fmu.getModelDescription())), var,
                        MableAstFactory.newANameType("FMI2"), scope, dynamicScope, newAIdentifierStateDesignator(newAIdentifier(uniqueName)),
                        newAIdentifierExp(uniqueName));

        if (builder.getSettings().fmiErrorHandlingEnabled) {

            ScopeFmi2Api thenScope = scope.enterIf(new PredicateFmi2Api(newEqual(fmuVar.getReferenceExp().clone(), newNullExp()))).enterThen();


            thenScope.add(newAAssignmentStm(builder.getGlobalExecutionContinue().getDesignator().clone(), newABoolLiteralExp(false)));

            builder.getLogger().error(thenScope, "FMU load failed on fmu: '%s' for classpath: '%s'", name, className + "");

            ComponentVariableFmi2Api.FmiStatusErrorHandlingBuilder.collectedPreviousLoadedModules(thenScope.getBlock().getBody().getLast())
                    .forEach(p -> {
                        thenScope.add(newExpressionStm(newUnloadExp(newAIdentifierExp(p))));
                        thenScope.add(newAAssignmentStm(newAIdentifierStateDesignator(p), newNullExp()));
                    });


            thenScope.add(newBreak());
            thenScope.leave();
        }

        return fmuVar;
    }

    public static FmuVariableFmi2Api createFMU(MablApiBuilder builder, TagNameGenerator nameGenerator, DynamicActiveBuilderScope dynamicScope,
            String name, Fmi2ModelDescription modelDescription, URI uriPath,
            IMablScope scope) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        String path = uriPath.toString();
        if (uriPath.getScheme() != null && uriPath.getScheme().equals("file")) {
            path = uriPath.getPath();
        }

        String uniqueName = nameGenerator.getName(name);

        PStm var = newVariable(uniqueName, newANameType("FMI2"), newALoadExp(
                Arrays.asList(newAStringLiteralExp("FMI2"), newAStringLiteralExp(modelDescription.getGuid()), newAStringLiteralExp(path))));
        scope.add(var);

        FmuVariableFmi2Api fmuVar =
                new FmuVariableFmi2Api(name, builder, new ModelDescriptionContext(modelDescription), var, MableAstFactory.newANameType("FMI2"), scope,
                        dynamicScope, newAIdentifierStateDesignator(newAIdentifier(uniqueName)), newAIdentifierExp(uniqueName));
        if (builder.getSettings().fmiErrorHandlingEnabled) {

            ScopeFmi2Api thenScope = scope.enterIf(new PredicateFmi2Api(newEqual(fmuVar.getReferenceExp().clone(), newNullExp()))).enterThen();


            thenScope.add(newAAssignmentStm(builder.getGlobalExecutionContinue().getDesignator().clone(), newABoolLiteralExp(false)));

            builder.getLogger().error(thenScope, "FMU load failed on fmu: '%s' for uri: '%s'", name, uriPath + "");

            ComponentVariableFmi2Api.FmiStatusErrorHandlingBuilder.collectedPreviousLoadedModules(thenScope.getBlock().getBody().getLast())
                    .forEach(p -> {
                        thenScope.add(newExpressionStm(newUnloadExp(newAIdentifierExp(p))));
                        thenScope.add(newAAssignmentStm(newAIdentifierStateDesignator(p), newNullExp()));
                    });


            thenScope.add(newBreak());
            thenScope.leave();
        }


        return fmuVar;
    }
}
