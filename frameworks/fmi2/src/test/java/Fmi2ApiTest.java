import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.intocps.maestro.framework.fmi2.api.Fmi2Builder.*;

public class Fmi2ApiTest {

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

        tank.step(1);
        tank2.step(1);
        controller.step(1);


        ifSwitchFmuScope.leave();


        whileScope.leave();

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
