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

public class SecondOrderDerivativeEstimationAlgorithm implements IDerivativeEstimationAlgorithm {

    @Override
    public Double[] update(final Double[] x, final Double[] xPrev, final Double[] xPrevPrev, final Double dt, final Double dtPrev) {

        // xdot, xdotdot already provided
        if (x[2] != null) {
            return new Double[]{x[0], x[1], x[2]};
        }

        // xdot provided but not xdotdot
        if (x[1] != null) {
            Double xdotdot = 0.0;
            // if xPrev is not null and xPrev[xdot] is not null then compute xdotdot.
            if (xPrev != null && xPrev[1] != null) {
                xdotdot = (x[1] - xPrev[1]) / dt;
            }
            return new Double[]{x[0], x[1], xdotdot};
        }

        // No provided and no estimated derivatives.
        // Therefore first step of simulation.
        if (xPrev == null) {
            return new Double[]{x[0], 0.0, 0.0};
        }

        // No provided derivatives
        // Therefore second step of simulation
        if (dtPrev == null || xPrevPrev == null) {
            return new Double[]{x[0], (x[0] - xPrev[0]) / dt, 0.0};
        }

        // No provided derivatives
        // > third step of simulation as none of the cases above has been met.
        // https://mathformeremortals.wordpress.com/2013/01/12/a-numerical-second-derivative-from-three-points/
        final Double r1 = (x[0] - xPrev[0]) / dt;
        final Double r2 = (xPrevPrev[0] - xPrev[0]) / dtPrev;
        final Double a = (r1 + r2) / (dt + dtPrev);
        final Double b = -r2 - a * dtPrev;
        final Double xdotdot = 2 * a;
        final Double xdot = xdotdot * (dt + dtPrev) + b;


        return new Double[]{x[0], xdot, xdotdot};
    }
}
