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

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.plugin.FixedStep.MableBuilder.*;

@SimulationFramework(framework = Framework.FMI2)
public class FixedStep implements IMaestroExpansionPlugin {
    final static String fixedStepStatus = "fix_status";
    final static Logger logger = LoggerFactory.getLogger(FixedStep.class);
    private final static int FMI_OK = 0;
    private final static int FMI_WARNING = 1;
    private final static int FMI_DISCARD = 2;
    private final static int FMI_ERROR = 3;
    private final static int FMI_FATAL = 4;
    private final static int FMI_PENDING = 5;
    static int loopCounter = 0;
    final AFunctionDeclaration fun = newAFunctionDeclaration(newAIdentifier("fixedStep"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());
    //    final AFunctionDeclaration funCsv = newAFunctionDeclaration(newAIdentifier("fixedStepCsv"),
    //            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime")),
    //                    newAFormalParameter(newAStringPrimitiveType(), newAIdentifier("csv_file_path"))), newAVoidType());
    //    final AFunctionDeclaration funCsvWs = newAFunctionDeclaration(newAIdentifier("fixedStepCsvWs"),
    //            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("stepSize")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
    //                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime")),
    //                    newAFormalParameter(newAStringPrimitiveType(), newAIdentifier("csv_file_path"))), newAVoidType());
    private final String data_HeadersIdentifier = "data_headers";
    private final String dataWriter = "dataWriter";
    private final String data_valuesIdentifier = "data_values";
    private final String data_configuration = "dataWriter_configuration";

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

    static PExp simLog(SimLogLevel level, String msg, PExp... args) {

        List<PExp> cargs = new Vector<>();
        cargs.addAll(Arrays.asList(newAIntLiteralExp(level.value), newAStringLiteralExp(msg)));
        for (PExp a : args) {
            cargs.add(a.clone());
        }

        return newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"), cargs);
    }

    static LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }

