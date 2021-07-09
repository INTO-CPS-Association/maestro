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
package org.intocps.maestro.webapi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by ctha on 23-02-2016.
 */
public class ZipDirectory {

    final static Logger logger = LoggerFactory.getLogger(ZipDirectory.class);

    public static byte[] zipDirectory(File dir) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ZipOutputStream zos = new ZipOutputStream(baos);
            addDir(dir, dir, zos);
            zos.close();
            baos.close();
            return baos.toByteArray();
        }
    }

    public static File zipDirectoryLarge(File dir, File tempFileBuffer) throws IOException {
        try (OutputStream baos = new FileOutputStream(tempFileBuffer)) {
            ZipOutputStream zos = new ZipOutputStream(baos);
            addDir(dir, dir, zos);
            zos.close();
            baos.close();
        }
        return tempFileBuffer;
    }

    public static void addDir(File root, File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();

        if(files != null){
            for (File file : files) {
                if (file.isDirectory()) {
                    addDir(root, file, out);
                    continue;
                }
                logger.trace("Adding file to archive ({}):  {}", root.getName(), file.getAbsolutePath());
                out.putNextEntry(new ZipEntry(file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)));

                FileInputStream input = null;

                try {
                    input = new FileInputStream(file);
                    IOUtils.copy(input, out);
                } finally {
                    IOUtils.closeQuietly(input);
                }
                out.closeEntry();
            }
        }
    }
}
