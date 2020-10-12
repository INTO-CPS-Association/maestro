package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.*;
import static org.intocps.maestro.plugin.LogUtil.simLog;

public class JacobianFixedStep {
    final static String fixedStepStatus = "fix_status";
    private final static int FMI_OK = 0;
    private final static int FMI_WARNING = 1;
    private final static int FMI_DISCARD = 2;
    private final static int FMI_ERROR = 3;
    private final static int FMI_FATAL = 4;
    private final static int FMI_PENDING = 5;
    private final static int FMI_STATUS_LAST_SUCCESSFUL = 2;
    static int loopCounter = 0;


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

        body.add(newVariable(indexVarName, newAIntNumericPrimitiveType(), newAIntLiteralExp(0)));

        loopAccumulatingVars.accept(body);

        List<PStm> whileBody = new Vector<>(handler.apply(newAIdentifierExp(indexVarName), arrayGet(list, newAIdentifierExp(indexVarName))));
        whileBody.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(indexVarName)),
                newPlusExp(newAIdentifierExp(newAIdentifier(indexVarName)), newAIntLiteralExp(1))));

        body.add(newWhile(newALessBinaryExp(newAIdentifierExp(newAIdentifier(indexVarName)), size.clone()), newABlockStm(whileBody)));

        return newABlockStm(body);
    }

    public static List<PStm> generate(FmiSimulationEnvironment env, IErrorReporter errorReporter, final List<LexIdentifier> componentNames,
            String componentsIdentifier, PExp stepSize, PExp startTime, PExp endTime,
            Set<FmiSimulationEnvironment.Relation> relations) throws ExpandException {
        FmiSimulationEnvironment unitRelationShip = (FmiSimulationEnvironment) env;
        Function<LexIdentifier, PStateDesignator> getCompStatusDesignator =
                comp -> newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(fixedStepStatus)),
                        newAIntLiteralExp(componentNames.indexOf(comp)));

        LexIdentifier end = newAIdentifier("end");
        LexIdentifier time = newAIdentifier("time");
        String fix_stepSize = "fix_stepSize";
        String fix_recoveryStepSize = "fix_recoveryStepSize";
        String fix_recovering = "fix_recovering";
        LexIdentifier fixedStepOverallStatus = newAIdentifier("fix_global_status");
        LexIdentifier compIndexVar = newAIdentifier("fix_comp_index");


        Function<LexIdentifier, PExp> getCompStatusExp = comp -> arrayGet(fixedStepStatus, componentNames.indexOf(comp));


        BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus = (inLoopAndMessage, list) -> {
            List<PStm> body = new Vector<>(Arrays.asList(newExpressionStm(
                    call("logger", "log", newAIntLiteralExp(4), newAStringLiteralExp(inLoopAndMessage.getValue() + " %d "),
                            arrayGet(fixedStepStatus, newAIdentifierExp((LexIdentifier) compIndexVar.clone())))),
                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)),
                            newABoolLiteralExp(false))));

            if (inLoopAndMessage.getKey()) {
                body.add(newBreak());
            }

            list.getValue().add(newIf(newOr((newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_ERROR))),
                    (newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_FATAL)))), newABlockStm(body), null));
        };

        Consumer<List<PStm>> terminate = list -> {
            list.addAll(componentNames.stream().map(comp -> newExpressionStm(
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier("terminate"), Collections.emptyList())))
                    .collect(Collectors.toList()));
        };


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


        StateHandler stateHandler = new StateHandler(componentNames, env, getCompStatusDesignator, checkStatus);


        DerivativesHandler derivativesHandler = new DerivativesHandler();
        DataExchangeHandler dataExchangeHandler = new DataExchangeHandler(relations, env, getCompStatusDesignator, checkStatus);
        DataWriterHandler dataWriter = new DataWriterHandler();
        List<PStm> dataWriterAllocateStms =
                dataWriter.allocate(dataExchangeHandler.getInputRelations(), dataExchangeHandler.getOutputs(), unitRelationShip);


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
                                                            simLog(LogUtil.SimLogLevel.DEBUG, "Instance discarding %d",
                                                                    newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                            newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("discardObserved")),
                                                                    newABoolLiteralExp(true))

                                                    ), null)


                                                    , newAAssignmentStm(
                                                            newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)),
                                                            newABoolLiteralExp(false)), newBreak())), null),


                                    newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) compIndexVar.clone()),
                                            newPlusExp(newAIdentifierExp((LexIdentifier) compIndexVar.clone()), newAIntLiteralExp(1)))


                            ))),


                    newIf(newNot(newAIdentifierExp(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE))),
                            !stateHandler.supportsGetSetState ? newBreak() : newIf(newAIdentifierExp("discardObserved"),

                                    new Supplier<PStm>() {
                                        @Override
                                        public PStm get() {
                                            List<PStm> list = new Vector<>();
                                            list.add(loopComponents(beforeList -> {
                                                beforeList.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recoveryStepSize)),
                                                        newAIdentifierExp(newAIdentifier(fix_stepSize))));

                                                beforeList.add(newVariable("fix_recover_real_status", newARealNumericPrimitiveType(),
                                                        newARealLiteralExp(0d)));

                                            }, newAIdentifierExp(fixedStepStatus), newAIntLiteralExp(componentNames.size()), (index, comp) -> {

                                                return Arrays.asList(newIf(newEqual(newAArrayIndexExp(newAIdentifierExp(fixedStepStatus),
                                                        Arrays.asList(newAIdentifierExp((LexIdentifier) compIndexVar.clone()))),
                                                        newAIntLiteralExp(FMI_DISCARD)), newABlockStm(Arrays.asList(

                                                        newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recovering)),
                                                                newABoolLiteralExp(true)),

                                                        newAAssignmentStm(newAArayStateDesignator(
                                                                newAIdentifierStateDesignator(newAIdentifier(fixedStepStatus)),
                                                                newAIdentifierExp((LexIdentifier) compIndexVar.clone())),
                                                                call(arrayGet(componentsIdentifier,
                                                                        newAIdentifierExp((LexIdentifier) compIndexVar.clone())), "getRealStatus",
                                                                        newAIntLiteralExp(FMI_STATUS_LAST_SUCCESSFUL),
                                                                        newAIdentifierExp("fix_recover_real_status"))),
                                                        newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recoveryStepSize)),
                                                                call("Math", "min", newAIdentifierExp(fix_recoveryStepSize),
                                                                        newMinusExp(newAIdentifierExp("fix_recover_real_status"),
                                                                                newAIdentifierExp("time")))), newExpressionStm(
                                                                simLog(LogUtil.SimLogLevel.DEBUG, "Recovery time set to: %f",
                                                                        newAIdentifierExp(fix_recoveryStepSize)))

                                                )), null), newExpressionStm(call(arrayGet(componentsIdentifier, index.clone()), "setState",
                                                        arrayGet(stateHandler.fix_comp_states, index.clone()))));


                                            }));
                                            list.addAll(stateHandler.freeAllStates());
                                            list.add(newAAssignmentStm(
                                                    newAIdentifierStateDesignator(newAIdentifier(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE)),
                                                    newABoolLiteralExp(true)));
                                            return newABlockStm(list);
                                        }
                                    }.get()


                                    , null), null), null)), null));


            //check for fatals


            //check for possible reduced stepping

        };


        List<PStm> loopStmts = new Vector<>();
        //handle recovery
        loopStmts.add(newIf(newAIdentifierExp(fix_recovering), newABlockStm(
                Arrays.asList(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_stepSize)), newAIdentifierExp(fix_recoveryStepSize)),
                        newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_recovering)), newABoolLiteralExp(false)))),
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fix_stepSize)), stepSize.clone())));


        //exchange according to mapping
        loopStmts.addAll(dataExchangeHandler.exchangeData());
        //set inputs
        loopStmts.addAll(dataExchangeHandler.setAll());
        loopStmts.addAll(derivativesHandler.set(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE));
        //get state
        loopStmts.addAll(stateHandler.getAllStates());
        //do step
        doStep.accept(loopStmts);
        //handle failures during stepping
        handleDoStepStatuses.accept(loopStmts);

        List<PStm> loopStmtsPost = new Vector<>();

        //

        //get data
        loopStmtsPost.addAll(dataExchangeHandler.getAll(true));
        loopStmtsPost.addAll(derivativesHandler.get(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE));

        progressTime.accept(loopStmtsPost);
        loopStmtsPost.addAll(dataWriter.write());
        loopStmtsPost.addAll(stateHandler.freeAllStates());

        loopStmts.add(newIf(
                newAnd(newAIdentifierExp(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE), newNot(newAIdentifierExp(newAIdentifier(fix_recovering)))),
                newABlockStm(loopStmtsPost), null));


        List<PStm> statements = new Vector<>();
        //pre allocation
        statements.add(newVariable(end, newAIntNumericPrimitiveType(), newMinusExp(endTime, stepSize)));
        statements.add(newVariable(time, newARealNumericPrimitiveType(), startTime));
        statements.add(newVariable(fix_stepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));
        statements.add(newVariable(fix_recoveryStepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));
        statements.add(newVariable(fix_recovering, newABoleanPrimitiveType(), newABoolLiteralExp(false)));
        statements.addAll(stateHandler.allocate());
        statements.add(newVariable(fixedStepOverallStatus, newABoleanPrimitiveType(), newABoolLiteralExp(false)));
        statements.add(newVariable(compIndexVar, newAIntNumericPrimitiveType(), newAIntLiteralExp(0)));
        statements.addAll(dataExchangeHandler.allocate());
        statements.add(newALocalVariableStm(
                newAVariableDeclaration(newAIdentifier(fixedStepStatus), newAArrayType(newAIntNumericPrimitiveType(), componentNames.size()),
                        newAArrayInitializer(
                                IntStream.range(0, componentNames.size()).mapToObj(i -> newAIntLiteralExp(0)).collect(Collectors.toList())))));
        // get prior to entering loop
        statements.addAll(dataExchangeHandler.getAll(false));
        statements.addAll(derivativesHandler.allocateMemory(componentNames, dataExchangeHandler.getInputRelations(), unitRelationShip));
        statements.addAll(derivativesHandler.get(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE));
        statements.addAll(dataWriterAllocateStms);
        statements.addAll(dataWriter.write());
        //loop
        statements.add(newWhile(newAnd(newAIdentifierExp(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE),
                (newALessEqualBinaryExp(newAIdentifierExp(time), newAIdentifierExp(end)))), newABlockStm(loopStmts)));
        //post simulation
        terminate.accept(statements);
        statements.addAll(dataWriter.deallocate());
        //statements.add(newExpressionStm(newUnloadExp(Arrays.asList(newAIdentifierExp("logger")))));
        return Collections.singletonList(newABlockStm(statements));
    }
}
