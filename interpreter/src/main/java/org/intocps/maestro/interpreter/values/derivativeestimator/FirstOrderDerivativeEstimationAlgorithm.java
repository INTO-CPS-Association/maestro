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
package org.intocps.maestro.interpreter.values.derivativeestimator;


public class FirstOrderDerivativeEstimationAlgorithm implements IDerivativeEstimationAlgorithm {

    @Override
    public Double[] update(final Double[] x, final Double[] xPrev, final Double[] xPrevPrev, final Double dt, final Double dtPrev) {
        // xdot already provided
        if (x[1] != null) {
            return new Double[]{x[0], x[1], x[2]};
        }

        // xdot not provided
        // first step of simulation
        if (xPrev == null) {
            return new Double[]{x[0], 0.0, x[2]};
        }

        // at least second step of simulation
        return new Double[]{x[0], (x[0] - xPrev[0]) / dt, x[2]};
    }
}
