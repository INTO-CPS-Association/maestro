//import Fmi2AMaBLBuilder.AMaBLVariableCreator;
//import Fmi2AMaBLBuilder.AMablBuilder;
//import Fmi2AMaBLBuilder.AMablFmi2ComponentAPI;
//import Fmi2AMaBLBuilder.AMablFmu2Api;
//import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
//import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder.*;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import javax.xml.xpath.XPathExpressionException;
//import java.io.File;
//import java.lang.reflect.InvocationTargetException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.Map;
//
//public class AMablFmi2ApiTest {
//
//    @Test
//    public void watertankExample() {
//        Fmi2SimulationEnvironmentConfiguration config = new Fmi2SimulationEnvironmentConfiguration();
//        config.
//                Fmi2SimulationEnvironment simEnv = new Fmi2SimulationEnvironment()
//    }
//
//    @Test
//    @Ignore
//    public void dummyTest() throws URISyntaxException, XPathExpressionException, InvocationTargetException, IllegalAccessException {
//        Fmi2SimulationEnvironment simEnv = null;
//        AMablBuilder builder = new AMablBuilder(simEnv);
//
//        // This returns a variableCreator that always follows the current scope.
//        AMaBLVariableCreator variableCreator = builder.variableCreator();
//        variableCreator.createTimeDeltaValue("step_size", 1.0);
//        AMablFmu2Api msd1Fmu = variableCreator.createFMU("msd1Fmu", new URI(""));
//        AMablFmu2Api msd2Fmu = variableCreator.createFMU("msd2Fmu", new URI(""));
//        AMablFmu2Api msd3Fmu = variableCreator.createFMU("msd3Fmu", new URI(""));
//        AMablFmi2ComponentAPI msd1 = msd1Fmu.create("msd1");
//        AMablFmi2ComponentAPI msd2 = msd2Fmu.create("msd2");
//        AMablFmi2ComponentAPI msd3 = msd3Fmu.create("msd3");
//
//        msd1.getPort("x1").linkTo(msd2.getPort("x1"));
//        msd1.getPort("v1").linkTo(msd2.getPort("v1"));
//        msd2.getPort("fk").linkTo(msd1.getPort("fk"));
//        msd2.getPort("z").linkTo(msd3.getPort("z"));
//        msd3.getPort("G").linkTo(msd2.getPort("G"));
//        msd1.getAndShare("x1");
//
//
//    }
//
//    @Test
//    @Ignore
//    public void testLoop_Within_loop() {
//        Fmi2Builder builder = null;
//        LogicBuilder logic = null;
//
//        Variable<TimeDeltaValue> step_size = builder.getRootScope().variableCreator().createTimeDeltaValue("step_size");
//        step_size.setValue(builder.getRootScope().literalCreator().createTimeDelta(1.0));
//
//        Fmu2Api msd1Fmu = builder.createFmu("msd1Fmu", new File("."));
//        Fmu2Api msd2Fmu = builder.createFmu("msd2Fmu", new File("."));
//        Fmu2Api msd3Fmu = builder.createFmu("msd3Fmu", new File("."));
//
//        Fmi2ComponentApi msd1 = msd1Fmu.create("msd1");
//        Fmi2ComponentApi msd2 = msd2Fmu.create("msd2");
//        Fmi2ComponentApi msd3 = msd3Fmu.create("msd3");
//
//
//        msd1.getPort("x1").linkTo(msd2.getPort("x1"));
//        msd1.getPort("v1").linkTo(msd2.getPort("v1"));
//        msd2.getPort("fk").linkTo(msd1.getPort("fk"));
//        msd2.getPort("z").linkTo(msd3.getPort("z"));
//        msd3.getPort("G").linkTo(msd2.getPort("G"));
//
//        // Initialization
//        msd1.getAndShare("x1");
//        msd2.set("x1");
//        msd1.share(msd1.get("v1"));
//        msd2.set("v1");
//        msd2.share(msd2.get("fk"));
//        msd1.set("fk");
//        msd2.getAndShare("z");
//        msd3.set("z");
//        msd3.getAndShare("G");
//        msd2.set("G");
//
//        // Stepping
//        msd1.getState();
//        msd2.getState();
//        msd3.getState();
//
//
//        Variable<MBoolean> until_step_accept = builder.getRootScope().variableCreator().createBoolean("until-step-accept");
//        Variable<MInt> maxStepAcceptAttempts = builder.getRootScope().variableCreator().createInteger("max_step_accept_attempts");
//        maxStepAcceptAttempts.setValue(builder.getRootScope().literalCreator().createMInt(5));
//        Variable<TimeDeltaValue> minimumStepSize = builder.getCurrentScope().variableCreator().createTimeDeltaValue("minimum_step");
//
//        Scope while_step_scope = builder.getRootScope().enterWhile(until_step_accept.getValue()
//                .and(logic.isLess(builder.getRootScope().literalCreator().createMInt(0), maxStepAcceptAttempts.getValue())));
//
//        Variable<MBoolean> until_converged = while_step_scope.variableCreator().createBoolean("until-converged");
//        Variable<MInt> maxConvergeAttempts = while_step_scope.variableCreator().createInteger("max_converge_attempts");
//        maxConvergeAttempts.setValue(while_step_scope.literalCreator().createMInt(5));
//        Scope while_converged_scope = while_step_scope.enterWhile(
//                until_converged.getValue().and(logic.isLess(while_step_scope.literalCreator().createMInt(0), maxConvergeAttempts.getValue())));
//
//        Value msd1_x1_old = builder.getCurrentLinkedValue(msd1.getPort("x1"));
//        Value msd1_v1_old = builder.getCurrentLinkedValue(msd1.getPort("v1"));
//        Value msd2_fk_old = builder.getCurrentLinkedValue(msd2.getPort("fk"));
//        Value msd2_z_old = builder.getCurrentLinkedValue(msd2.getPort("z"));
//        Value msd3_G_old = builder.getCurrentLinkedValue(msd3.getPort("G"));
//
//        msd1.set("fk");
//        msd2.set("v1");
//        msd2.set("G");
//        // Todo: Get minimum step
//        TimeDeltaValue msd1StepTime = msd1.step(minimumStepSize);
//        Variable<TimeDeltaValue> msd1StepTime_ = builder.getRootScope().variableCreator().createTimeDeltaValue("msd1StepDelta");
//        msd1StepTime_.setValue(msd1StepTime);
//        TimeDeltaValue msd2StepTime = msd2.step(minimumStepSize);
//        Variable<TimeDeltaValue> msd2StepTime_ = builder.getRootScope().variableCreator().createTimeDeltaValue("msd2StepDelta");
//        msd2StepTime_.setValue(msd2StepTime);
//        TimeDeltaValue msd3StepTime =
//                msd3.step(minimumStepSize); // TODO: Create utility function such that storing this outside loop is not necessary
//        Variable<TimeDeltaValue> msd3StepTime_ = builder.getRootScope().variableCreator().createTimeDeltaValue("msd3StepDelta");
//        msd3StepTime_.setValue(msd3StepTime);
//        minimumStepSize.setValue(builder.createTimeDeltaValue(
//                builder.getCurrentScope().doubleFromExternalFunction("getMinimum", msd1StepTime_, msd2StepTime_, msd3StepTime_)));
//
//
//        msd1.getAndShare("x1");
//        msd2.set("x1");
//        msd2.share(msd2.get("fk", "z"));// Collapsed 2 gets into 1. It is a filter
//        msd3.set("z");
//        msd3.share(msd3.get("G"));
//        LogicBuilder.Predicate x1Pred = builder.getCurrentScope().booleanFromExternalFunction("doesConverge", msd1_x1_old, msd1.getSingle("x1"));
//        LogicBuilder.Predicate v1Pred = builder.getCurrentScope().booleanFromExternalFunction("doesConverge", msd1_v1_old, msd1.getSingle("v1"));
//        LogicBuilder.Predicate fkPred = builder.getCurrentScope().booleanFromExternalFunction("doesConverge", msd2_fk_old, msd2.getSingle("fk"));
//        LogicBuilder.Predicate zPred = builder.getCurrentScope().booleanFromExternalFunction("doesConverge", msd2_z_old, msd2.getSingle("z"));
//        LogicBuilder.Predicate gPred = builder.getCurrentScope().booleanFromExternalFunction("doesConverge", msd3_G_old, msd3.getSingle("G"));
//
//        IfScope convergenceIfScope = while_converged_scope.enterIf(x1Pred.and(v1Pred.and(fkPred.and(zPred.and(gPred)))).not());
//        msd1.setState();
//        msd2.setState();
//        msd3.setState();
//        maxConvergeAttempts.getValue().decrement();
//        convergenceIfScope.enterElse();
//        // TODO: Set until_converged to True
//        convergenceIfScope.leave();
//        while_converged_scope.leave();
//
//        IfScope equalStepIfScope = while_step_scope.enterIf(logic.isEqual(msd1StepTime_.getValue(), msd2StepTime_.getValue())
//                .and(logic.isEqual(msd2StepTime_.getValue(), msd3StepTime_.getValue())).not());
//        msd1.setState();
//        msd2.setState();
//        msd3.setState();
//        maxStepAcceptAttempts.getValue().decrement();
//        equalStepIfScope.leave();
//        while_step_scope.leave();
//
//
//    }
//
//    @Test
//    @Ignore
//    public void test() {
//
//        Fmi2Builder builder = null;
//        LogicBuilder logic = null;
//
//        Fmu2Api controllerFmu = builder.createFmu("controllerFmu", new File("."));
//        Fmu2Api tankFmu = builder.createFmu("tankFmu", new File("."));
//        Fmu2Api tank2Fmu = builder.createFmu("tank2Fmu", new File("."));
//
//        Fmi2ComponentApi controller = controllerFmu.create("controller");
//        Fmi2ComponentApi tank = tankFmu.create("tank");
//        Fmi2ComponentApi tank2 = tank2Fmu.create("tank2");
//
//        controller.getPort("valve").linkTo(tank.getPort("valve"));
//        tank.getPort("level").linkTo(controller.getPort("level"));
//
//
//        //builder whileScope sets target implicitly for all other components derived from it
//        Scope whileScope = builder.getRootScope().enterWhile(logic.isLess(builder.getCurrentTime(), builder.getTime(10)));
//
//        //lets describe two cases. Base if before time 20 other is after. We switch to another simulation but reuse some components
//
//        IfScope ifSwitchFmuScope = whileScope.enterIf(logic.isLess(builder.getCurrentTime(), builder.getTime(20)));
//        Scope thenScope = ifSwitchFmuScope.enterThen();//implicit, could be left out
//
//        //get and share the values with every one linked
//        controller.share(controller.get());
//
//        Value levelVal = tank.getSingle("level");
//
//        //just to try an if lets alter a value at this point, we will not use it for this but for the stabilization where we check something and do
//        // changes with state accordingly
//        IfScope ifScope = whileScope.enterIf(logic.isGreater(levelVal, 10));
//        levelVal = null;/*???*/
//        ifScope.enterElse();
//        //add something to else
//        ifScope.leave();
//
//        //back in loop;
//        tank.share(tank.getPort("level"), levelVal);
//
//        //        controller.set(controller.getPort("valve"), valveValue);
//        //        controller.set();
//        //        controller.set(controller.getPort("valve"));
//
//        tank.step(1);
//        tank.step(1);
//
//        //optional
//        thenScope.leave();
//
//        ifSwitchFmuScope.enterElse();
//
//        //break all links
//        tank.getPort("level").breakLink();
//        //break single link - do do it but we could
//        //controller.getPort("valve").breakLink(tank.getPort("valve"));
//
//        tank2.getPort("level2").linkTo(controller.getPort("level"));
//        controller.getPort("valve").linkTo(tank2.getPort("valve2"));
//
//        tank2.share(tank2.get());
//        controller.share(controller.get());
//
//        TimeDeltaValue deltaT = tank.step(1);
//        tank2.step(deltaT);
//        controller.step(deltaT);
//
//
//        ifSwitchFmuScope.leave();
//
//
//        whileScope.leave();
//
//        tank.share(tank.get());
//        controller.share(controller.get());
//        tank.set();
//        controller.set();
//
//
//        Value searchForConverged = null;//false
//        //        TimeTaggedValue vOld = tank.getSingle("v");//get from linked data
//        //
//        //        TimeTaggedValue v = tank.getSingle("v");
//
//
//        TimeTaggedState ts = tank.getState();
//        TimeTaggedState cs = controller.getState();
//
//        WhileScope convergeScppe = builder.getRootScope().enterWhile(logic.fromValue(searchForConverged).not());
//        Value vOld = builder.getCurrentLinkedValue(tank.getPort("v"));
//
//        tank.set();
//        controller.set();
//
//        tank.step(1);
//        controller.step(1);
//
//
//        Map<Port, Value> tankValues = tank.get();
//        Map<Port, Value> controllerValues = controller.get();
//        Value v = tank.getSingle("v");//actually find this on in tankValues
//
//
//        IfScope ifConvergeScope = convergeScppe.enterIf(logic.fromExternalFunction("doesConverge", v, vOld, Value.of(0.0111)));
//        ifConvergeScope.enterElse();
//
//        tank.setState(ts);
//        controller.setState(cs);
//
//        ifConvergeScope.leave();
//
//        tank.share(tankValues);
//        controller.share(controllerValues);
//        /*
//        v = tank.get("v")
//        while(diff(v,tank.peak("v")) > 0.001)
//        {
//            controller.share(controller.get());
//            tank.share(tank.get())
//            controller.step(1);
//            tank.step(1);
//            scope.v = tank.get("v")
//        }
//        * */
//    }
//}
