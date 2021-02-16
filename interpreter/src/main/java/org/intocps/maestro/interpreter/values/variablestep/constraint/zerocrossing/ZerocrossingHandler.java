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
package org.intocps.maestro.interpreter.values.variablestep.constraint.zerocrossing;

import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint;
import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint.Operation;
import org.intocps.maestro.interpreter.values.variablestep.InitializationMsgJson;
import org.intocps.maestro.interpreter.values.variablestep.StepsizeInterval;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintHandler;
import org.intocps.maestro.interpreter.values.variablestep.constraint.zerocrossing.detection.ZerocrossingConstraintState;
import org.intocps.maestro.interpreter.values.variablestep.constraint.zerocrossing.detection.ZerocrossingDetector;
import org.intocps.maestro.interpreter.values.variablestep.constraint.zerocrossing.stepsize.DefaultZcStepsizeAdjustmentStrategy;
import org.intocps.maestro.interpreter.values.variablestep.constraint.zerocrossing.stepsize.ZcStepsizeAdjustmentStrategy;
import org.intocps.maestro.interpreter.values.variablestep.valuetracker.OptionalDifferenceTracker;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

public class ZerocrossingHandler implements Observer, ConstraintHandler {

    final static Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private final Double tol;
    private final String id;
    private final StepsizeInterval interval;
    private final ZerocrossingDetector detector;
    private final ZcStepsizeAdjustmentStrategy stepsizeAdjustmentStrategy;
    private ZerocrossingHandler previousState = null;
    private Double prevStepsize;
    private Double lastStepsizeLimitedByContinuousConstraint;
    private Double currentTime;
    private Double nextTime;

    public ZerocrossingHandler(final ZerocrossingHandler zh) {
        tol = zh.tol;
        id = zh.id;
        prevStepsize = zh.prevStepsize;
        lastStepsizeLimitedByContinuousConstraint = zh.lastStepsizeLimitedByContinuousConstraint;
        currentTime = zh.currentTime;
        nextTime = zh.nextTime;
        interval = zh.interval;
        detector = zh.detector;
        stepsizeAdjustmentStrategy = zh.stepsizeAdjustmentStrategy;
    }

    public ZerocrossingHandler(final Observable observable, final InitializationMsgJson.Constraint constraint, final StepsizeInterval interval,
            final Double strongRelaxationFactor) {
        final OptionalDifferenceTracker tracker = new OptionalDifferenceTracker(observable, constraint.getPorts(), constraint.getOrder());
        detector = new ZerocrossingDetector(tracker);
        tol = constraint.getAbsoluteTolerance();
        id = constraint.getId();
        this.interval = interval;
        observable.addObserver(this);
        stepsizeAdjustmentStrategy = new DefaultZcStepsizeAdjustmentStrategy(tracker, tol, interval, strongRelaxationFactor, constraint.getSafety());
    }

    @Override
    public void update(final Observable obs, final Object arg) {
        if (obs instanceof CurrentSolutionPoint) {
            CurrentSolutionPoint cs = (CurrentSolutionPoint) obs;
            final Operation op = cs.getOperation();

            if (Operation.ADVANCE.equals(op)) {
                previousState = new ZerocrossingHandler(this);
                prevStepsize = cs.getPrevStepsize();
                lastStepsizeLimitedByContinuousConstraint = cs.getLastStepsizeLimitedByContinuousConstraint();
                currentTime = cs.getCurrentTime();
                nextTime = null;
            }

            if (Operation.PEEK.equals(op)) {
                nextTime = cs.getNextTime();
            }

            if (Operation.ROLLBACK.equals(op)) {
                prevStepsize = previousState.prevStepsize;
                lastStepsizeLimitedByContinuousConstraint = previousState.lastStepsizeLimitedByContinuousConstraint;
                currentTime = previousState.currentTime;
                nextTime = previousState.nextTime;
                previousState = null;
            }
        }
    }

    @Override
    public Double getMaxStepSize() {
        return stepsizeAdjustmentStrategy.getStepsize(detector.getZerocrossingState(), lastStepsizeLimitedByContinuousConstraint);
    }

    @Override
    public Boolean wasStepValid() {
        detector.updateZeroCrossingState();
        if (ZerocrossingConstraintState.CROSSED.equals(detector.getZerocrossingState())) {
            logZerocrossing();
            if (detector.hasZerocrossingViolatedTolerance(tol)) {
                logToleranceViolation();
                return false;
            }
        }
        return true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDecision() {
        return stepsizeAdjustmentStrategy.getDecision();
    }

    @Override
    public Boolean isRelaxingStrongly() {
        return stepsizeAdjustmentStrategy.isRelaxingStrongly();
    }

    private void logZerocrossing() {
        final Double resolvedDistance = detector.getResolvedDistanceToZerocrossing();
        String msg = "A zerocrossing of constraint \"";
        msg += id;
        msg += "\" occurred in the time interval [ ";
        msg += currentTime;
        msg += " ; ";
        msg += nextTime;
        msg += " ] and was hit with a distance of ";
        msg += resolvedDistance;
        logger.warn(msg);
    }

    private void logToleranceViolation() {
        final Double peekStepsize = nextTime - currentTime;
        final Boolean isStepsizeMinimal = peekStepsize.equals(interval.getMinimalStepsize());
        final Double resolvedDistance = detector.getResolvedDistanceToZerocrossing();
        String msg = "Absolute tolerance violated!\n";
        msg += "\t| A zerocrossing of constraint \"";
        msg += id;
        msg += "\"\n";
        msg += "\t| occurred in the time interval [ ";
        msg += currentTime;
        msg += " ; ";
        msg += nextTime;
        msg += " ]\n";
        msg += "\t| and could only be resolved with a distance of ";
        msg += resolvedDistance;
        msg += "\n\t| which is greather than the absolute tolerance of ";
        msg += tol;
        if (isStepsizeMinimal) {
            msg += "\n\t| The stepsize equals the minimal stepsize of ";
            msg += interval.getMinimalStepsize();
            msg += " !";
            msg += "\n\t| Decrease the minimal stepsize or increase this constraint's tolerance";
        } else {
            msg += "\n\t| Increase the 'safety' parameter of this constraint for more conservatism";
        }
        logger.warn(msg);
    }

}
