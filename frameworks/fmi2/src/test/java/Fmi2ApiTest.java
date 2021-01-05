import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder.*;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class Fmi2ApiTest {

    @Test
    @Ignore
    public void testLoop_Within_loop() {
        Fmi2Builder builder = null;
        LogicBuilder logic = null;
        Fmu2Api msd1Fmu = builder.createFmu(new File("."));
        Fmu2Api msd2Fmu = builder.createFmu(new File("."));
        Fmu2Api msd3Fmu = builder.createFmu(new File("."));

        Fmi2ComponentApi msd1 = msd1Fmu.create();
        Fmi2ComponentApi msd2 = msd2Fmu.create();
        Fmi2ComponentApi msd3 = msd3Fmu.create();


        msd1.getPort("x1").linkTo(msd2.getPort("x1"));
        msd1.getPort("v1").linkTo(msd2.getPort("v1"));
        msd2.getPort("fk").linkTo(msd1.getPort("fk"));
        msd2.getPort("z").linkTo(msd3.getPort("z"));
        msd3.getPort("G").linkTo(msd2.getPort("G"));

        // Initialization
        msd1.getSingle("x1");
        msd2.setSingle("x1");
        msd1.getSingle("v1");
        msd2.setSingle("v1");
        msd2.getSingle("fk");
        msd1.setSingle("fk");
        msd2.getSingle("z");
        msd3.setSingle("z");
        msd3.getSingle("G");
        msd2.setSingle("G");

        // Stepping
        msd1.getState();
        msd2.getState();
        msd3.getState();

        MBoolean until_step_accept = builder.createBoolean("until-step-accept");
        MInt maxStepAcceptAttempts = builder.createInteger("max_step_accept_attempts");
        maxStepAcceptAttempts.set(5);
        Scope while_step_scope = builder.getDefaultScope()
                .enterWhile(until_step_accept.and(logic.isLess(builder.primitivesCreator().createMInt(0), maxStepAcceptAttempts)));
        // Todo: What does iterate mean?
        MBoolean until_converged = builder.createBoolean("until-converged");
        MInt maxConvergeAttempts = builder.createInteger("max_converge_attempts");
        maxConvergeAttempts.set(5);
        Scope while_converged_scope =
                while_step_scope.enterWhile(until_converged.and(logic.isLess(builder.primitivesCreator().createMInt(0), maxConvergeAttempts)));

        TimeTaggedValue msd1_x1_old = builder.getCurrentLinkedValue(msd1.getPort("x1"));
        TimeTaggedValue msd1_v1_old = builder.getCurrentLinkedValue(msd1.getPort("v1"));
        TimeTaggedValue msd2_fk_old = builder.getCurrentLinkedValue(msd2.getPort("fk"));
        TimeTaggedValue msd2_z_old = builder.getCurrentLinkedValue(msd2.getPort("z"));
        TimeTaggedValue msd3_G_old = builder.getCurrentLinkedValue(msd3.getPort("G"));

        msd1.setSingle("fk");
        msd2.setSingle("v1");
        msd2.setSingle("G");
        TimeDeltaValue msd1StepTime = msd1.step(1.0);
        TimeDeltaValue msd2StepTime = msd2.step(1.0);
        TimeDeltaValue msd3StepTime = msd3.step(1.0);
        msd1.getSingle("x1");
        msd2.setSingle("x1");
        msd2.getSingle("fk");
        msd2.getSingle("z");
        msd3.getSingle("G");
        LogicBuilder.Predicate x1Pred = logic.fromExternalFunction("doesConverge", msd1_x1_old, msd1.getSingle("x1"));
        LogicBuilder.Predicate v1Pred = logic.fromExternalFunction("doesConverge", msd1_v1_old, msd1.getSingle("v1"));
        LogicBuilder.Predicate fkPred = logic.fromExternalFunction("doesConverge", msd2_fk_old, msd2.getSingle("fk"));
        LogicBuilder.Predicate zPred = logic.fromExternalFunction("doesConverge", msd2_z_old, msd2.getSingle("z"));
        LogicBuilder.Predicate gPred = logic.fromExternalFunction("doesConverge", msd3_G_old, msd3.getSingle("G"));

        IfScope convergenceIfScope = while_converged_scope.enterIf(x1Pred.and(v1Pred.and(fkPred.and(zPred.and(gPred)))).not());
        msd1.setState();
        msd2.setState();
        msd3.setState();
        convergenceIfScope.enterElse();
        // TODO: Set until_converged to True
        convergenceIfScope.leave();
        while_converged_scope.leave();

        IfScope equalStepIfScope =
                while_step_scope.enterIf(logic.isEqual(msd1StepTime, msd2StepTime).and(logic.isEqual(msd2StepTime, msd3StepTime)).not());
        msd1.setState();
        msd2.setState();
        msd3.setState();
        maxStepAcceptAttempts.decrement();
        equalStepIfScope.leave();
        while_step_scope.leave();


    }

    @Test
    @Ignore
    public void test() {

        Fmi2Builder builder = null;
        LogicBuilder logic = null;

        Fmu2Api controllerFmu = builder.createFmu(new File("."));
        Fmu2Api tankFmu = builder.createFmu(new File("."));
        Fmu2Api tank2Fmu = builder.createFmu(new File("."));

        Fmi2ComponentApi controller = controllerFmu.create();
        Fmi2ComponentApi tank = tankFmu.create();
        Fmi2ComponentApi tank2 = tank2Fmu.create();

        controller.getPort("valve").linkTo(tank.getPort("valve"));
        tank.getPort("level").linkTo(controller.getPort("level"));


        //builder whileScope sets target implicitly for all other components derived from it
        Scope whileScope = builder.getDefaultScope().enterWhile(logic.isLess(builder.getCurrentTime(), builder.getTime(10)));

        //lets describe two cases. Base if before time 20 other is after. We switch to another simulation but reuse some components

        IfScope ifSwitchFmuScope = whileScope.enterIf(logic.isLess(builder.getCurrentTime(), builder.getTime(20)));
        Scope thenScope = ifSwitchFmuScope.enterThen();//implicit, could be left out

        //get and share the values with every one linked
        controller.share(controller.get());

        TimeTaggedValue levelVal = tank.getSingle("level");

        //just to try an if lets alter a value at this point, we will not use it for this but for the stabilization where we check something and do
        // changes with state accordingly
        IfScope ifScope = whileScope.enterIf(logic.isGreater(levelVal, 10));
        levelVal = null;/*???*/
        ifScope.enterElse();
        //add something to else
        ifScope.leave();

        //back in loop;
        tank.share(tank.getPort("level"), levelVal);

        //        controller.set(controller.getPort("valve"), valveValue);
        //        controller.set();
        //        controller.set(controller.getPort("valve"));

        tank.step(1);
        tank.step(1);

        //optional
        thenScope.leave();

        ifSwitchFmuScope.enterElse();

        //break all links
        tank.getPort("level").breakLink();
        //break single link - do do it but we could
        //controller.getPort("valve").breakLink(tank.getPort("valve"));

        tank2.getPort("level2").linkTo(controller.getPort("level"));
        controller.getPort("valve").linkTo(tank2.getPort("valve2"));

        tank2.share(tank2.get());
        controller.share(controller.get());

        TimeDeltaValue deltaT = tank.step(1);
        tank2.step(deltaT);
        controller.step(deltaT);


        ifSwitchFmuScope.leave();


        whileScope.leave();

        tank.share(tank.get());
        controller.share(controller.get());
        tank.set();
        controller.set();


        TimeTaggedValue searchForConverged = null;//false
        //        TimeTaggedValue vOld = tank.getSingle("v");//get from linked data
        //
        //        TimeTaggedValue v = tank.getSingle("v");


        TimeTaggedState ts = tank.getState();
        TimeTaggedState cs = controller.getState();

        WhileScope convergeScppe = builder.getDefaultScope().enterWhile(logic.fromValue(searchForConverged).not());
        TimeTaggedValue vOld = builder.getCurrentLinkedValue(tank.getPort("v"));

        tank.set();
        controller.set();

        tank.step(1);
        controller.step(1);


        Map<Port, TimeTaggedValue> tankValues = tank.get();
        Map<Port, TimeTaggedValue> controllerValues = controller.get();
        TimeTaggedValue v = tank.getSingle("v");//actually find this on in tankValues


        IfScope ifConvergeScope = convergeScppe.enterIf(logic.fromExternalFunction("doesConverge", v, vOld, TimeTaggedValue.of(0.0111)));
        ifConvergeScope.enterElse();

        tank.setState(ts);
        controller.setState(cs);

        ifConvergeScope.leave();

        tank.share(tankValues);
        controller.share(controllerValues);
        /*
        v = tank.get("v")
        while(diff(v,tank.peak("v")) > 0.001)
        {
            controller.share(controller.get());
            tank.share(tank.get())
            controller.step(1);
            tank.step(1);
            scope.v = tank.get("v")
        }
        * */
    }
}
