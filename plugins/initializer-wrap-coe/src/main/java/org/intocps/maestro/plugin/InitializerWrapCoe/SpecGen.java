package org.intocps.maestro.plugin.InitializerWrapCoe;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerNew.Spec.StatementContainer;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.CoeObject;
import scala.Tuple2;
import scala.Tuple3;
import scala.collection.JavaConverters;
import scala.collection.immutable.Set;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpecGen {
    public SpecGen() {
    }


    public PStm run(List<LexIdentifier> knownComponentNames, String json, String startMsg, PExp startTime,
            PExp endTime) throws IOException, NanoHTTPD.ResponseException, UnfoldException {
        FmuFactory.customFactory = new StatementFactory();

        StatementContainer.getInstance().setInstances(knownComponentNames);

        SessionController sc = new SessionController(new ProdSessionLogicFactory());
        String s = sc.createNewSession();
        RequestProcessors rp = new RequestProcessors(sc);
        rp.processInitialize(s, json);

        Coe coe = sc.getCoe(s);

        // Initialize
        Tuple3<scala.collection.immutable.Map<ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper>, scala.collection.immutable.Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>>, scala.collection.immutable.Map<ModelConnection.ModelInstance, scala.collection.immutable.Map<ModelDescription.ScalarVariable, Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>>> initResult = coe
                .init();

        // Ensure that modelInstance names corresponding to knownComponentNames
        Map<ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper> converted = JavaConverters
                .mapAsJavaMapConverter(initResult._1()).asJava();

        if (!knownComponentNames.stream().map(l -> l.getText()).collect(Collectors.toSet())
                .containsAll(converted.keySet().stream().map(l -> l.instanceName).collect(Collectors.toSet()))) {
            throw new UnfoldException("The env configuration and plugin configuration does not match in terms of instance names");
        }

        // Extract the mapping: Input -> Corresponding output
        scala.collection.immutable.Map<ModelConnection.ModelInstance, scala.collection.immutable.Map<ModelDescription.ScalarVariable, Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> inputOutputMapping = initResult
                ._3();
        StatementContainer.getInstance().setInputOutputMapping(inputOutputMapping);

        StartMsgJson startMessage = new ObjectMapper().readValue(startMsg, StartMsgJson.class);
        Map<ModelConnection.ModelInstance, List<String>> logLevels = new HashMap<>();

        // Overridding loglevels. Will be removed in future.
        Coe.CoeSimulationHandle handle = coe
                .getSimulateControlHandle(startMessage.startTime, startMessage.endTime, logLevels, startMessage.reportProgress,
                        startMessage.liveLogInterval);

        // Overriding start time and end time
        StatementContainer.getInstance().startTime = startTime;
        StatementContainer.getInstance().endTime = endTime;

        handle.preSimulation();
        ABlockStm initializationStm = MableAstFactory.newABlockStm(StatementContainer.getInstance().getStatements());
        handle.postSimulation();
        rp.processDestroy(s);
        return initializationStm;
    }


}
