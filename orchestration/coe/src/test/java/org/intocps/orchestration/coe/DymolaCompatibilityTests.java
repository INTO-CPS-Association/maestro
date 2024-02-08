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

import fi.iki.elonen.NanoHTTPD;
import org.intocps.orchestration.coe.ConditionalIgnoreRule.ConditionalIgnore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

public class DymolaCompatibilityTests extends OnlineModelsCoSimTest
{
	@Rule
	public final ConditionalIgnoreRule mConditionalIgnore = new ConditionalIgnoreRule();

	//TODO: Overture toolwrapping FMUs has to be updated for mac catalina
	//See: https://github.com/overturetool/overture-fmu/issues/87
	@Test
	@ConditionalIgnore(condition = NonMac.class)
	public void testDymolaVarStepWithVdm()	throws IOException, NanoHTTPD.ResponseException
	{
		test("/dymola_varTest/config.json", 0, 30);
	}

}
