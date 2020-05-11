package org.intocps.maestro.plugin.InitializerWrapCoe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.initializing.Port;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.single.StubFactory;
import scala.Tuple2;
import scala.collection.*;
import scalaz.ProductDistributive;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.Map;
import java.util.Set;

public class SpecGen {
    public class InstanceVariable{
        public String instance;
        public ModelDescription.ScalarVariable scalarVariable;
    }

    public class InstanceConfiguration{
        public double startTime;
        public double endTime;
        public boolean endTimeDefined;
    }

    double endTime = 10;
    boolean endTimeDefined = true;
    double startTime = 0;

    public SpecGen() {
    }



    public PStm run(String json, String startMsg) throws JsonProcessingException {
        List<String> instances = null;
        HashMap<String, ModelDescription> instanceNameToMD = null;
        HashMap<String, ModelParameter> instanceNameToParameters = null;
        HashMap<String, HashMap<ModelDescription.ScalarVariable, InstanceVariable>> instanceInputsToOutput = null;
        HashMap<String, InstanceConfiguration> instanceToInstanceConfiguration = null;

        GiveMeInstanceData(lexName)

        // Load all FMUs --> REQUIRES FMU NAMES AND URIs
        List<PStm> fmuNames = createLoadStatements(fmus);

        // Create and initialize all instances:
        // REQUIRES FMU -> INSTANCE MAPPING
        // REQUIRES MODEL DESCRIPTION FILE
        // REQUIRES START TIME AND END TIME
        // REQUIRES PARAMETERS
        List<PStm> initializeStatements = new ArrayList<>();
                for(String instance : instances){
                    initializeStatements.addAll(initializeInstance(instance, instanceToInstanceConfiguration.get(instance), instanceNameToMD.get(instance), instanceNameToParameters.get(instance), instanceInputsToOutput.get(instance).keySet()));
                }




        // SET INDEPENDENT SCALAR VARIABLES --> REQUIRES MODEL DESCRIPTION ON INSTANCE LEVEL

        // SET/GET DEPENDENT SCALAR VARIABLES --> REQUIRES TOPOLOGICAL SORTED LIST
        // IF IT IS AN INPUT THAT SHOULD BE SET: THE RELATED OUTPUT WILL ALREADY HAVE BEEN RETRIEVED
        // WHERE IS THIS OUTPUT STORED? LOOK UP inputToOutputsMap
        // IF IT IS AN OUTPUT THAT INTERNALLY ON AN INPUT: THE INPUT WILL ALREADY HAVE BEEN SET

        return null;
    }

    /**
     *
     * @param instance the name of the instance
     * @param instanceConfiguration Configuration for the specific instance
     * @param modelDescription The modeldescription file of the instance
     * @param modelParameter Parameters for the instance
     * @param dependantInputs These are inputs that depend on outputs from other instances. Therefore, do not set them.
     * @return
     */
    private Collection<? extends PStm> initializeInstance(String instance, InstanceConfiguration instanceConfiguration, ModelDescription modelDescription, ModelParameter modelParameter, Set<ModelDescription.ScalarVariable> dependantInputs) {
        return null;
    }


}
