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
package org.intocps.orchestration.coe.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FolderCompressor
{

	public static void compress(File input, File output) throws IOException
	{
		if (output.getParentFile() != null)
		{
			output.getParentFile().mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(output);

		ZipOutputStream zos = new ZipOutputStream(fos);

		scanAndAddEntries(zos, input, null);

		// close the ZipOutputStream
		zos.close();

	}

	static File[] scanAndAddEntries(ZipOutputStream zos, File folder,
			String base) throws IOException
	{

		List<File> items = new Vector<File>();

		if (base == null)
		{
			base = "";
		} else
		{
			base += "/";
		}

		for (File child : folder.listFiles())
		{
			if (child.isFile())
			{

				File fileToArchive = child;
				String name = base + child.getName();

				byte[] buffer = new byte[1024];

				FileInputStream fis = new FileInputStream(fileToArchive);

				zos.putNextEntry(new ZipEntry(name));

				int length;

				while ((length = fis.read(buffer)) > 0)
				{
					zos.write(buffer, 0, length);
				}

				zos.closeEntry();

				// close the InputStream
				fis.close();

				items.add(new File(base + child.getName()));

			} else
			{
				// items.add(new Item(base + child.getName() + File.separatorChar, 0, null));
				File[] childItems = scanAndAddEntries(zos, child, base
						+ child.getName());
				if (childItems != null)
				{
					for (File item : childItems)
					{
						items.add(item);
					}
				}
			}
		}

		return items.toArray(new File[items.size()]);
	}

}
