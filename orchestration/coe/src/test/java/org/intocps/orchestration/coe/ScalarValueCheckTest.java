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

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.fmi.VdmSvChecker;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Vector;

/**
 * Created by kel on 26/04/2017.
 */
public class ScalarValueCheckTest
{
	static ModelDescription.ScalarVariable mkSv(
			ModelDescription.Causality causality,
			ModelDescription.Variability variability,
			ModelDescription.Initial initial, ModelDescription.Type type)
	{
		ModelDescription.ScalarVariable sv = new ModelDescription.ScalarVariable();
		sv.name = "a";
		sv.causality = causality;
		sv.variability = variability;
		sv.initial = initial;

		sv.type = type;
		return sv;
	}

	static ModelDescription.Type mkString(Object start)
	{
		ModelDescription.Type t = new ModelDescription.StringType();
		t.start = start;
		return t;
	}

	static ModelDescription.Type mkBool(Object start)
	{
		ModelDescription.Type t = new ModelDescription.BooleanType();
		t.start = start;
		return t;
	}

	static ModelDescription.Type mkInt(Object start)
	{
		ModelDescription.Type t = new ModelDescription.IntegerType();
		t.start = start;
		return t;
	}

	static ModelDescription.Type mkReal(Object start)
	{
		ModelDescription.Type t = new ModelDescription.RealType();
		t.start = start;
		return t;
	}

	static VdmSvChecker.ScalarVariableConfigException check(
			ModelDescription.ScalarVariable sv)
	{
		try
		{
			List<ModelDescription.ScalarVariable> svs = new Vector<>();
			svs.add(sv);
			VdmSvChecker.validateModelVariables(svs);
			return null;
		} catch (VdmSvChecker.ScalarVariableConfigException e)
		{
			return e;
		}
	}

	@Test public void stringCheck()
			throws VdmSvChecker.ScalarVariableConfigException
	{
		Assert.assertNotNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkString(null))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkString(""))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkString("abc"))));
	}

	@Test public void boolCheck()
			throws VdmSvChecker.ScalarVariableConfigException
	{
		Assert.assertNotNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkBool(null))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkBool(true))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkBool(false))));
	}

	@Test public void intCheck()
			throws VdmSvChecker.ScalarVariableConfigException
	{
		Assert.assertNotNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkInt(null))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkInt(0))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkInt(1))));
	}

	@Test public void realCheck()
			throws VdmSvChecker.ScalarVariableConfigException
	{
		Assert.assertNotNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkReal(null))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkReal(0))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkReal(0.0))));
		Assert.assertNull(check(mkSv(ModelDescription.Causality.Input, ModelDescription.Variability.Discrete, null, mkReal(1.1))));
	}
}
