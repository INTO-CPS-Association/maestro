package org.intocps.maestro;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.FromMaBLToMaBLAPI;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.typechecker.TypeComparator;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newABlockStm;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class BuilderHelper {
    static final TypeComparator typeComparator = new TypeComparator();
    private final MablApiBuilder builder;
    List<Fmi2Builder.Variable<PStm, ?>> variables;

    public BuilderHelper(ACallExp callToBeReplaced, Map<INode, PType> typesMap,
            ISimulationEnvironment simulationEnvironment) throws Fmi2Builder.Port.PortLinkException {

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = true;
        this.builder = new MablApiBuilder(settings, callToBeReplaced);

        Map<String, ComponentVariableFmi2Api> instances = new HashMap<>();
        this.variables = callToBeReplaced.getArgs().stream()
                .map(exp -> wrapAsVariable(builder, typesMap, simulationEnvironment, typesMap.get(exp), exp, instances)).collect(Collectors.toList());

        FromMaBLToMaBLAPI.createBindings(instances, simulationEnvironment);
    }

    private static Fmi2Builder.Variable<PStm, ?> wrapAsVariable(MablApiBuilder builder, Map<INode, PType> typesMap,
            ISimulationEnvironment simulationEnvironment, PType type, PExp exp, Map<String, ComponentVariableFmi2Api> instances) {

        Fmi2SimulationEnvironment env = null;

        if (simulationEnvironment instanceof Fmi2SimulationEnvironment) {
            env = (Fmi2SimulationEnvironment) simulationEnvironment;
        }


        if (typeComparator.compatible(ARealNumericPrimitiveType.class, type)) {
            return new DoubleVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(AIntNumericPrimitiveType.class, type)) {
            return new IntVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(ABooleanPrimitiveType.class, type)) {
            return new BooleanVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(AStringPrimitiveType.class, type)) {
            return new StringVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (env != null && type instanceof AModuleType && ((AModuleType) type).getName().getText().equals("FMI2")) {
            if (exp instanceof AIdentifierExp) {
                String componentName = ((AIdentifierExp) exp).getName().getText();
                ComponentInfo instance = env.getInstanceByLexName(componentName);
                ModelDescriptionContext modelDescriptionContext = null;
                try {
                    modelDescriptionContext = new ModelDescriptionContext(instance.modelDescription);
                } catch (IllegalAccessException | XPathExpressionException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                //This dummy statement is removed later. It ensures that the share variables are added to the root scope.
                PStm dummyStm = newABlockStm();
                builder.getDynamicScope().add(dummyStm);

                FmuVariableFmi2Api fmu =
                        new FmuVariableFmi2Api(instance.fmuIdentifier, builder, modelDescriptionContext, dummyStm, newANameType("FMI2"),
                                builder.getDynamicScope().getActiveScope(), builder.getDynamicScope(), null,
                                new AIdentifierExp(new LexIdentifier(instance.fmuIdentifier.replace("{", "").replace("}", ""), null)));
                return fmu;
            } else {
                throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
            }

        } else if (env != null && type instanceof AModuleType && ((AModuleType) type).getName().getText().equals("FMI2Component")) {
            try {
                Map.Entry<String, ComponentVariableFmi2Api> component = FromMaBLToMaBLAPI.getComponentVariableFrom(builder, exp, env);

                instances.put(component.getKey(), component.getValue());
                return component.getValue();
            } catch (IllegalAccessException | XPathExpressionException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (type instanceof AArrayType &&
                (((AArrayType) type).getType() instanceof ANameType || ((AArrayType) type).getType() instanceof AModuleType)) {
            LexIdentifier itemName = ((AIdentifierExp) exp).getName();
            SBlockStm containingBlock = exp.getAncestor(SBlockStm.class);
            Optional<AVariableDeclaration> itemDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(itemName) && !decl.getSize().isEmpty() && decl.getInitializer() != null)
                            .findFirst();

            if (itemDecl.isEmpty()) {
                throw new RuntimeException("Could not find names for components");
            }

            AArrayInitializer initializer = (AArrayInitializer) itemDecl.get().getInitializer();


            List<PExp> initializerExps = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .collect(Collectors.toList());

            return new ArrayVariableFmi2Api(null, type.clone(), null, null, null, exp,
                    initializerExps.stream().map(e -> wrapAsVariable(builder, typesMap, simulationEnvironment, typesMap.get(e), e, instances))
                            .collect(Collectors.toList()));
        }

        return null;
    }

    public Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp, MablApiBuilder.MablSettings> getBuilder() {
        return this.builder;
    }

    public List<Fmi2Builder.Variable<PStm, ?>> getArgumentVariables() {
        return this.variables;
    }
}
