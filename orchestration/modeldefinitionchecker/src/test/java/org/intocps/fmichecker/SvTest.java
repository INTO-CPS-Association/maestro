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


package org.intocps.fmichecker;


import org.intocps.fmichecker.quotes.*;
import org.junit.Assert;
import org.junit.Test;
import org.overture.codegen.runtime.Tuple;

public class SvTest {

	@Test
	public void basicTest()
	{
		Orch.SV_X_ sv = new Orch.SV_X_(new inputQuote(), new fixedQuote(), null,
				new Orch.Type(new RealQuote(), 2));

		Tuple res = Orch.Validate(sv);

		Assert.assertFalse("Expected the SV value to be invalid: "+sv,(Boolean) res.get(0) );
	}

	@Test
	public void InputDiscreteAppTest()
	{
		Orch.SV_X_ sv = new Orch.SV_X_(new inputQuote(), new discreteQuote(), null,
				new Orch.Type(new StringQuote(), ""));

		Tuple res = Orch.Validate(sv);

		Assert.assertTrue("Expected the SV value to be valid: "+sv+". "+res.get(1),(Boolean) res.get(0) );
	}

}
