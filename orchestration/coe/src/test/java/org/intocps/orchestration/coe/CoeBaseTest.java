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

import org.intocps.orchestration.coe.ConditionalIgnoreRule.IgnoreCondition;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.junit.BeforeClass;
import org.junit.Rule;

public class CoeBaseTest
{
	public static class DymolaLicenseWin32 implements IgnoreCondition {
		@Override
		public boolean isSatisfied() {
			return new HasDymolaLicense().isSatisfied()||new Win32Only().isSatisfied(); //&& (new Win32Only().isSatisfied() == false);
		}
	}

	public static class NonMac extends RunOnlyOn{
		public NonMac(){super(Platform.Win32, Platform.Win64, Platform.Linux32, Platform.Linux64);}
	}

	public static class MacOnly extends RunOnlyOn
	{
		public MacOnly()
		{
			super(Platform.Mac);
		}
	}

	public static class Win32Only extends RunOnlyOn
	{
		public Win32Only()
		{
			super(Platform.Win32);
		}
	}

	public static class Win64Only extends RunOnlyOn
	{
		public Win64Only()
		{
			super(Platform.Win64);
		}
	}

	public static class HasDymolaLicense extends HasEnvironmentVariable
	{
		public HasDymolaLicense() {super("DYMOLA_RUNTIME_LICENSE");}
	}

	public static class HasEnvironmentVariable implements IgnoreCondition
	{
		private String environmentVariable;
		public HasEnvironmentVariable(String envVar) {
			environmentVariable = envVar;
		}

		@Override
		public boolean isSatisfied() {
			return System.getenv(environmentVariable) == null;
		}
	}

	public static class RunOnlyOn implements IgnoreCondition
	{
		public enum Platform
		{
			Mac("Mac", "x86_64"),

			Win32("Windows", "x86"),

			Win64("Windows", "amd64"),

			Linux32("Linux", "x86"),

			Linux64("Linux", "amd64");

			public final String osName;
			public final String arch;

			private Platform(String osName, String arch)
			{
				this.osName = osName;
				this.arch = arch;
			}
		}

		private Platform[] platforms;

		public RunOnlyOn(Platform... platforms)
		{
			this.platforms = platforms;
		}

		public boolean isSatisfied()
		{
			String osName = System.getProperty("os.name");

			int index = osName.indexOf(' ');
			if (index != -1)
			{
				osName = osName.substring(0, index);
			}

			String arch = System.getProperty("os.arch");

			for (Platform platform : platforms)
			{
				if (platform.osName.equalsIgnoreCase(osName)
						&& platform.arch.equals(arch))
				{
					return false;
				}
			}
			return true;
			// return System.getProperty("os.name").startsWith("Windows");
		}
	}

	@BeforeClass
	public static void setupClass()
	{
		SessionController.test = true;
	}

	@Rule
	public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();
}
