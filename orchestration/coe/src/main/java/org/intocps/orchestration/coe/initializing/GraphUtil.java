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

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultEdge;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by kel on 25/07/16.
 */
public class GraphUtil
{

	public enum EdgeType
	{
		InternalDependency, ExternalLink
	}

	/**
	 * show the graph in a jframe, returns when closed
	 *
	 * @param graph
	 */
	public static void show(DirectedGraph graph)
	{
		JFrame frame = new JFrame();
		frame.setSize(400, 400);
		//JGraph jgraph = new JGraph(new JGraphModelAdapter(graph));
		//frame.getContentPane().add(jgraph);
		frame.setVisible(true);

		JGraphXAdapter<Pair<FmiSimulationInstance, ModelDescription.ScalarVariable>, DefaultEdge> jgxAdapter = new JGraphXAdapter<Pair<FmiSimulationInstance, ModelDescription.ScalarVariable>, DefaultEdge>(graph);

		mxGraphComponent graphComponent = new mxGraphComponent(jgxAdapter);
		mxGraphModel graphModel = (mxGraphModel) graphComponent.getGraph().getModel();
		Collection<Object> cells = graphModel.getCells().values();
		mxUtils.setCellStyles(graphComponent.getGraph().getModel(), cells.toArray(), mxConstants.STYLE_ENDARROW, mxConstants.NONE);

		mxUtils.setCellStyles(graphComponent.getGraph().getModel(), cells.toArray(), mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		//frame.getContentPane().add(graphComponent);

		mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
		layout.execute(jgxAdapter.getDefaultParent());

		JScrollPane jScrollPane = new JScrollPane(graphComponent);
		// only a configuration to the jScrollPane...
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Then, add the jScrollPane to your frame
		frame.getContentPane().add(jScrollPane);
		//		frame.setSize(500, 500);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setVisible(true);

		try
		{
			while (true)
				if (frame.isVisible())
					Thread.sleep(1000);
				else
					break;
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		/*DOTExporter exporter = new DOTExporter();
		String targetDirectory = "testresults/graph/";
		new File(targetDirectory).mkdirs();
		exporter.export(new FileWriter(targetDirectory + "initial-graph.dot"), graph);*/
	}

	/**
	 * sorts the set into the connection path
	 *
	 * @param graph
	 * @param ports
	 * @param current
	 * @return
	 */
	public static List<Port> findConnectionsInOrder(
			DirectedGraph<Port, LabelledEdge> graph, Set<Port> ports,
			Port current, boolean completeSortOnly)
	{
		List<Port> sorted = new Vector<>();

		if (current == null)
		{
			current = ports.iterator().next();
			Set<Port> ports2 = new HashSet<>(ports);
			ports2.remove(current);
			sorted.addAll(findConnectionsInOrder(graph, ports2, current, completeSortOnly));
			return sorted;
		} else
		{
			sorted.add(current);

			for (Port p : ports)
			{
				LabelledEdge edge = graph.getEdge(current, p);

				Set<Port> ports2 = new HashSet<>(ports);
				ports2.remove(p);

				if (edge != null)
				{
					if (ports2.isEmpty())
					{
						sorted.add(p);
						break;
					} else
					{
						List<Port> res = findConnectionsInOrder(graph, ports2, p, completeSortOnly);
						if (res != null)
						{
							sorted.addAll(res);
						}
					}
				}
			}
		}

		if (!completeSortOnly || sorted.size() == ports.size() + 1)
			return sorted;
		else
			return null;

	}

	public static void dot(Graph graph)
	{
		DOTExporter<Port, LabelledEdge<EdgeType>> exporter = new DOTExporter<>(new IntegerNameProvider<>(), new VertexNameProvider<Port>()
		{
			@Override public String getVertexName(Port vertex)
			{
				return vertex.toString();
			}
		}, new EdgeNameProvider<LabelledEdge<EdgeType>>()
		{
			@Override public String getEdgeName(LabelledEdge edge)
			{
				return edge.toString();
			}
		});
		String targetDirectory = "target/graph/";
		new File(targetDirectory).mkdirs();
		try
		{
			exporter.export(new FileWriter(
					targetDirectory + "initial-graph.dot"), graph);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
