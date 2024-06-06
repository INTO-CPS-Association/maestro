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

import org.intocps.fmi.IFmu;

import java.io.File;
import java.net.URI;

public interface IFmuFactory
{
	/**
	 * File prefix for session relative files
	 */
	public static final String SESSION_SCHEME = "session";

	/**
	 * The accept method returns true if this factory will handle instantiation of the file into an FMU
	 * 
	 * @param uri
	 *            the file
	 * @return true, if handled by this factory
	 */
	boolean accept(URI uri);

	/**
	 * Instantiates an FMU from the file
	 * 
	 * @param sessionRoot
	 *            the root of the session
	 * @param uri
	 *            the FMU file or directory
	 * @return an instance of an FMU
	 * @throws Exception
	 *             throws an exception for any internal error
	 */
	IFmu instantiate(File sessionRoot, URI uri) throws Exception;
}