    static LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    private static String getTypeId(SPrimitiveType type) {
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

    /**
     * @param loopAccumulatingVars vars before while
     * @param list                 the list to iterate
     * @param size                 the list size
     * @param handler              (index, element) -> return what should happen
     * @return a block with the while
     */
    static ABlockStm loopComponents(Consumer<List<PStm>> loopAccumulatingVars, PExp list, PExp size, BiFunction<PExp, PExp, List<PStm>> handler) {
        String indexVarName = "fix_indexing_" + loopCounter++;
        List<PStm> body = new Vector<>();

        body.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(indexVarName)), newAIntLiteralExp(0)));

        loopAccumulatingVars.accept(body);

        List<PStm> whileBody = new Vector<>(handler.apply(newAIdentifierExp(indexVarName), arrayGet(list, newAIdentifierExp(indexVarName))));
        whileBody.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(indexVarName)),
                newPlusExp(newAIdentifierExp(newAIdentifier(indexVarName)), newAIntLiteralExp(1))));

        body.add(newWhile(newALessBinaryExp(newAIdentifierExp(newAIdentifier(indexVarName)), size.clone()), newABlockStm(whileBody)));

        return newABlockStm(body);
    }

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fun/*, funCsv, funCsvWs*/).collect(Collectors.toSet());
    }

    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {

        UnitRelationship unitRelationShip = (UnitRelationship) env;
        logger.info("Unfolding with fixed step: {}", declaredFunction.toString());

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        //        boolean withCsv = false;
        //        boolean withWs = declaredFunction == funCsvWs;
        AFunctionDeclaration selectedFun = fun;

        //        if (declaredFunction.equals(funCsv) || declaredFunction.equals(funCsvWs)) {
        //            selectedFun = funCsv;
        //            withCsv = true;
        //        }


        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (env == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        PStm componentDecl = null;
        String componentsIdentifier = "fix_components";

        List<LexIdentifier> knownComponentNames = null;

        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (!compDecl.isPresent()) {
                throw new ExpandException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            //clone for local use
            componentDecl = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(componentsIdentifier), compDecl.get().getType().clone(),
                    compDecl.get().getInitializer().clone()));


            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        if (knownComponentNames == null || knownComponentNames.isEmpty()) {
            throw new ExpandException("No components found cannot fixed step with 0 components");
        }

        final List<LexIdentifier> componentNames = knownComponentNames;

        Set<UnitRelationship.Relation> relations =
                env.getRelations(componentNames).stream().filter(r -> r.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External)
                        .collect(Collectors.toSet());

        PExp stepSize = formalArguments.get(1).clone();
        PExp startTime = formalArguments.get(2).clone();
        PExp endTime = formalArguments.get(3).clone();


        boolean supportsGetSetState =
                env.getInstances().stream().filter(f -> componentNames.stream().anyMatch(m -> m.getText().equals(f.getKey()))).allMatch(pair -> {
                    try {
                        return pair.getValue().modelDescription.getCanGetAndSetFmustate();
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
        logger.debug("Expand with get/set state: {}", supportsGetSetState);

        List<PStm> statements = new Vector<>();
        statements.add(componentDecl);

        LexIdentifier end = newAIdentifier("end");

        statements.add(newVariable(end, newAIntNumericPrimitiveType(), newMinusExp(endTime, stepSize)));

        LexIdentifier time = newAIdentifier("time");
        statements.add(newVariable(time, newARealNumericPrimitiveType(), startTime));
        String fix_stepSize = "fix_stepSize";
        statements.add(newVariable(fix_stepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));

        String fix_recoveryStepSize = "fix_recoveryStepSize";
        statements.add(newVariable(fix_recoveryStepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));

        String fix_recovering = "fix_recovering";
        statements.add(newVariable(fix_recovering, newABoleanPrimitiveType(), newABoolLiteralExp(false)));

        String fix_comp_states = "fix_comp_states";
        if (supportsGetSetState) {
            statements.add(newVariable(fix_comp_states, newANameType("FmuState"), componentNames.size()));
        }

        LexIdentifier fixedStepOverallStatus = newAIdentifier("fix_global_status");
        statements.add(newVariable(fixedStepOverallStatus, newABoleanPrimitiveType(), newABoolLiteralExp(false)));

        LexIdentifier compIndexVar = newAIdentifier("fix_comp_index");

        statements.add(newVariable(compIndexVar, newAIntNumericPrimitiveType(), newAIntLiteralExp(0)));


        //        Set<UnitRelationship.Variable> outputs =
        //                Stream.concat(componentNames.stream().map(componentName -> env.getVariablesToLogForComponent(componentName)))
        //        relations.stream().filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.OutputToInput).map(r -> r.getSource())

        Set<UnitRelationship.Relation> outputRelations =
                relations.stream().filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.OutputToInput).collect(Collectors.toSet());

        // outputs contains both outputs based on relations and outputs based on additional variables to log
        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> outputs =
                outputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                        s -> outputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s)).flatMap(r -> {
                            List<ModelDescription.ScalarVariable> outputs_ =
                                    env.getVariablesToLog(s.getText()).stream().map(x -> x.scalarVariable).collect(Collectors.toList());
                            //outputs_.add(r.getSource().scalarVariable.getScalarVariable());
                            return outputs_.stream();
                        }).distinct().collect(Collectors.groupingBy(sv -> sv.getType().type))));

        // We need to add the additional

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
                        .flatMap(h -> {
                            List<RelationVariable> outputs_ = env.getVariablesToLog(h.scalarVariable.instance.getText());
                            //outputs_.add(h.scalarVariable);
                            return outputs_.stream();
                            //return h.scalarVariable;
                        }).sorted(Comparator.comparing(getLogName::apply)).collect(Collectors.toMap(l -> l, r -> {


                    //the relation should be a one to one relation so just take the first one
                    RelationVariable fromVar = r;
                    PExp from = arrayGet(getBufferName(fromVar.instance, fromVar.getScalarVariable().type.type, UsageType.Out),
                            outputs.get(fromVar.instance).get(fromVar.getScalarVariable().getType().type).stream()
                                    .map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList())
                                    .indexOf(fromVar.scalarVariable.getName()));
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

        statements.add(newVariable(this.data_HeadersIdentifier, newAStringPrimitiveType(),
                variableNames.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList())));


        statements.add(newVariable(this.data_configuration, newANameType("DataWriterConfig"),
                call(this.dataWriter, "writeHeader", newAIdentifierExp(this.data_HeadersIdentifier))));


        Function<LexIdentifier, PExp> getCompStatusExp = comp -> arrayGet(fixedStepStatus, componentNames.indexOf(comp));

        Function<LexIdentifier, PStateDesignator> getCompStatusDesignator =
                comp -> newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(fixedStepStatus)),
                        newAIntLiteralExp(componentNames.indexOf(comp)));


        BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus = (inLoopAndMessage, list) -> {
            List<PStm> body = new Vector<>(Arrays.asList(newExpressionStm(
                    call("logger", "log", newAIntLiteralExp(4), newAStringLiteralExp(inLoopAndMessage.getValue() + " %d "),
                            arrayGet(fixedStepStatus, newAIdentifierExp((LexIdentifier) compIndexVar.clone())))),
                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")), newABoolLiteralExp(false))));

            if (inLoopAndMessage.getKey()) {
                body.add(newBreak());
            }

            list.getValue().add(newIf(newOr((newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_ERROR))),
                    (newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_FATAL)))), newABlockStm(body), null));
        };


        Consumer<List<PStm>> handleDoStepStatuses = list -> {

            list.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) fixedStepOverallStatus.clone()), newABoolLiteralExp(true)));
            list.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(0)));
            //check if all ok
            list.add(newWhile(newALessBinaryExp(newAIdentifierExp((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(componentNames.size())),
                    newABlockStm(Arrays.asList(

                            newIf(newNotEqual(arrayGet(newAIdentifierExp(fixedStepStatus), newAIdentifierExp((LexIdentifier) compIndexVar.clone())),
                                    newAIntLiteralExp(FMI_OK)), newABlockStm(Arrays.asList(
                                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) fixedStepOverallStatus.clone()),
                                            newABoolLiteralExp(false)),

                                    newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                                            Arrays.asList(newAIntLiteralExp(4), newAStringLiteralExp("doStep failed for %d - status code "),
                                                    newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                            Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))))))


                                    , newBreak())), null),


                            newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) compIndexVar.clone()),
                                    newPlusExp(newAIdentifierExp((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(1)))


                    ))));


            list.add(newIf(newNot(newAIdentifierExp((LexIdentifier) fixedStepOverallStatus.clone())), newABlockStm(Arrays.asList(


                    newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                            Arrays.asList(newAIntLiteralExp(4), newAStringLiteralExp("Deviating from normal execution. Handling exceptions %d"),
                                    newAIntLiteralExp(0)))),


                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) fixedStepOverallStatus.clone()), newABoolLiteralExp(true)),
                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(0)),
                    newVariable("discardObserved", newABoleanPrimitiveType(), newABoolLiteralExp(false)),
                    //check if all ok
                    newWhile(newALessBinaryExp(newAIdentifierExp((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(componentNames.size())),
                            newABlockStm(Arrays.asList(

                                    newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                                            Arrays.asList(newAIntLiteralExp(4), newAStringLiteralExp("Fmu index %d, status code %d"),
                                                    newAIdentifierExp((LexIdentifier) compIndexVar.clone()),
                                                    arrayGet(fixedStepStatus, newAIdentifierExp((LexIdentifier) compIndexVar.clone()))))),

                                    newIf(newNotEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                            Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))), newAIntLiteralExp(FMI_OK)),
                                            newABlockStm(Arrays.asList(
                                                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) fixedStepOverallStatus.clone()),
                                                            newABoolLiteralExp(false)),

                                                    newIf(newEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                            Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                            newAIntLiteralExp(FMI_PENDING)), newExpressionStm(
                                                            newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                                                                    Arrays.asList(newAIntLiteralExp(4), newAStringLiteralExp(
                                                                            "doStep failed for %d PENDING not supported- " + "status code "),
                                                                            newAArrayIndexExp(newAIdentifierExp(fixedStepStatus), Arrays.asList(
                                                                                    newAIdentifierExp((LexIdentifier) compIndexVar.clone())))))),
                                                            newIf(newOr((newEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                                    Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                                    newAIntLiteralExp(FMI_ERROR))), (newEqual(
                                                                    newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                                            Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                                    newAIntLiteralExp(FMI_FATAL)))),

                                                                    newExpressionStm(newACallExp(newAIdentifierExp("logger"), newAIdentifier("log"),
                                                                            Arrays.asList(newAIntLiteralExp(4),
                                                                                    newAStringLiteralExp("doStep failed for %d - status code "),
                                                                                    newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                                                            Arrays.asList(newAIdentifierExp(
                                                                                                    (LexIdentifier) compIndexVar.clone())))))),
                                                                    null)), newIf(newEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                            Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                            newAIntLiteralExp(FMI_DISCARD)), newABlockStm(newExpressionStm(
                                                            simLog(SimLogLevel.DEBUG, "Instance discarding %d",
                                                                    newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                            newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("discardObserved")),
                                                                    newABoolLiteralExp(true))

                                                    ), null)


                                                    , newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")),
                                                            newABoolLiteralExp(false)), newBreak())), null),


                                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) compIndexVar.clone()),
                                            newPlusExp(newAIdentifierExp((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(1)))


                            ))),


                    newIf(newNot(newAIdentifierExp(newAIdentifier("global_execution_continue"))),
                            !supportsGetSetState ? newBreak() : newIf(newAIdentifierExp("discardObserved"),


                                    loopComponents(beforeList -> {
                                        beforeList.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recoveryStepSize)),
                                                newAIdentifierExp(newAIdentifier(fix_stepSize))));

                                        beforeList
                                                .add(newVariable("fix_recover_real_status", newARealNumericPrimitiveType(), newARealLiteralExp(0d)));

                                    }, newAIdentifierExp(fixedStepStatus), newAIntLiteralExp(componentNames.size()), (index, comp) -> {

                                        return Arrays.asList(newIf(newEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                newAIntLiteralExp(FMI_DISCARD)), newABlockStm(Arrays.asList(

                                                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recovering)),
                                                        newABoolLiteralExp(true)),

                                                newAAssignmentStm(
                                                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(fixedStepStatus)),
                                                                newAIdentifierExp((LexIdentifier) compIndexVar.clone())),
                                                        call(arrayGet(componentsIdentifier, newAIdentifierExp((LexIdentifier) compIndexVar.clone())),
                                                                "getRealStatus", newAIdentifierExp("fix_recover_real_status"))),
                                                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recoveryStepSize)),
                                                        call("Math", "min", newAIdentifierExp(fix_recoveryStepSize),
                                                                newAIdentifierExp("fix_recover_real_status")))

                                        )), null), newExpressionStm(
                                                call(arrayGet(componentsIdentifier, newAIdentifierExp((LexIdentifier) compIndexVar.clone())),
                                                        "setState", arrayGet(fix_comp_states, index.clone()))));

                                    })


                                    , null), null), null)), null));

            //check for fatals


            //check for possible reduced stepping

        };

        try {
            statements.add(newALocalVariableStm(
                    newAVariableDeclaration(newAIdentifier(fixedStepStatus), newAArrayType(newAIntNumericPrimitiveType(), componentNames.size()),
                            newAArrayInitializer(
                                    IntStream.range(0, componentNames.size()).mapToObj(i -> newAIntLiteralExp(0)).collect(Collectors.toList())))));

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
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier("terminate"), Collections.emptyList())))
                    .collect(Collectors.toList()));
        };

        Consumer<List<PStm>> setAll = (list) ->
                //set inputs
                inputs.forEach((comp, map) -> map.forEach((type, vars) -> {
                    list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                            newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier(getFmiGetName(type, UsageType.In)),
                                    Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.In)), newAIntLiteralExp(vars.size()),
                                            newAIdentifierExp(getBufferName(comp, type, UsageType.In))))));
                    checkStatus.accept(Map.entry(true, "set failed"), Map.entry(comp, list));
                }));

        //get outputs
        BiConsumer<Boolean, List<PStm>> getAll = (inLoop, list) -> outputs.forEach((comp, map) -> map.forEach((type, vars) -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier(getFmiGetName(type, UsageType.Out)),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.Out)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.Out))))));
            checkStatus.accept(Map.entry(true, "get failed"), Map.entry(comp, list));
        }));


        Function<LexIdentifier, PExp> getCompStateDesignator = comp -> arrayGet(fix_comp_states, componentNames.indexOf(comp));

        //get states
        Consumer<List<PStm>> getAllStates = (list) -> outputs.forEach((comp, map) -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    call(newAIdentifierExp((LexIdentifier) comp.clone()), "getState", getCompStateDesignator.apply(comp))));
            checkStatus.accept(Map.entry(true, "get state failed"), Map.entry(comp, list));
        });

        //free states
        Consumer<List<PStm>> freeAllStates = (list) -> outputs.forEach((comp, map) -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    call(newAIdentifierExp((LexIdentifier) comp.clone()), "freeState", getCompStateDesignator.apply(comp))));
            checkStatus.accept(Map.entry(true, "free state failed"), Map.entry(comp, list));
        });


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

        Consumer<List<PStm>> doStep = (list) -> componentNames.forEach(comp -> {
            //int doStep(real currentCommunicationPoint, real communicationStepSize, bool noSetFMUStatePriorToCurrentPoint);
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier("doStep"),
                            Arrays.asList(newAIdentifierExp((LexIdentifier) time.clone()), newAIdentifierExp(fix_stepSize),
                                    newABoolLiteralExp(true)))));

            // checkStatusDoStep.accept(Map.entry(comp, list));
        });

        Consumer<List<PStm>> progressTime = list -> list.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) time.clone()),
                newPlusExp(newAIdentifierExp((LexIdentifier) time.clone()), newAIdentifierExp(fix_stepSize))));


        Consumer<List<PStm>> declareCsvBuffer = list -> {
            list.add(newALocalVariableStm(newAVariableDeclaration(newAIdentifier(this.data_valuesIdentifier),
                    newAArrayType(newAStringPrimitiveType(), variableNames.size()),
                    newAArrayInitializer(csvFields.values().stream().map(PExp::clone).collect(Collectors.toList())))));

            list.add(newExpressionStm(newACallExp(newAIdentifierExp(this.dataWriter), newAIdentifier("writeDataPoint"),
                    Arrays.asList(newAIdentifierExp(this.data_configuration), newAIdentifierExp("time"),
                            newAIdentifierExp(this.data_valuesIdentifier)))));
        };

        Consumer<List<PStm>> logCsvValues = list -> {
            List<PExp> values = new ArrayList<>(csvFields.values());
            //values.add(0, newAIdentifierExp("time"));
            for (int i = 0; i < values.size(); i++) {
                AArrayStateDesignator to =
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(this.data_valuesIdentifier)), newAIntLiteralExp(i));
                list.add(newAAssignmentStm(to, values.get(i).clone()));
            }

            list.add(newExpressionStm(newACallExp(newAIdentifierExp(this.dataWriter), newAIdentifier("writeDataPoint"),
                    Arrays.asList(newAIdentifierExp(this.data_configuration), newAIdentifierExp("time"),
                            newAIdentifierExp(this.data_valuesIdentifier)))));

        };

        List<PStm> loopStmts = new Vector<>();
        loopStmts.add(newIf(newAIdentifierExp(fix_recovering), newABlockStm(
                Arrays.asList(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_stepSize)), newAIdentifierExp(fix_recoveryStepSize)),
                        newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recovering)), newABoolLiteralExp(false)))),
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_stepSize)), stepSize.clone())));

        // get prior to entering loop
        getAll.accept(false, statements);
        //        if (withCsv) {
        declareCsvBuffer.accept(statements);
        //        }


        //exchange according to mapping
        exchangeData.accept(loopStmts);
        //set inputs
        setAll.accept(loopStmts);
        if (supportsGetSetState) {
            getAllStates.accept(loopStmts);
        }
        //do step
        doStep.accept(loopStmts);
        handleDoStepStatuses.accept(loopStmts);
        //get data
        getAll.accept(true, loopStmts);
        // time = time + STEP_SIZE;
        progressTime.accept(loopStmts);
        //        if (withCsv || withWs) {
        logCsvValues.accept(loopStmts);
        if (supportsGetSetState) {
            freeAllStates.accept(loopStmts);
        }
        //        }

        statements.add(newWhile(
                newAnd(newAIdentifierExp("global_execution_continue"), (newALessEqualBinaryExp(newAIdentifierExp(time), newAIdentifierExp(end)))),
                newABlockStm(loopStmts)));


        terminate.accept(statements);

        //        if (withCsv || withWs) {
        statements.add(newExpressionStm(newACallExp(newAIdentifierExp(this.dataWriter), newAIdentifier("close"), Arrays.asList())));
        //        }

        statements.add(newExpressionStm(newUnloadExp(Arrays.asList(newAIdentifierExp("logger")))));

        return Arrays.asList(newABlockStm(statements));
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

    enum SimLogLevel {
        TRACE(0),
        DEBUG(2),
        INFO(3),
        WARN(3),
        ERROR(4);

        public final int value;

        private SimLogLevel(int value) {
            this.value = value;
        }
    }

    enum UsageType {
        In,
        Out
    }

    static class MableBuilder {
        public static PStm newVariable(LexIdentifier name, PType type, PExp value) {
            return newVariable(name.getText(), type, value);
        }

        public static PStm newVariable(String name, PType type, PExp value) {
            return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), type.clone(), newAExpInitializer(value.clone())));
        }

        public static PStm newVariable(LexIdentifier name, PType type, List<PExp> values) {
            return newVariable(name.getText(), type, values);

        }

        public static PStm newVariable(String name, PType type, List<PExp> values) {
            return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), newAArrayType(type.clone(), values.size()),
                    newAArrayInitializer(values.stream().map(PExp::clone).collect(Collectors.toList()))));

        }

        public static PStm newVariable(String name, PType type, int size) {
            return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), newAArrayType(type.clone(), size), null));

        }

        public static PExp call(String object, String method, PExp... args) {
            return call(object, method, args == null ? null : Arrays.asList(args));
        }

        public static PExp call(String object, String method, List<PExp> args) {
            return call(newAIdentifierExp(object), method, args);
        }

        public static PExp call(PExp object, String method, List<PExp> args) {
            return newACallExp(object, newAIdentifier(method), args);
        }

        public static PExp call(PExp object, String method, PExp... args) {
            return newACallExp(object, newAIdentifier(method), Arrays.asList(args));
        }

        public static PExp call(String object, String method) {
            return call(object, method, (List<PExp>) null);
        }

        public static PExp call(String method, PExp... args) {
            return call(method, args == null ? null : Arrays.asList(args));
        }

        public static PExp call(String method, List<PExp> args) {
            return newACallExp(newAIdentifier(method), args);
        }

        public static PExp call(String method) {
            return call(method, (List<PExp>) null);
        }

        public static PExp arrayGet(String name, PExp index) {
            return newAArrayIndexExp(newAIdentifierExp(name), Collections.singletonList(index));
        }

        public static PExp arrayGet(PExp name, PExp index) {
            return newAArrayIndexExp(name.clone(), Collections.singletonList(index.clone()));
        }

        public static PExp arrayGet(String name, int index) {
            return arrayGet(name, newAIntLiteralExp(index));
        }


        public static PExp arrayGet(LexIdentifier name, AIntLiteralExp index) {
            return arrayGet(name.getText(), index);
        }

        public static PExp arrayGet(LexIdentifier name, int index) {
            return arrayGet(name.getText(), index);
        }
    }

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
