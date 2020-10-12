package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class DataExchangeHandler {

    private final Set<FmiSimulationEnvironment.Relation> outputRelations;
    private final Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> outputs;
    private final Set<FmiSimulationEnvironment.Relation> inputRelations;
    private final Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> inputs;
    Function<LexIdentifier, PStateDesignator> getCompStatusDesignator;
    BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus;

    public DataExchangeHandler(Set<FmiSimulationEnvironment.Relation> relations, FmiSimulationEnvironment env,
            Function<LexIdentifier, PStateDesignator> getCompStatusDesignator,
            BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus) {
        this.checkStatus = checkStatus;
        this.getCompStatusDesignator = getCompStatusDesignator;
        outputRelations = relations.stream().filter(r -> r.getDirection() == FmiSimulationEnvironment.Relation.Direction.OutputToInput)
                .collect(Collectors.toSet());

        // outputs contains both outputs based on relations and outputs based on additional variables to log
        outputs = outputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors
                .toMap(Function.identity(), s -> outputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s)).flatMap(r -> {
                    List<ModelDescription.ScalarVariable> outputs_ =
                            env.getVariablesToLog(s.getText()).stream().map(x -> x.scalarVariable).collect(Collectors.toList());
                    //outputs_.add(r.getSource().scalarVariable.getScalarVariable());
                    return outputs_.stream();
                }).distinct().collect(Collectors.groupingBy(sv -> sv.getType().type))));

        // We need to add the additional

        inputRelations = relations.stream().filter(r -> r.getDirection() == FmiSimulationEnvironment.Relation.Direction.InputToOutput)
                .collect(Collectors.toSet());

        inputs = inputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                s -> inputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
                        .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));

    }

    static String getFmiGetName(ModelDescription.Types type, UsageType usage) {

        String fun = usage == UsageType.In ? "set" : "get";
        switch (type) {
            case Boolean:
                return fun + "Boolean";
            case Real:
                return fun + "Real";
            case Integer:
                return fun + "Integer";
            case String:
                return fun + "String";
            case Enumeration:
            default:
                return null;
        }
    }

    static SPrimitiveType convert(ModelDescription.Types type) {
        switch (type) {

            case Boolean:
                return newABoleanPrimitiveType();
            case Real:
                return newARealNumericPrimitiveType();
            case Integer:
                return newAIntNumericPrimitiveType();
            case String:
                return newAStringPrimitiveType();
            case Enumeration:
            default:
                return null;
        }
    }

    static LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }

    static LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    static String getTypeId(SPrimitiveType type) {
        String t = type.getClass().getSimpleName();

        if (type instanceof ARealNumericPrimitiveType) {
            t = "R";
        } else if (type instanceof AIntNumericPrimitiveType) {
            t = "I";
        } else if (type instanceof AStringPrimitiveType) {
            t = "S";
        } else if (type instanceof ABooleanPrimitiveType) {
            t = "B";
        }
        return t;
    }

    public Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> getOutputs() {
        return outputs;
    }

    public Set<FmiSimulationEnvironment.Relation> getInputRelations() {
        return inputRelations;
    }

    LexIdentifier getVrefName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {

        return newAIdentifier(comp.getText() + "Vref" + getTypeId(convert(type)) + usage);
    }

    public List<PStm> allocate() {
        List<PStm> statements = new Vector<>();

        //create output buffers
        outputs.forEach((comp, map) -> map.forEach((type, vars) -> statements.add(newALocalVariableStm(
                newAVariableDeclaration(getBufferName(comp, type, UsageType.Out), newAArrayType(convert(type), vars.size()))))));

        outputs.forEach((comp, map) -> map.forEach((type, vars) -> statements.add(newALocalVariableStm(
                newAVariableDeclaration(getVrefName(comp, type, UsageType.Out), newAArrayType(newAUIntNumericPrimitiveType(), vars.size()),
                        newAArrayInitializer(vars.stream().map(v -> newAIntLiteralExp((int) v.valueReference)).collect(Collectors.toList())))))));

        //create input buffers
        inputs.forEach((comp, map) -> map.forEach((type, vars) -> statements.add(newALocalVariableStm(
                newAVariableDeclaration(getBufferName(comp, type, UsageType.In), newAArrayType(convert(type), vars.size()))))));

        inputs.forEach((comp, map) -> map.forEach((type, vars) -> statements.add(newALocalVariableStm(
                newAVariableDeclaration(getVrefName(comp, type, UsageType.In), newAArrayType(newAUIntNumericPrimitiveType(), vars.size()),
                        newAArrayInitializer(vars.stream().map(v -> newAIntLiteralExp((int) v.valueReference)).collect(Collectors.toList())))))));


        return statements;
    }

    public List<PStm> getAll(boolean inSimulationLoop) {

        //get outputs
        BiConsumer<Boolean, List<PStm>> getAll = (inLoop, list) -> outputs.forEach((comp, map) -> map.forEach((type, vars) -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier(getFmiGetName(type, UsageType.Out)),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.Out)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.Out))))));
            checkStatus.accept(Map.entry(inSimulationLoop, "get failed"), Map.entry(comp, list));
        }));

        List<PStm> statements = new Vector<>();
        getAll.accept(inSimulationLoop, statements);
        return statements;


    }

    public List<PStm> setAll() {
        Consumer<List<PStm>> setAll = (list) ->
                //set inputs
                inputs.forEach((comp, map) -> map.forEach((type, vars) -> {
                    list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                            newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier(getFmiGetName(type, UsageType.In)),
                                    Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.In)), newAIntLiteralExp(vars.size()),
                                            newAIdentifierExp(getBufferName(comp, type, UsageType.In))))));
                    checkStatus.accept(Map.entry(true, "set failed"), Map.entry(comp, list));
                }));


        List<PStm> statements = new Vector<>();
        setAll.accept(statements);
        return statements;

    }

    public List<PStm> exchangeData() {

        Consumer<List<PStm>> exchangeData = (list) -> inputRelations.forEach(r -> {
            int toIndex =
                    inputs.get(r.getSource().scalarVariable.instance).get(r.getSource().scalarVariable.getScalarVariable().getType().type).stream()
                            .map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList())
                            .indexOf(r.getSource().scalarVariable.scalarVariable.getName());

            AArrayStateDesignator to = newAArayStateDesignator(newAIdentifierStateDesignator(
                    getBufferName(r.getSource().scalarVariable.instance, r.getSource().scalarVariable.getScalarVariable().getType().type,
                            UsageType.In)), newAIntLiteralExp(toIndex));

            //the relation should be a one to one relation so just take the first one
            RelationVariable fromVar = r.getTargets().values().iterator().next().scalarVariable;
            PExp from = newAArrayIndexExp(newAIdentifierExp(getBufferName(fromVar.instance, fromVar.getScalarVariable().type.type, UsageType.Out)),
                    Collections.singletonList(newAIntLiteralExp(outputs.get(fromVar.instance).get(fromVar.getScalarVariable().getType().type).stream()
                            .map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList()).indexOf(fromVar.scalarVariable.getName()))));

            if (r.getSource().scalarVariable.getScalarVariable().getType().type != fromVar.getScalarVariable().getType().type) {
                //ok the types are not matching, lets use a converter

                AArrayIndexExp toAsExp = newAArrayIndexExp(newAIdentifierExp(
                        getBufferName(r.getSource().scalarVariable.instance, r.getSource().scalarVariable.getScalarVariable().getType().type,
                                UsageType.In)), Arrays.asList(newAIntLiteralExp(toIndex)));

                list.add(newExpressionStm(newACallExp(newExpandToken(), newAIdentifier("convert" + fromVar.getScalarVariable().getType().type + "2" +
                        r.getSource().scalarVariable.getScalarVariable().getType().type), Arrays.asList(from, toAsExp))));


            } else {
                list.add(newAAssignmentStm(to, from));
            }

        });
        List<PStm> statements = new Vector<>();
        exchangeData.accept(statements);
        return statements;
    }

    static enum UsageType {
        In,
        Out
    }

}
