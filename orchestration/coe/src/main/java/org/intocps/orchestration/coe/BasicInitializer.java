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
package org.intocps.orchestration.coe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.cosim.base.CoSimInitializer;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.cosim.base.Tuple2;
import org.intocps.orchestration.coe.initializing.GraphBuilder;
import org.intocps.orchestration.coe.initializing.GraphUtil;
import org.intocps.orchestration.coe.initializing.LabelledEdge;
import org.intocps.orchestration.coe.initializing.Port;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Initial;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Types;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Variability;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.util.Util;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.ext.DOTExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;

public class BasicInitializer implements CoSimInitializer
{
	final static Logger logger = LoggerFactory.getLogger(BasicInitializer.class);
	private long lastExecTime = 0;
	private final Coe coe;

	static class FmiStateCache
	{
		Map<IFmiComponent, Map<Types, Map<Long, Object>>> cache = new HashMap<>();

		Map<IFmiComponent, Map<Types, Map<Long, Object>>> currentValue = new HashMap<>();

		public Object getCurrentValue(IFmiComponent instance, ScalarVariable sv)
		{
			return currentValue.get(instance).get(sv.type.type).get(sv.valueReference);
		}

		boolean hasCurrentValue(IFmiComponent instance, ScalarVariable sv)
		{
			return currentValue.containsKey(instance)
					&& currentValue.get(instance).get(sv.type.type).containsKey(sv.valueReference);
		}

		void addValue(IFmiComponent instance, ScalarVariable sv, Object value)
		{
			initMap(cache, instance);

			cache.get(instance).get(sv.type.type).put(sv.valueReference, value);

		}

		private void initMap(
				Map<IFmiComponent, Map<Types, Map<Long, Object>>> m,
				IFmiComponent instance)
		{
			if (!m.containsKey(instance))
			{
				HashMap<Types, Map<Long, Object>> map = new HashMap<>();
				for (Types t : Types.values())
				{
					map.put(t, new HashMap<>());
				}
				m.put(instance, map);
			}
		}

		void synchroniseInstances()
				throws InvalidParameterException, FmiInvalidNativeStateException
		{
			for (IFmiComponent instance : cache.keySet())
			{
				for (Types type : cache.get(instance).keySet())
				{
					Map<Long, Object> map = cache.get(instance).get(type);
					if (map.isEmpty())
						continue;
					Util.setRaw(instance, type, map);
					initMap(currentValue, instance);
					currentValue.get(instance).get(type).putAll(map);
					map.clear();
				}
			}
		}
	}

	public BasicInitializer(Coe coe)
	{
		this.coe = coe;
	}

