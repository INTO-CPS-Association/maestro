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
package org.intocps.orchestration.coe.single;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.intocps.orchestration.coe.BasicTest;
import org.intocps.orchestration.coe.CoeMain;
import org.intocps.orchestration.coe.OnlineModelsCoSimTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by kel on 23/01/17.
 */
public class SingleShotTest {

    @BeforeClass
    public static void prepareFmus() throws IOException {
        OnlineModelsCoSimTest.downloadFmus();
    }

    @Test
    public void singeTestWatertank()
            throws InterruptedException, IOException,
            NanoHTTPD.ResponseException {
        File output = new File("target/oneshot/result.csv");
        output.getParentFile().mkdirs();
        String config = "src/test/resources/online-models/watertank-c_controller-20sim_tank/config.json";
        CoeMain.CMDHandler(new String[]{"--oneshot", "--configuration",
                config,
                "--result", output.getAbsolutePath(), "--starttime", "0.0",
                "--endtime", "30"});

        String parent =
                config.substring(0, config.lastIndexOf('/') + 1)
                        + "result.csv";

        InputStream resultInputStream0 = new FileInputStream(parent.replace('/', File.separatorChar));

        BasicTest.assertResultEqualsDiff(FileUtils.readFileToString(output), resultInputStream0);
    }
}
