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
*		Oliver Kotte
*		Alexander Kluber
*		Kenneth Lausdahl
*		Casper Thule
*/

/*
* Author:
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.orchestration.coe.cosim.varstep.constraint;

import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Type;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtil
{
	private static final String GUID = "{12345}";
	private static final String INSTANCE = "instance";

	public static Map<ModelInstance, Map<ScalarVariable, Object>> setOutputValue(
			final Double da)
	{
		final ModelInstance mi = new ModelInstance(GUID, INSTANCE);
		final ScalarVariable a = makeScalarRealOutputVariable("A");

		final Map<ScalarVariable, Object> m = new HashMap<ScalarVariable, Object>();
		m.put(a, da);

		final Map<ModelInstance, Map<ScalarVariable, Object>> values = new HashMap<ModelInstance, Map<ScalarVariable, Object>>();
		values.put(mi, m);

		return values;
	}

	public static Map<ModelInstance, Map<ScalarVariable, Object>> setOutputValues(
			final Double da, final Double db)
	{
		final ModelInstance mi = new ModelInstance(GUID, INSTANCE);
		final ScalarVariable a = makeScalarRealOutputVariable("A");
		final ScalarVariable b = makeScalarRealOutputVariable("B");

		final Map<ScalarVariable, Object> m = new HashMap<ScalarVariable, Object>();
		m.put(a, da);
		m.put(b, db);

		final Map<ModelInstance, Map<ScalarVariable, Object>> values = new HashMap<ModelInstance, Map<ScalarVariable, Object>>();
		values.put(mi, m);

		return values;
	}

	public static Constraint makeBoundDifferenceConstraint()
	{
		final Constraint jc = new Constraint();
		jc.abstol = 1E-3;
		jc.reltol = 1E-4;
		jc.type = "bounddifference";
		jc.order = 2;
		jc.setId("id");

		final List<String> ports = new ArrayList<String>();
		ports.add(GUID + "." + INSTANCE + ".A");
		ports.add(GUID + "." + INSTANCE + ".B");
		jc.ports = ports;

		return jc;
	}

	public static Constraint makeZerocrossingConstraint()
	{
		final Constraint jc = new Constraint();
		jc.abstol = 1E-3;
		jc.reltol = 1E-4;
		jc.type = "zerocrossing";
		jc.order = 2;
		jc.setId("id");

		final List<String> ports = new ArrayList<String>();
		ports.add(GUID + "." + INSTANCE + ".A");
		ports.add(GUID + "." + INSTANCE + ".B");
		jc.ports = ports;

		return jc;
	}

	public static Constraint makeScalarZerocrossingConstraint()
	{
		final Constraint jc = new Constraint();
		jc.abstol = 1E-3;
		jc.reltol = 1E-4;
		jc.type = "zerocrossing";
		jc.order = 2;
		jc.setId("id");

		final List<String> ports = new ArrayList<String>();
		ports.add(GUID + "." + INSTANCE + ".A");
		jc.ports = ports;

		return jc;
	}

	public static Constraint makeSamplingRateConstraint()
	{
		final Constraint jc = new Constraint();
		jc.base = 0;
		jc.rate = 1;
		jc.startTime = 5;
		jc.type = "samplingrate";
		jc.setId("id");

		final List<String> ports = new ArrayList<String>();
		ports.add(GUID + "." + INSTANCE + ".A");
		jc.ports = ports;

		return jc;
	}

	private static ScalarVariable makeScalarRealOutputVariable(final String name)
	{
		final ScalarVariable v = new ScalarVariable();
		v.name = name;
		v.causality = ModelDescription.Causality.Output;
		v.type = new Type();
		v.type.type = Types.Real;
		return v;
	}

}
