package org.intocps.maestro.plugin.InitializerWrapCoe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.ast.ABlockStm;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
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
    public SpecGen() {
    }



    public PStm run(String json, String startMsg) throws JsonProcessingException {


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

        StartMsgJson startMessage = new ObjectMapper().readValue(startMsg, StartMsgJson.class);
        Map<ModelConnection.ModelInstance, List<String>> logLevels = new HashMap<>();
        // Overridding loglevels. Will be removed in future.
        Coe.CoeSimulationHandle handle = coe.getSimulateControlHandle(startMessage.startTime, startMessage.endTime, logLevels, startMessage.reportProgress, startMessage.liveLogInterval);

        handle.preSimulation();
        ABlockStm initializationStm = MableAstFactory.newABlockStm(StatementContainer.getInstance()
                .getStatements());
        return initializationStm;
    }


}
