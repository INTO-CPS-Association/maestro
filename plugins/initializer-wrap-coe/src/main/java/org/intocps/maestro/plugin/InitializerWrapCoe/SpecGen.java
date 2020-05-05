package org.intocps.maestro.plugin.InitializerWrapCoe;

import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.single.StubFactory;
import scala.Tuple2;
import scala.collection.*;
import scalaz.ProductDistributive;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpecGen {
// Need to call initialize and presimulation on coe.
public SpecGen() {

    }

    public void run(String json, String startMsg){

        System.setProperty(FmuFactory.customFmuFactoryProperty, StatementFactory.class.getName());

        SessionController sc = new SessionController(new ProdSessionLogicFactory());
        String s = sc.createNewSession();
        RequestProcessors rp = new RequestProcessors(sc);
        try {
            rp.processInitialize(s, json);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NanoHTTPD.ResponseException e) {
            e.printStackTrace();
        }

        Coe coe = sc.getCoe(s);
        // Extract the mapping: Input -> Corresponding output
        scala.collection.immutable.Map<ModelConnection.ModelInstance,
                scala.collection.immutable.Map<ModelDescription.ScalarVariable,
                        Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>>
                inputOutputMapping = coe.init()._3();
        StatementContainer.getInstance().setInputOutputMapping(inputOutputMapping);

        Map<ModelConnection.ModelInstance, List<String>> logLevels = new HashMap<>();
        Coe.CoeSimulationHandle handle = coe.getSimulateControlHandle(0.0, 5.0, logLevels, false, 0);
        handle.preSimulation();
        System.out.println(StatementContainer.getInstance().getStatements());


    }


}
