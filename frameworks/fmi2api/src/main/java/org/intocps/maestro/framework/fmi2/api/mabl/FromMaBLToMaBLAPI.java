package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.core.*;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class FromMaBLToMaBLAPI {
    final static Logger logger = LoggerFactory.getLogger(FromMaBLToMaBLAPI.class);

    public static Map.Entry<String, ComponentVariableFmi2Api> getComponentVariableFrom(MablApiBuilder builder, PExp exp,
            Fmi2SimulationEnvironment env) throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (exp instanceof AIdentifierExp) {
            return getComponentVariableFrom(builder, exp, env, ((AIdentifierExp) exp).getName().getText());
        } else {
            throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
        }
    }

    public static Map.Entry<String, ComponentVariableFmi2Api> getComponentVariableFrom(MablApiBuilder builder, PExp exp,
            Fmi2SimulationEnvironment env,
            String environmentComponentName) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (exp instanceof AIdentifierExp) {
            String componentName = ((AIdentifierExp) exp).getName().getText();

            FrameworkUnitInfo inst = env.getInstanceByLexName(environmentComponentName);
            if (inst instanceof ComponentInfo) {


                ComponentInfo instance = (ComponentInfo) inst;
                ModelDescriptionContext modelDescriptionContext = new ModelDescriptionContext(instance.modelDescription);

                //This dummy statement is removed later. It ensures that the share variables are added to the root scope.
                PStm dummyStm = newABlockStm();
                builder.getDynamicScope().add(dummyStm);

                FmuVariableFmi2Api fmu =
                        new FmuVariableFmi2Api(instance.fmuIdentifier, builder, modelDescriptionContext, dummyStm, newANameType("FMI2"),
                                builder.getDynamicScope().getActiveScope(), builder.getDynamicScope(), null,
                                new AIdentifierExp(new LexIdentifier(instance.fmuIdentifier.replace("{", "").replace("}", ""), null)));

                ComponentVariableFmi2Api a;
                if (environmentComponentName == null) {
                    a = new ComponentVariableFmi2Api(dummyStm, fmu, componentName, modelDescriptionContext, builder,
                            builder.getDynamicScope().getActiveScope(), null, newAIdentifierExp(componentName));
                } else {
                    a = new ComponentVariableFmi2Api(dummyStm, fmu, componentName, modelDescriptionContext, builder,
                            builder.getDynamicScope().getActiveScope(), null, newAIdentifierExp(componentName), environmentComponentName);
                }
                List<RelationVariable> variablesToLog = null;
                if (environmentComponentName == null) {
                    variablesToLog = env.getVariablesToLog(componentName);
                } else {
                    variablesToLog = env.getVariablesToLog(environmentComponentName);
                }
                a.setVariablesToLog(variablesToLog.stream().filter(org.intocps.maestro.framework.fmi2.RelationVariable.class::isInstance)
                        .map(org.intocps.maestro.framework.fmi2.RelationVariable.class::cast).collect(Collectors.toList()));

                return Map.entry(componentName, a);
            } else {
                throw new RuntimeException("instance is not an fmi2 component: " + componentName);
            }
        } else {
            throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
        }
    }

    public static void createBindings(Map<String, ComponentVariableFmi2Api> instances,
            ISimulationEnvironment env) throws Fmi2Builder.Port.PortLinkException {
        for (Map.Entry<String, ComponentVariableFmi2Api> entry : instances.entrySet()) {
            java.util.Set<? extends IRelation> relations = getRelations(entry, env);
            for (IRelation relation : relations.stream().filter(x -> x.getDirection() == Fmi2SimulationEnvironment.Relation.Direction.OutputToInput &&
                    x.getOrigin() == Fmi2SimulationEnvironment.Relation.InternalOrExternal.External).collect(Collectors.toList())) {

                for (IVariable targetVar : relation.getTargets().values()) {
                    String targetName = targetVar.getScalarVariable().getInstance().getText();
                    if (instances.containsKey(targetName) ||
                            instances.values().stream().anyMatch(x -> x.getEnvironmentName().equalsIgnoreCase(targetName))) {
                        ComponentVariableFmi2Api instance = instances.get(targetName);
                        if (instance == null) {
                            Optional<ComponentVariableFmi2Api> instanceOpt =
                                    instances.values().stream().filter(x -> x.getEnvironmentName().equalsIgnoreCase(targetName)).findFirst();
                            if (instanceOpt.isPresent()) {
                                instance = instanceOpt.get();
                            }
                        }

                        PortFmi2Api targetPort = instance.getPort(targetVar.getScalarVariable().getName());

                        String sourcePortName = relation.getSource().getScalarVariable().getName();
                        if (targetPort != null) {
                            entry.getValue().getPort(sourcePortName).linkTo(targetPort);
                        } else {
                            //error port not found in target var
                            logger.warn("Failed to find port '{}' on instance '{}' required by relational '{}' ", sourcePortName, targetName,
                                    relation);
                        }
                    } else {
                        logger.warn(
                                "Failed to find instance required by relational information from simulation env. Missing '{}' in relation " + "{}",
                                targetName, relation);
                    }
                }
            }
        }

    }

    private static Set<? extends IRelation> getRelations(Map.Entry<String, ComponentVariableFmi2Api> entry, ISimulationEnvironment env) {
        if (entry.getValue().getEnvironmentName() != entry.getKey()) {
            return env.getRelations(entry.getValue().getEnvironmentName());
        } else {
            return env.getRelations(entry.getKey());
        }
    }


    public static Map<String, ComponentVariableFmi2Api> getComponentVariablesFrom(MablApiBuilder builder, PExp exp,
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