	@Override public Map<ModelInstance, Map<ScalarVariable, Object>> initialize(
			Map<ModelInstance, Map<ScalarVariable, Tuple2<ModelInstance, ScalarVariable>>> inputs,
			Map<ModelInstance, FmiSimulationInstance> instances,
			List<ModelParameter> parameters)
			throws FmuInvocationException, AbortSimulationException
	{
		long execStartTime = System.currentTimeMillis();
		try
		{

			DirectedGraph<Port, LabelledEdge> graph = new GraphBuilder().buildAlgebraicLoopDetectionGraph(inputs, instances);

			//GraphUtil.show(graph);
			checkRealAlgebraicCycles(graph, instances);

			//collect independent ports to set first
			Set<Port> independentPorts = new HashSet<>();

			Set<Port> noInputEdgePorts = new HashSet<>();
			for (Port p : graph.vertexSet())
			{
				boolean noInComing = graph.incomingEdgesOf(p).isEmpty();
				if (noInComing && graph.outgoingEdgesOf(p).isEmpty())
				{
					independentPorts.add(p);
				} else if (noInComing)
				{
					noInputEdgePorts.add(p);
				}
			}

			FmiStateCache cache = new FmiStateCache();
			Set<Port> pendingPorts = new HashSet<>();

			logger.debug("Initialization, state = before initialization");
			logger.debug("Setting independent values");
			for (Port p : independentPorts)
			{
				if (p.sv.variability != Variability.Constant && (
						p.sv.initial == Initial.Exact
								|| p.sv.initial == Initial.Approx))
				{
					Object newVal = getNewVal(parameters, p.modelInstance, p.sv);
					logger.trace("Adding independent value to cache: {}", p);
					cache.addValue(p.simInstance.instance, p.sv, newVal);

				} else
				{
					pendingPorts.add(p);
				}

			}

			logger.debug("Setting no input values");
			for (Port p : noInputEdgePorts)
			{
				if (p.sv.variability != Variability.Constant && (
						p.sv.initial == Initial.Exact
								|| p.sv.initial == Initial.Approx))
				{
					Object newVal = getNewVal(parameters, p.modelInstance, p.sv);
					if (newVal != null)
					{
						logger.trace("Adding no-input value to cache: {}", p);
						cache.addValue(p.simInstance.instance, p.sv, newVal);
					}
				} else
				{
					pendingPorts.add(p);
				}

			}

			try
			{
				cache.synchroniseInstances();
			} catch (InvalidParameterException | FmiInvalidNativeStateException e)
			{
				logger.error("Failed to set before initialization parameters", e);
				throw new AbortSimulationException("Failed to set before initialization parameters", e);
			}

			logger.debug("Entering initialization mode");
			for (Map.Entry<ModelInstance, FmiSimulationInstance> simulationInstance : instances.entrySet())
			{
				Fmi2Status status = simulationInstance.getValue().instance.enterInitializationMode();
				if(status==Fmi2Status.Warning)
				{
					logger.warn("Received warning from 'enterInitializationMode' {}",simulationInstance.getKey());
				}

				if(!(status ==Fmi2Status.OK || status == Fmi2Status.Warning))
				{
					throw new AbortSimulationException("The call to 'enterInitializationMode' failed for: "+simulationInstance.getKey());
				}
			}

			try
			{
				checkAndSetInputs(instances, inputs);
			} catch (InvalidParameterException e)
			{
				logger.error("Failed when calculating and setting un connected inputs", e);
				throw new AbortSimulationException("Failed when calculating and setting un connected inputs", e);
			}

			logger.debug("Setting pending ports");
			for (Port p : pendingPorts)
			{
				Object newVal = getNewVal(parameters, p.modelInstance, p.sv);
				if (newVal != null)
				{
					logger.trace("Adding value to cache: {}", p);
					cache.addValue(p.simInstance.instance, p.sv, newVal);
				}
			}

			try
			{
				cache.synchroniseInstances();
			} catch (InvalidParameterException | FmiInvalidNativeStateException e)
			{
				logger.error("Failed to set in initialization", e);
				throw new AbortSimulationException("Failed to set initialization", e);
			}

			//now handle all outputs/inputs
			// there is no cycles left in the graph so we can recurse back to  the start of each graph path
			logger.debug("Setting remaining inputs");
			List<Port> portsToSet = graph.vertexSet().stream().filter(p ->
					p.sv.causality == ModelDescription.Causality.Input ||
							p.sv.causality
									== ModelDescription.Causality.Parameter
									&& p.sv.variability
									== Variability.Tunable).collect(Collectors.toList());

			Map<Port, Object> currentKnownPortValues = Stream.concat(independentPorts.stream(), noInputEdgePorts.stream()).filter(p -> cache.hasCurrentValue(p.simInstance.instance, p.sv)).collect(Collectors.toMap(Function.identity(), p -> cache.getCurrentValue(p.simInstance.instance, p.sv)));//.map(p -> cache.getCurrentValue(p.simInstance.instance, p.sv));

			while (!portsToSet.isEmpty())
			{
				Port p = portsToSet.iterator().next();
				portsToSet.remove(p);
				try
				{
					setPort(graph, null, currentKnownPortValues, parameters, p);
				} catch (InvalidParameterException | FmiInvalidNativeStateException e)
				{
					logger.error("Failed to set in initialization", e);
					throw new AbortSimulationException("Failed to set initialization", e);
				}
			}

			logger.debug("Exiting initialization mode");
			for (Map.Entry<ModelInstance, FmiSimulationInstance> simulationInstance : instances.entrySet())
			{
				Fmi2Status status = simulationInstance.getValue().instance.exitInitializationMode();

				if(status==Fmi2Status.Warning)
				{
					logger.warn("Received warning from 'exitInitializationMode' {}",simulationInstance.getKey());
				}

				if(!(status ==Fmi2Status.OK || status == Fmi2Status.Warning))
				{
					throw new AbortSimulationException("The call to 'exitInitializationMode' failed for: "+simulationInstance.getKey());
				}
			}

			//FIXME: the latter is old stuff
			for (ModelParameter modelParameter : parameters)
			{
				if (!modelParameter.isSet)
				{
					logger.warn("Initial parameter not found: {}", modelParameter.variable);
				}
			}

			logger.info("Initialization complete");

			//GraphUtil.dot(graph);

			return null;
		} finally
		{
			long execEndTime = System.currentTimeMillis();
			lastExecTime = execEndTime - execStartTime;
		}

	}

