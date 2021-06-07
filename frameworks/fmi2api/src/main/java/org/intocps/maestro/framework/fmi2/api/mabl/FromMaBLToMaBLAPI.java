package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.core.IRelation;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class FromMaBLToMaBLAPI {

    public static Map.Entry<String, ComponentVariableFmi2Api> getComponentVariableFrom(MablApiBuilder builder, PExp exp,
            Fmi2SimulationEnvironment env) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (exp instanceof AIdentifierExp) {
            String componentName = ((AIdentifierExp) exp).getName().getText();
            ComponentInfo instance = env.getInstanceByLexName(componentName);
            ModelDescriptionContext modelDescriptionContext = new ModelDescriptionContext(instance.modelDescription);

            //This dummy statement is removed later. It ensures that the share variables are added to the root scope.
            PStm dummyStm = newABlockStm();
            builder.getDynamicScope().add(dummyStm);

            FmuVariableFmi2Api fmu = new FmuVariableFmi2Api(instance.fmuIdentifier, builder, modelDescriptionContext, dummyStm, newANameType("FMI2"),
                    builder.getDynamicScope().getActiveScope(), builder.getDynamicScope(), null,
                    new AIdentifierExp(new LexIdentifier(instance.fmuIdentifier.replace("{", "").replace("}", ""), null)));

            ComponentVariableFmi2Api a = new ComponentVariableFmi2Api(dummyStm, fmu, componentName, modelDescriptionContext, builder,
                    builder.getDynamicScope().getActiveScope(), null, newAIdentifierExp(componentName));
            List<RelationVariable> variablesToLog = env.getVariablesToLog(componentName);
            a.setVariablesToLog(variablesToLog);

            return Map.entry(componentName, a);
        } else {
            throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
        }
    }

    public static void CreateBindings(Map<String, ComponentVariableFmi2Api> instances,
            ISimulationEnvironment env) throws Fmi2Builder.Port.PortLinkException {
        for (Map.Entry<String, ComponentVariableFmi2Api> entry : instances.entrySet()) {
            for (IRelation relation : env.getRelations(entry.getKey()).stream()
                    .filter(x -> x.getDirection() == Fmi2SimulationEnvironment.Relation.Direction.OutputToInput &&
                            x.getOrigin() == Fmi2SimulationEnvironment.Relation.InternalOrExternal.External).collect(Collectors.toList())) {
                PortFmi2Api[] targets = relation.getTargets().entrySet().stream().map(x -> {
                    ComponentVariableFmi2Api instance = instances.get(x.getValue().getScalarVariable().getInstance().getText());
                    return instance.getPort(x.getValue().getScalarVariable().getScalarVariable().getName());
                }).toArray(PortFmi2Api[]::new);
                entry.getValue().getPort(relation.getSource().getScalarVariable().getScalarVariable().getName()).linkTo(targets);
            }
        }

    }


    public static Map<String, ComponentVariableFmi2Api> GetComponentVariablesFrom(MablApiBuilder builder, PExp exp,
            Fmi2SimulationEnvironment env) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        LexIdentifier componentsArrayName = ((AIdentifierExp) exp).getName();
        SBlockStm containingBlock = exp.getAncestor(SBlockStm.class);
        Optional<AVariableDeclaration> componentDeclaration =
                containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                        .map(ALocalVariableStm::getDeclaration)
                        .filter(decl -> decl.getName().equals(componentsArrayName) && !decl.getSize().isEmpty() && decl.getInitializer() != null)
                        .findFirst();

        if (!componentDeclaration.isPresent()) {
            throw new RuntimeException("Could not find names for components");
        }

        AArrayInitializer initializer = (AArrayInitializer) componentDeclaration.get().getInitializer();


        List<PExp> componentIdentifiers =
                initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast).collect(Collectors.toList());

        HashMap<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>();

        for (PExp componentName : componentIdentifiers) {
            Map.Entry<String, ComponentVariableFmi2Api> component = getComponentVariableFrom(builder, componentName, env);
            fmuInstances.put(component.getKey(), component.getValue());
        }


        return fmuInstances;
    }
}
