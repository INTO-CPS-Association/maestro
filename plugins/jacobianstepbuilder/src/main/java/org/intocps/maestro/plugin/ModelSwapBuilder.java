package org.intocps.maestro.plugin;

import org.apache.commons.lang3.tuple.Triple;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ModelSwapInfo;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ModelSwapBuilder {

    static PortFmi2Api getSwapSourcePort(PortFmi2Api port, Set<Fmi2SimulationEnvironment.Relation> swapRelations,
                                         Map<String, ComponentVariableFmi2Api> fmuInstances) {
        PortFmi2Api sourcePort = null;
        Optional<Fmi2SimulationEnvironment.Relation> relation = swapRelations.stream()
                .filter(r -> r.getTargets().values().stream().anyMatch(v -> v.toString().equals(port.getMultiModelScalarVariableNameWithoutFmu())))
                .findFirst();
        if (relation.isPresent()) {
            String source = relation.get().getSource().getInstance().getText();
            sourcePort = fmuInstances.get(source).getPort(relation.get().getSource().getName());
        }
        return sourcePort;
    }

    static ModelSwapInfo getSwapSourceInfo(PortFmi2Api port, Set<Fmi2SimulationEnvironment.Relation> swapRelations,
                                           Map<String, ComponentVariableFmi2Api> fmuInstances, Fmi2SimulationEnvironment env) {
        ModelSwapInfo swapInfo = null;
        Optional<Fmi2SimulationEnvironment.Relation> relation = swapRelations.stream()
                .filter(r -> r.getTargets().values().stream().anyMatch(v -> v.toString().equals(port.getMultiModelScalarVariableNameWithoutFmu())))
                .findFirst();
        if (relation.isPresent()) {
            String source = relation.get().getSource().getInstance().getText();
            Optional<Map.Entry<String, ModelSwapInfo>> infoEntry =
                    env.getModelSwaps().stream().filter(e -> e.getValue().swapInstance.equals(source)).findFirst();
            if (infoEntry.isPresent()) {
                swapInfo = infoEntry.get().getValue();
            }
        }

        return swapInfo;
    }

    static class ModelSwapContext {
        Map<ModelSwapInfo, Triple<BooleanVariableFmi2Api, BooleanVariableFmi2Api, DoubleVariableFmi2Api>> modelSwapConditions;
    }

    public static Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> updateStep(ModelSwapContext swapCtxt, Fmi2SimulationEnvironment env, ComponentVariableFmi2Api instance, DoubleVariableFmi2Api communicationTime) {

        PredicateFmi2Api stepPredicate = null;
        for (Map.Entry<String, ModelSwapInfo> modelSwapInfoEntry : env.getModelSwaps()) {
            if (instance.getName().equals(modelSwapInfoEntry.getKey()) ||
                    instance.getName().equals(modelSwapInfoEntry.getValue().swapInstance)) {

                //fixme handle cases where we cannot replace all field expressions
                if (instance.getName().equals(modelSwapInfoEntry.getKey())) {
                    // instance swapped out -> stepPredicate = !swapCondition
                    stepPredicate = swapCtxt.modelSwapConditions.get(modelSwapInfoEntry.getValue()).getLeft().toPredicate().not();
                } else {
                    // instance swapped in -> stepPredicate = stepCondition
                    stepPredicate = swapCtxt.modelSwapConditions.get(modelSwapInfoEntry.getValue()).getMiddle().toPredicate();
                    communicationTime = swapCtxt.modelSwapConditions.get(modelSwapInfoEntry.getValue()).getRight();
                }
                break;
            }
        }

        return Map.entry(communicationTime, stepPredicate == null ? Optional.empty() : Optional.of(stepPredicate));
    }

    public static void updateDiscardStepTime(ModelSwapContext swapCtxt, DynamicActiveBuilderScope dynamicScope, DoubleVariableFmi2Api currentStepSize) {
        swapCtxt.modelSwapConditions.forEach((info, value) -> {
            DoubleVariableFmi2Api communicationTime = value.getRight();
            PredicateFmi2Api stepPredicate = value.getMiddle().toPredicate();
            if (stepPredicate != null) {
                dynamicScope.enterIf(stepPredicate);
            }
            communicationTime.setValue(communicationTime.toMath().addition(currentStepSize));
            if (stepPredicate != null) {
                dynamicScope.leave();
            }
        });
    }

    public static ModelSwapContext buildContext(Fmi2SimulationEnvironment env, DynamicActiveBuilderScope dynamicScope) {
        ModelSwapContext ctxt = new ModelSwapContext();

        Map<ModelSwapInfo, Triple<BooleanVariableFmi2Api, BooleanVariableFmi2Api, DoubleVariableFmi2Api>> modelSwapConditions = new HashMap<>();
        List<ModelSwapInfo> swapInfoList = env.getModelSwaps().stream().map(Map.Entry::getValue).collect(Collectors.toList());
        int index = 0;
        for (ModelSwapInfo info : swapInfoList) {
            BooleanVariableFmi2Api swapVar = dynamicScope.store("swapCondition" + index, false);
            BooleanVariableFmi2Api stepVar = dynamicScope.store("stepCondition" + index, false);
            DoubleVariableFmi2Api currentCommunicationTimeOffset = dynamicScope.store("jac_current_communication_point_offset" + index, 0.0);
            modelSwapConditions.put(info, Triple.of(swapVar, stepVar, currentCommunicationTimeOffset));
            index++;
        }

        ctxt.modelSwapConditions = modelSwapConditions;
        return ctxt;
    }

    public static void updateSwapConditionVariables(ModelSwapContext swapCtxt, DynamicActiveBuilderScope dynamicScope, Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues) {

        Map<String, PExp> replaceRule = componentsToPortsWithValues.entrySet().stream().flatMap(c -> c.getValue().entrySet().stream()
                        .map(p -> Map.entry(p.getKey().getMultiModelScalarVariableNameWithoutFmu(),
                                p.getKey().getSharedAsVariable().getReferenceExp())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        // Update all swap and step condition variables
        swapCtxt.modelSwapConditions.forEach((info, value) -> {
            BooleanVariableFmi2Api swapVar = value.getLeft();
            BooleanVariableFmi2Api stepVar = value.getMiddle();
            swapVar.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                    MableAstFactory.newOr(swapVar.getExp(), IdentifierReplacer.replaceFields(info.swapCondition, replaceRule))));
            stepVar.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                    MableAstFactory.newOr(stepVar.getExp(), IdentifierReplacer.replaceFields(info.stepCondition, replaceRule))));
        });
    }


    public static void generateLinking(Map<String, ComponentVariableFmi2Api> fmuInstances, Fmi2SimulationEnvironment env, DynamicActiveBuilderScope dynamicScope, ModelSwapContext swapCtxt) {
        // SET ALL LINKED VARIABLES
        // This has to be carried out regardless of stabilisation or not.
        fmuInstances.values().forEach(instance -> {
            //  HE: fixme, remove this comment block - only for devel
            //  if instance is source in swap and has existing source ports
            //      enterif(swapCond.not)
            //      inst.setLinked() // all ports
            //  else if instance is target in swap and has swap source ports
            //      create links from swap source ports to ports
            //      enterif(stepCond)
            //      inst.setLinked() // all ports
            //  else if instance is connected in swap relation
            //      for each port of instance
            //              if port is target in swap relation
            //                  if port has existing source port
            //                      enterIf(swapCond.not)
            //                      instance.setLinked(port)
            //                      p.breakLink()
            //                  create link from swap source port to port
            //                  enterIf(swapCond)
            //                  instance.setLinked(p)
            //              else
            //                  instance.setLinked(p)
            //  else if instance has source ports
            //      inst.setLinked()

            Predicate<PortFmi2Api> onlyInputs = p -> p.scalarVariable.causality == Fmi2ModelDescription.Causality.Input;


            Set<Fmi2SimulationEnvironment.Relation> swapRelations = env.getModelSwapRelations();
            Optional<Map.Entry<String, ModelSwapInfo>> swapInfoOutput =
                    env.getModelSwaps().stream().filter(e -> e.getKey().equals(instance.getName())).findFirst();
            Optional<Map.Entry<String, ModelSwapInfo>> swapInfoInput =
                    env.getModelSwaps().stream().filter(e -> e.getValue().swapInstance.equals(instance.getName())).findFirst();

            if (swapInfoOutput.isPresent()) {
                if (instance.getPorts().stream().filter(onlyInputs).anyMatch(PortFmi2Api::isLinkedAsInputConsumer)) {
                    PredicateFmi2Api swapPredicate = swapCtxt.modelSwapConditions.get(swapInfoOutput.get().getValue()).getLeft().toPredicate();
                    dynamicScope.enterIf(swapPredicate.not());
                    instance.setLinked();
                    dynamicScope.leave();
                }
            } else if (swapInfoInput.isPresent()) {
                instance.getPorts().stream().filter(onlyInputs).forEach(port -> {
                    PortFmi2Api sourcePort = getSwapSourcePort(port, swapRelations, fmuInstances);
                    if (sourcePort != null) {
                        JacobianStepBuilder.relinkPorts.accept(sourcePort, port);
                    }
                });

                if (instance.getPorts().stream().filter(onlyInputs).anyMatch(PortFmi2Api::isLinkedAsInputConsumer)) {
                    PredicateFmi2Api stepPredicate = swapCtxt.modelSwapConditions.get(swapInfoInput.get().getValue()).getMiddle().toPredicate();
                    dynamicScope.enterIf(stepPredicate);
                    instance.setLinked();
                    dynamicScope.leave();
                }
            } else if (instance.getPorts().stream().filter(onlyInputs)
                    .anyMatch(port -> getSwapSourcePort(port, swapRelations, fmuInstances) != null)) {
                instance.getPorts().stream().filter(onlyInputs).forEach(port -> {
                    PortFmi2Api swapSourcePort = getSwapSourcePort(port, swapRelations, fmuInstances);

                    if (swapSourcePort != null) {
                        ModelSwapInfo swapInfo = getSwapSourceInfo(port, swapRelations, fmuInstances, env);
                        PredicateFmi2Api swapPredicate = swapCtxt.modelSwapConditions.get(swapInfo).getLeft().toPredicate();

                        if (port.getSourcePort() != null) {
                            dynamicScope.enterIf(swapPredicate.not());
                            instance.setLinked(port);
                            dynamicScope.leave();
                            port.breakLink();
                        }

                        JacobianStepBuilder.relinkPorts.accept(swapSourcePort, port);

                        dynamicScope.enterIf(swapPredicate);
                        instance.setLinked(port);
                        dynamicScope.leave();

                    } else if (port.getSourcePort() != null) {
                        instance.setLinked(port);
                    }
                });
            } else if (instance.getPorts().stream().filter(onlyInputs).anyMatch(PortFmi2Api::isLinkedAsInputConsumer)) {
                instance.setLinked();
            }
        });
    }
}
