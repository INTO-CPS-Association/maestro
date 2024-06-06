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
package org.intocps.orchestration.coe.initializing;

import org.intocps.orchestration.coe.AbortSimulationException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.cosim.base.Tuple2;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Map;

/**
 * Created by kel on 25/07/16.
 */
public class GraphBuilder
{

	protected boolean accept(ModelDescription.ScalarVariable sv)
	{
		return true;// sv.causality == ModelDescription.Causality.Output|| sv.causality == ModelDescription.Causality.Input;
	}

	public DirectedGraph<Port, LabelledEdge> buildAlgebraicLoopDetectionGraph(
		Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> inputs,
		Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances)
		throws AbortSimulationException
	{

		DirectedGraph<Port, LabelledEdge> graph = new DefaultDirectedGraph<>(LabelledEdge.class);

		for (Map.Entry<ModelConnection.ModelInstance, FmiSimulationInstance> instance : instances.entrySet())
		{
			for (ModelDescription.ScalarVariable sv : instance.getValue().config.scalarVariables)
			{
				if (accept(sv))
				{
					Port port = new Port(instance.getKey(), instance.getValue(), sv);
					graph.addVertex(port);

					for (ModelDescription.ScalarVariable other : sv.outputDependencies.keySet())
					{
						if (accept(other)&&other != sv)
						{
							Port to = new Port(instance.getKey(), instance.getValue(), other);
							graph.addVertex(to);
							graph.addEdge(to, port, new LabelledEdge(GraphUtil.EdgeType.InternalDependency));
						}
					}
				}
			}
		}

		for (Map.Entry<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> output : inputs.entrySet())
		{
			for (Map.Entry<ModelDescription.ScalarVariable, Tuple2<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>> out : output.getValue().entrySet())
			{
				Port portOut = new Port(output.getKey(), instances.get(output.getKey()), out.getKey());
				Port portIn = new Port(out.getValue().first, instances.get(out.getValue().first), out.getValue().second);
				graph.addVertex(portOut);
				graph.addVertex(portIn);
				graph.addEdge(portIn, portOut, new LabelledEdge(GraphUtil.EdgeType.ExternalLink));
			}
		}

		return graph;
	}

}
