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

public class ScalarDerivativeEstimator {
    private final DerivativeEstimationAlgorithm algorithm;
    private final Integer order;
    private Double[] x = null;
    private Double[] xPrev = null;
    private Double[] xPrevPrev = null;
    private Double[] xPrevPrevPrev = null;
    private Double dt = null;
    private Double dtPrev = null;
    private Double dtPrevPrev = null;

    public ScalarDerivativeEstimator(final Integer order) {
        if (order == null) {
            algorithm = new SecondOrderDerivativeEstimationAlgorithm();
            this.order = 2;
            return;
        }

        algorithm = order.equals(1) ? new FirstOrderDerivativeEstimationAlgorithm() : new SecondOrderDerivativeEstimationAlgorithm();
        this.order = order;
    }

    public void advance(final Double[] xNew, final Double dtNew) {
        shiftBackward();
        x = algorithm.update(xNew, xPrev, xPrevPrev, dtNew, dtPrev);
        dt = dtNew;
    }

    public void rollback() {
        shiftForward();
    }

    public Double getDerivative(final Integer order) {
        if (x != null && x[order] != null) {
            return x[order];
        }
        return null;
    }

    public Double getFirstDerivative() {
        return x[1];
    }

    public Double getSecondDerivative() {
        return x[2];
    }

    public Integer getOrder() {
        return order;
    }

    private void shiftBackward() {
        xPrevPrevPrev = xPrevPrev;
        xPrevPrev = xPrev;
        xPrev = x;

        dtPrevPrev = dtPrev;
        dtPrev = dt;
    }

    private void shiftForward() {
        x = xPrev;
        xPrev = xPrevPrev;
        xPrevPrev = xPrevPrevPrev;
        xPrevPrevPrev = null;

        dt = dtPrev;
        dtPrev = dtPrevPrev;
        dtPrevPrev = null;
    }
}