	@Override public long lastExecutionTimeMilis()
	{
		return lastExecTime;
	}

	private void checkAndSetInputs(
			Map<ModelInstance, FmiSimulationInstance> instances,
			Map<ModelInstance, Map<ScalarVariable, Tuple2<ModelInstance, ScalarVariable>>> inputs)
			throws InvalidParameterException, FmiInvalidNativeStateException,
			AbortSimulationException
	{
		Map<ModelInstance, List<ScalarVariable>> knownInputs = new HashMap<>();
		for (Map.Entry<ModelInstance, Map<ScalarVariable, Tuple2<ModelInstance, ScalarVariable>>> inPair : inputs.entrySet())
		{

			List<ScalarVariable> miKnownInputs = null;
			if (!knownInputs.containsKey(inPair.getKey()))
			{
				miKnownInputs = new Vector<>();
				knownInputs.put(inPair.getKey(), miKnownInputs);
			} else
			{
				miKnownInputs = knownInputs.get(inPair.getKey());
			}

			miKnownInputs.addAll(inPair.getValue().keySet());
		}

		for (Map.Entry<ModelInstance, List<ScalarVariable>> entry : knownInputs.entrySet())
		{
			List<String> names = entry.getValue().stream().map(s -> s.name).collect(Collectors.toList());

			List<ScalarVariable> inputsNotSet = instances.get(entry.getKey()).config.scalarVariables.stream().filter(p ->
					p.causality == ModelDescription.Causality.Input
							&& p.type.type == Types.Real
							&& !names.contains(p.name)).collect(Collectors.toList());

			if (!inputsNotSet.isEmpty())
			{
				for (ScalarVariable sv : inputsNotSet)
				{
					logger.warn("Input not connected for '{}.{}.{}'. It will be set to zero.", entry.getKey().key, entry.getKey().instanceName, sv.name);
					Fmi2Status status = instances.get(entry.getKey()).instance.setReals(new long[] {
							sv.getValueReference() }, new double[] { 0.0 });
					if (status != Fmi2Status.OK)
					{
						throw new AbortSimulationException(String.format("Setting input did not complete successfully for: '%s.%s.%s'", entry.getKey().key, entry.getKey().instanceName, sv.name));
					}
				}
			}
		}

	}

