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
package org.intocps.maestro.fmi;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ModelDefinitionSchemaValidatorTests {
    //TODO: What is this testing?
    @Test
    public void test() throws IOException, SAXException {
        File f = new File("src/test/resources/modelDescription.xml".replace('/', File.separatorChar));

        FileInputStream in = FileUtils.openInputStream(f);

        InputStream resourceAsStream = Fmi2ModelDescription.class.getClassLoader().getResourceAsStream("fmi2ModelDescription.xsd");

        try {
            Fmi2ModelDescription.Companion.validateAgainstXSD(new StreamSource(in), new StreamSource(resourceAsStream));
        } catch (SAXParseException e) {

        }
    }

    @Test()
    public void modelDescriptionWithUnitDefinitionInWrongPosition() {
        Assertions.assertThrows(SAXParseException.class, () -> {
            File f = new File("src/test/resources/modelDescriptionUnitDefinition.xml".replace('/', File.separatorChar));

            FileInputStream in = FileUtils.openInputStream(f);

            InputStream resourceAsStream = Fmi2ModelDescription.class.getClassLoader().getResourceAsStream("fmi2ModelDescription.xsd");

            Fmi2ModelDescription.Companion.validateAgainstXSD(new StreamSource(in), new StreamSource(resourceAsStream));
        });
    }
}
