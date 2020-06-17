package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class FixedStep implements IMaestroUnfoldPlugin {

    final static Logger logger = LoggerFactory.getLogger(FixedStep.class);

    final AFunctionDeclaration fun = newAFunctionDeclaration(newAIdentifier("fixedStep"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());

    final AFunctionDeclaration funCsv = newAFunctionDeclaration(newAIdentifier("fixedStepCsv"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime")),
                    newAFormalParameter(newAStringPrimitiveType(), newAIdentifier("csv_file_path"))), newAVoidType());

    final AFunctionDeclaration funCsvWs = newAFunctionDeclaration(newAIdentifier("fixedStepCsvWs"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime")),
                    newAFormalParameter(newAStringPrimitiveType(), newAIdentifier("csv_file_path"))), newAVoidType());


    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fun, funCsv, funCsvWs).collect(Collectors.toSet());
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException {

        logger.info("Unfolding with fixed step: {}", declaredFunction.toString());

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new UnfoldException("Unknown function declaration");
        }

        boolean withCsv = false;
        boolean withWs = declaredFunction == funCsvWs;
        AFunctionDeclaration selectedFun = fun;

        if (declaredFunction.equals(funCsv) || declaredFunction.equals(funCsvWs)) {
            selectedFun = funCsv;
            withCsv = true;
        }


        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new UnfoldException("Invalid args");
        }

        if (env == null) {
            throw new UnfoldException("Simulation environment must not be null");
        }

        List<LexIdentifier> knownComponentNames = null;

        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (!compDecl.isPresent()) {
                throw new UnfoldException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        if (knownComponentNames == null || knownComponentNames.isEmpty()) {
            throw new UnfoldException("No components found cannot fixed step with 0 components");
        }

        final List<LexIdentifier> componentNames = knownComponentNames;

        Set<UnitRelationship.Relation> relations =
                env.getRelations(componentNames).stream().filter(r -> r.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External)
                        .collect(Collectors.toSet());

        PExp stepSize = formalArguments.get(1).clone();
        PExp startTime = formalArguments.get(2).clone();
        PExp endTime = formalArguments.get(3).clone();

        //relations.stream().filter(r -> r.getDirection() == In)
        List<PStm> statements = new Vector<>();


        if (withCsv) {
            // ADD CSV

            PExp csvPath = formalArguments.get(4).clone();

            statements.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier("csv"), newANameType("CSV"),
                    newAExpInitializer(newALoadExp(Arrays.asList(newAStringLiteralExp("CSV")))))));
            statements.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier("csvfile"), newANameType("CSVFile"),
                    newAExpInitializer(newACallExp(newADotExp(newAIdentifierExp("csv"), newAIdentifierExp("open")), Arrays.asList(csvPath))))));
        }

        if (withWs) {
            statements.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier("wsHandler"), newANameType("WebsocketHandler"),
                    newAExpInitializer(newALoadExp(Arrays.asList(newAStringLiteralExp("WebsocketHandler")))))));
            statements.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier("ws"), newANameType("Websocket"), newAExpInitializer(
                    newACallExp(newADotExp(newAIdentifierExp("wsHandler"), newAIdentifierExp("open")), Collections.emptyList())))));
        }


        LexIdentifier end = newAIdentifier("end");
        statements.add(newALocalVariableStm(
                newAVariableDeclaration(end, newAIntNumericPrimitiveType(), newAExpInitializer(newMinusExp(endTime, stepSize)))));

        LexIdentifier time = newAIdentifier("time");
        statements.add(newALocalVariableStm(
                newAVariableDeclaration((LexIdentifier) time.clone(), newARealNumericPrimitiveType(), newAExpInitializer(startTime))));


        Set<UnitRelationship.Relation> outputRelations =
                relations.stream().filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.OutputToInput).collect(Collectors.toSet());


        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> outputs =
                outputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                        s -> outputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
                                .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));


        Set<UnitRelationship.Relation> inputRelations =
                relations.stream().filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.InputToOutput).collect(Collectors.toSet());

        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> inputs =
                inputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                        s -> inputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
                                .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));


        //string headers[2] = {"level","valve"};
        List<String> variableNames = new Vector<>();

        Function<RelationVariable, String> getLogName = k -> k.instance.getText() + "." + k.getScalarVariable().getName();

        Map<RelationVariable, PExp> csvFields =
                inputRelations.stream().map(r -> r.getTargets().values().stream().findFirst()).filter(Optional::isPresent).map(Optional::get)
                        .map(h -> h.scalarVariable).sorted(Comparator.comparing(getLogName::apply)).collect(Collectors.toMap(l -> l, r -> {


                    //the relation should be a one to one relation so just take the first one
                    RelationVariable fromVar = r;
                    PExp from = newAArrayIndexExp(
                            newAIdentifierExp(getBufferName(fromVar.instance, fromVar.getScalarVariable().type.type, UsageType.Out)), Collections
                                    .singletonList(newAIntLiteralExp(
                                            outputs.get(fromVar.instance).get(fromVar.getScalarVariable().getType().type).stream()
                                                    .map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList())
                                                    .indexOf(fromVar.scalarVariable.getName()))));
                    return from;

                }, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        variableNames.addAll(csvFields.keySet().stream().map(k -> {

            UnitRelationship.FrameworkUnitInfo info = env.getUnitInfo(k.instance, Framework.FMI2);

            Stream<String> nameComponents = Stream.of(k.instance.getText(), k.getScalarVariable().getName());

            if (info instanceof ComponentInfo) {
                nameComponents = Stream.concat(Stream.of(((ComponentInfo) info).fmuIdentifier), nameComponents);
            }
            return nameComponents.collect(Collectors.joining("."));
        }).collect(Collectors.toList()));

        //csvfile.writeHeader(headers);

        if (withCsv) {
            statements.add(newALocalVariableStm(
                    newAVariableDeclaration(newAIdentifier("csv_headers"), newAArrayType(newAStringPrimitiveType(), variableNames.size()),
                            newAArrayInitializer(variableNames.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList())))));

            statements.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("csvfile"), newAIdentifierExp("writeHeader")),
                    Arrays.asList(newAIdentifierExp("csv_headers")))));

            if (withWs) {
                statements.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("ws"), newAIdentifierExp("writeHeader")),
                        Arrays.asList(newAIdentifierExp("csv_headers")))));
            }
        }

        Consumer<List<PStm>> checkStatus = list -> {

            list.add(newIf(newEqual(newAIdentifierExp("status"), newAIntLiteralExp(0)), newABlockStm(Collections.emptyList()), null));

        };

        try {


            statements.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier("status"), newAIntNumericPrimitiveType())));


            for (LexIdentifier comp : componentNames) {
                ComponentInfo info = env.getUnitInfo(comp, Framework.FMI2);
                if (info.modelDescription.getCanGetAndSetFmustate()) {
                    statements.add(newALocalVariableStm(newAVariableDeclaration(getStateName(comp), newANameType("FmuState"))));
                }
            }

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


        } catch (Exception e) {
            e.printStackTrace();
        }

        Consumer<List<PStm>> terminate = list -> {
            list.addAll(componentNames.stream().map(comp -> newExpressionStm(
                    newACallExp(newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp("terminate")),
                            Collections.emptyList()))).collect(Collectors.toList()));
        };


        Consumer<List<PStm>> setAll = (list) ->
                //set inputs
                inputs.forEach((comp, map) -> map.forEach((type, vars) -> {
                    list.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("status")), newACallExp(
                            newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp(getFmiGetName(type, UsageType.In))),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.In)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.In))))));
                    checkStatus.accept(list);
                }));

        //get outputs
        Consumer<List<PStm>> getAll = (list) -> outputs.forEach((comp, map) -> map.forEach((type, vars) -> {
            list.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("status")),
                    newACallExp(newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp(getFmiGetName(type, UsageType.Out))),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.Out)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.Out))))));
            checkStatus.accept(list);
        }));

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

                list.add(newExternalStm(newACallExp(newAIdentifierExp(newAIdentifier("convert" + fromVar.getScalarVariable().getType().type + "2" +
                        r.getSource().scalarVariable.getScalarVariable().getType().type)), Arrays.asList(from, toAsExp))));


            } else {
                list.add(newAAssignmentStm(to, from));
            }

        });


        Consumer<List<PStm>> doStep = (list) -> componentNames.forEach(comp -> {
            //int doStep(real currentCommunicationPoint, real communicationStepSize, bool noSetFMUStatePriorToCurrentPoint);
            list.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("status")),
                    newACallExp(newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp("doStep")),
                            Arrays.asList(newAIdentifierExp((LexIdentifier) time.clone()), stepSize.clone(), newABoolLiteralExp(true)))));

            checkStatus.accept(list);
        });

        Consumer<List<PStm>> progressTime = list -> list.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) time.clone()),
                newPlusExp(newAIdentifierExp((LexIdentifier) time.clone()), stepSize.clone())));

        Consumer<List<PStm>> declareCsvBuffer = list -> {
            list.add(newALocalVariableStm(
                    newAVariableDeclaration(newAIdentifier("csv_values"), newAArrayType(newAStringPrimitiveType(), variableNames.size()),
                            newAArrayInitializer(csvFields.values().stream().map(PExp::clone).collect(Collectors.toList())))));

            list.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("csvfile"), newAIdentifierExp("writeRow")),
                    Arrays.asList(newAIdentifierExp("time"), newAIdentifierExp("csv_values")))));

            if (withWs) {
                list.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("ws"), newAIdentifierExp("writeRow")),
                        Arrays.asList(newAIdentifierExp("time"), newAIdentifierExp("csv_values")))));
            }
        };

        Consumer<List<PStm>> logCsvValues = list -> {
            List<PExp> values = new ArrayList<>(csvFields.values());
            //values.add(0, newAIdentifierExp("time"));
            for (int i = 0; i < values.size(); i++) {
                AArrayStateDesignator to = newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier("csv_values")), newAIntLiteralExp(i));
                list.add(newAAssignmentStm(to, values.get(i).clone()));
            }

            list.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("csvfile"), newAIdentifierExp("writeRow")),
                    Arrays.asList(newAIdentifierExp("time"), newAIdentifierExp("csv_values")))));

            if (withWs) {
                list.add(newExpressionStm(newACallExp(newADotExp(newAIdentifierExp("ws"), newAIdentifierExp("writeRow")),
                        Arrays.asList(newAIdentifierExp("time"), newAIdentifierExp("csv_values")))));
            }

        };

        List<PStm> loopStmts = new Vector<>();

        // get prior to entering loop
        getAll.accept(statements);
        if (withCsv) {
            declareCsvBuffer.accept(statements);
        }


        //exchange according to mapping
        exchangeData.accept(loopStmts);
        //set inputs
        setAll.accept(loopStmts);
        //do step
        doStep.accept(loopStmts);
        //get data
        getAll.accept(loopStmts);
        // time = time + STEP_SIZE;
        progressTime.accept(loopStmts);
        if (withCsv) {
            logCsvValues.accept(loopStmts);
        }

        statements.add(newWhile(newALessEqualBinaryExp(newAIdentifierExp(time), newAIdentifierExp(end)), newABlockStm(loopStmts)));


        terminate.accept(statements);

        if (withCsv) {
            statements.add(newExpressionStm(
                    newACallExp(newADotExp(newAIdentifierExp("csv"), newAIdentifierExp("close")), Arrays.asList(newAIdentifierExp("csvfile")))));
            statements.add(newExpressionStm(newUnloadExp(Arrays.asList(newAIdentifierExp("csv")))));
        }
        if (withWs) {
            statements.add(newExpressionStm(
                    newACallExp(newADotExp(newAIdentifierExp("wsHandler"), newAIdentifierExp("close")), Arrays.asList(newAIdentifierExp("ws")))));
            statements.add(newExpressionStm(newUnloadExp(Arrays.asList(newAIdentifierExp("wsHandler")))));
        }


        return newABlockStm(statements);
    }


    private String getFmiGetName(ModelDescription.Types type, UsageType usage) {

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

    LexIdentifier getStateName(LexIdentifier comp) {
        return newAIdentifier(comp.getText() + "State");
    }

    SPrimitiveType convert(ModelDescription.Types type) {
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

    LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }


    LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    private String getTypeId(SPrimitiveType type) {
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

    LexIdentifier getVrefName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {

        return newAIdentifier(comp.getText() + "Vref" + getTypeId(convert(type)) + usage);
    }

    @Override
    public boolean requireConfig() {
        return false;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return new FixedstepConfig(new ObjectMapper().readValue(is, Integer.class));
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    enum UsageType {
        In,
        Out
    }

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
