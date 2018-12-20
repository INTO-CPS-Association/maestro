/*
* This file is part of the INTO-CPS toolchain.
*
* Copyright (c) 2017-CurrentYear, INTO-CPS Association,
* c/o Professor Peter Gorm Larsen, Department of Engineering
* Finlandsgade 22, 8200 Aarhus N.
*
* All rights reserved.
*
* THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
* THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
* ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
* RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL
* VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
*
* The INTO-CPS toolchain  and the INTO-CPS Association Public License
* are obtained from the INTO-CPS Association, either from the above address,
* from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
* GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
*
* This program is distributed WITHOUT ANY WARRANTY; without
* even the implied warranty of  MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
* BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
* THE INTO-CPS ASSOCIATION.
*
* See the full INTO-CPS Association Public License conditions for more details.
*/

/*
* Author:
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.orchestration.coe.hierarchical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.*;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.IFmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.modeldefinition.xml.NodeIterator;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.CoeObject;
import org.intocps.orchestration.coe.scala.IStateChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import scala.collection.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

/**
 * Created by kel on 04/10/2017.
 */
public class HierarchicalCoeComponent extends HierarchicalCoeStateComponent
		implements IFmiComponent, IExternalSignalHandler
{
	final static Logger logger = LoggerFactory.getLogger(HierarchicalCoeComponent.class);
	final HierarchicalCoeFmu fmu;
	private final RequestProcessors requestProcessors;
	private final String sessionId;
	private final Coe coeSession;
	final static ObjectMapper mapper = new ObjectMapper();

	boolean isRunning = false;
	Thread coeSimThread = null;
	StartMsgJson startMessage = null;
	double doStepTargetTime = 0;

	List<ModelDescription.ScalarVariable> inputs = null;
	List<ModelDescription.ScalarVariable> outputs = null;

	boolean firstInnerCoeExternalSignalsProcessing = true;

	final Map<ModelDescription.ScalarVariable, ModelConnection.Variable> innerOutputToOutputMapping;

	private final Semaphore simulationStepCompleted = new Semaphore(0, true);
	private final Semaphore simulationStepWaitForExternalInputs = new Semaphore(0, true);
	private ModelConnection.ModelInstance modelInstance;

	public HierarchicalCoeComponent(HierarchicalCoeFmu fmu) throws Exception
	{
		this.fmu = fmu;

		SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
		this.requestProcessors = new org.intocps.orchestration.coe.httpserver.RequestProcessors(sessionController);

		String data = IOUtils.toString(requestProcessors.processCreateSession().getData());

		JsonNode actualObj = mapper.readTree(data);
		this.sessionId = actualObj.get("sessionId").asText();
		logger.debug("Created hierarchical coe session: {}", this.sessionId);
		this.coeSession = sessionController.getCoe(sessionId);

		ModelDescription md = new ModelDescription(fmu.getModelDescription());
		this.outputs = md.getOutputs();
		this.inputs = new Vector<>();

		for (ModelDescription.ScalarVariable sv : md.getScalarVariables())
		{
			refToSv.put(sv.getValueReference(), sv);
			if (sv.type.start != null)
			{
				if (sv.causality == ModelDescription.Causality.Output)
					outputsSvToValue.put(sv, sv.type.start);
				else
					inputsSvToValue.put(sv, sv.type.start);
			}
			else if (sv.causality == ModelDescription.Causality.Output)
			{
				switch (sv.type.type){

					case Boolean:
						outputsSvToValue.put(sv, false);
						break;
					case Real:
						outputsSvToValue.put(sv, 0.0);
						break;
					case Integer:
						outputsSvToValue.put(sv, 0);
						break;
					case String:
						outputsSvToValue.put(sv, "");
						break;
					case Enumeration:
						outputsSvToValue.put(sv, 0);
						break;
				}
			}

			if (sv.causality == ModelDescription.Causality.Input)
			{
				this.inputs.add(sv);
			}

		}
		this.innerOutputToOutputMapping = createInnerOutputMapping();
	}

	private Map<ModelDescription.ScalarVariable, ModelConnection.Variable> createInnerOutputMapping()
			throws Exception
	{

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		Document doc = docBuilderFactory.newDocumentBuilder().parse(this.getFmu().getModelDescription());

		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();

		Map<ModelDescription.ScalarVariable, ModelConnection.Variable> map = new HashMap<>();

		for (Node n : new NodeIterator(ModelDescription.lookup(doc, xpath, "fmiModelDescription/VendorAnnotations/Tool[@name='COE']/Mappings/link")))
		{
			String name = n.getAttributes().getNamedItem("name").getNodeValue();
			Long valueReference = Long.valueOf(n.getAttributes().getNamedItem("valueReference").getNodeValue());

			ModelConnection.Variable va = ModelConnection.Variable.parse(name);
			map.put(refToSv.get(valueReference), va);
		}

		return map;
	}

	@Override public IFmu getFmu()
	{
		return this.fmu;
	}

	@Override public Fmi2Status setDebugLogging(boolean b, String[] strings)
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setupExperiment(boolean toleranceDefined,
			double tolerance, double startTime, boolean stopTimeDefined,
			double stopTime) throws FmuInvocationException
	{

		try
		{
			this.coeSession.externalSignalHandler_$eq(this);
			IFmuFactory factory = FmuFactory.customFactory;
			FmuFactory.customFactory = new HierarchicalCoeFactory();
			NanoHTTPD.Response response = requestProcessors.processInitialize(sessionId, fmu.getConfig());
			FmuFactory.customFactory = factory;

			String responseString = IOUtils.toString(response.getData());
			logger.debug("Initialized hierarchical coe session: {}", responseString);

			if(responseString.isEmpty())
			{
				logger.error("Failed to initialize.");
				return Fmi2Status.Fatal;
			}

			startMessage = new StartMsgJson();
			startMessage.startTime = startTime;
			startMessage.endTime = stopTime+0.1;

		} catch (IOException e)
		{
			e.printStackTrace();
			return Fmi2Status.Error;
		} catch (NanoHTTPD.ResponseException e)
		{
			e.printStackTrace();
			return Fmi2Status.Error;
		}
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status enterInitializationMode()
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status exitInitializationMode()
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status reset() throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status doStep(double currentCommunicationPoint,
			double communicationStepSize,
			boolean noSetFMUStatePriorToCurrentPoint)
			throws FmuInvocationException
	{
		doStepTargetTime = currentCommunicationPoint + communicationStepSize;
		if (!this.isRunning)
		{
			//start the nested COE
			this.coeSimThread = new Thread(() ->
			{
				try
				{
					requestProcessors.processSimulate(sessionId, mapper.writeValueAsString(startMessage), false);
				} catch (IOException e)
				{
					e.printStackTrace();
					isRunning = false;
				}
			});
			this.coeSimThread.start();
			this.isRunning = true;
		}

		simulationStepWaitForExternalInputs.release();
		simulationStepCompleted.acquireUninterruptibly();

		return Fmi2Status.OK;
	}

	@Override public CoeObject.GlobalState processExternalSignals(
			CoeObject.GlobalState newState)
	{
		if (firstInnerCoeExternalSignalsProcessing)
		{
			this.simulationStepWaitForExternalInputs.acquireUninterruptibly();
			firstInnerCoeExternalSignalsProcessing = false;

		}
		if (newState.time() >= doStepTargetTime)
		{
			//sync outputs back to the FMU interface

			for (Map.Entry<ModelDescription.ScalarVariable, ModelConnection.Variable> entry : this.innerOutputToOutputMapping.entrySet())
			{
				Iterator<scala.Tuple2<ModelDescription.ScalarVariable, Object>> iter = newState.instanceStates().get(entry.getValue().instance).get().state().iterator();

				while (iter.hasNext())
				{
					scala.Tuple2<ModelDescription.ScalarVariable, Object> n = iter.next();
					if (n._1().name.equals(entry.getValue().variable))
					{
						outputsSvToValue.put(entry.getKey(), n._2());
					}
				}
			}

			this.simulationStepCompleted.release();
			this.simulationStepWaitForExternalInputs.acquireUninterruptibly();
		}

		//merge FMU inputs with global state before internal coe resolve
		return GlobalStateMerger.merge(newState, this.modelInstance, this.inputsSvToValue);
	}

	@Override public double getRequiredSyncTime()
	{
		return doStepTargetTime;
	}

	@Override public void configure(
			Map<ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper> instances)
	{

		for (Map.Entry<ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper> entry : instances.entrySet())
		{
			if (entry.getValue().instance instanceof HierarchicalExternalFmuStubComponent)
			{
				this.modelInstance = entry.getKey();
				break;
			}
		}
	}

	@Override public Fmi2Status terminate() throws FmuInvocationException
	{

		return Fmi2Status.OK;
	}

	@Override public void freeInstance() throws FmuInvocationException
	{
		try
		{
			requestProcessors.processDestroy(sessionId);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override public boolean isValid()
	{
		return false;
	}

	@Override public FmuResult<Double> getMaxStepSize()
			throws FmiInvalidNativeStateException
	{
		return null;
	}

	@Override public FmuResult<Boolean> getBooleanStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	@Override public FmuResult<Fmi2Status> getStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	@Override public FmuResult<Integer> getIntegerStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	@Override public FmuResult<Double> getRealStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	@Override public FmuResult<String> getStringStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	public File getResult(){
		return this.coeSession.getResult();
	}

	/**
	 * Deprecated access to hierarchical coe
	 * @return
	 */
	public Coe getCoe() { return this.coeSession;}
}