	private void setPort(DirectedGraph<Port, LabelledEdge> graph,
			Set<Port> portsAlreadySet, Map<Port, Object> currentKnownPortValues,
			List<ModelParameter> parameters, Port p)
			throws InvalidParameterException, FmuInvocationException
	{
		if (portsAlreadySet == null)
		{
			portsAlreadySet = new HashSet<>();
		}

		// check if already set
		if (currentKnownPortValues.containsKey(p))
			return;

		// set all in coming edges first
		Set<LabelledEdge> inComing = graph.incomingEdgesOf(p);

		for (LabelledEdge edge : inComing)
		{
			Port source = graph.getEdgeSource(edge);
			if (portsAlreadySet.contains(source))
			{
				continue;
			}
			portsAlreadySet.add(source);
			setPort(graph, portsAlreadySet, currentKnownPortValues, parameters, source);
		}

		//outputs can have internal dependencies aka in-coming so these needs to be handled and set first
		if (p.sv.causality == ModelDescription.Causality.Output)
		{
			//obtain output parameter
			currentKnownPortValues.put(p, Util.getRaw(p.simInstance.instance, new ScalarVariable[] {
					p.sv }, new long[] {
					p.sv.valueReference }, p.sv.type.type));
			return;//cannot be set
		}

		//check if override parameter / model description value exists
		Object newValue = getNewVal(parameters, p.modelInstance, p.sv);
		if (newValue == null)
		{
			// check if this is connected, if so obtain the value otherwise ignore
			if (inComing.isEmpty())
			{
				//no in coming connections to no value exists
				return;
			}
			LabelledEdge randomEdge = inComing.iterator().next();
			newValue = currentKnownPortValues.get(graph.getEdgeSource(randomEdge));
			if (newValue == null)
			{
				logger.warn("Unable to set input for {}.{}", p.modelInstance, p.sv.getName());
			}

		}

		final Object newFinalValue = newValue;
		Util.setRaw(p.simInstance.instance, p.sv.type.type, new HashMap<Long, Object>()
		{
			{
				put(p.sv.valueReference, newFinalValue);
			}
		});
		currentKnownPortValues.put(p, newValue);

	}

	private Object getNewVal(List<ModelParameter> parameters,
			ModelInstance comp, ScalarVariable sv)
	{
		Object newVal = null;

		if (sv.type.start != null)
		{
			newVal = sv.type.start;
		}

		for (ModelParameter par : parameters)
		{
			if (par.variable.toString().equals(comp + "." + sv.name))
			{
				newVal = par.value;
				par.isSet = true;
			}
		}

		if (sv.type.type == Types.Real)
		{
			if (newVal instanceof Integer)
			{
				newVal = (double) (int) newVal;
			}
		}

		return newVal;
	}

	private void checkRealAlgebraicCycles(
			DirectedGraph<Port, LabelledEdge> graph,
			Map<ModelInstance, FmiSimulationInstance> instances)
			throws AbortSimulationException
	{
		CycleDetector<Port, LabelledEdge> cycleDetector = new CycleDetector<>(graph);

		if (cycleDetector.detectCycles())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Cycle detected with connections: ");

			Set<Port> cycle = cycleDetector.findCycles();

			List<Port> sorted = GraphUtil.findConnectionsInOrder(graph, cycle, null, false);

			assert sorted != null;
			for (Iterator<Port> itr = sorted.iterator(); itr.hasNext(); )
			{
				sb.append(itr.next());
				if (itr.hasNext())
				{
					sb.append(" -> ");
				} else
				{
					sb.append(" -> loop");
				}
			}

			if (!coe.getConfiguration().isStabalizationEnabled)
			{
				throw new AbortSimulationException(
						"Initialization: " + sb.toString());
			} else
			{
				Set<ModelInstance> modelInstances = new HashSet<>();
				for (Port port : sorted)
				{
					modelInstances.add(port.modelInstance);
				}

				for (ModelInstance mi : modelInstances)
				{
					try
					{
						if (!instances.get(mi).config.modelDescription.getCanGetAndSetFmustate())
							logger.warn("Stabilization can not be fully activated since one of the FMUs does not support get/set state: {}", mi);
					} catch (XPathExpressionException e)
					{
						e.printStackTrace();
					}

				}
			}
		}
	}

}
