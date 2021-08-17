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
package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.fmi.FmiSimulationInstance;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintHandler;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintHandlerFactory;
import org.intocps.maestro.interpreter.values.variablestep.constraint.FmuMaxStepSizeHandler;
import org.intocps.maestro.interpreter.values.variablestep.constraint.samplingrate.SamplingRateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.intocps.maestro.fmi.Fmi2ModelDescription.*;

import java.util.*;

public class StepsizeCalculator {

    final static Logger logger = LoggerFactory.getLogger(StepsizeCalculator.class);
    private final static Double STRONG_RELAXATION_FACTOR = 3.0;
    private final static Double INVALID_STEP_TIGHTENING_FACTOR = 0.25;
    private final static String ROLLBACK_MSG = "Discarding previous step and rolling back internal states";

    private final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
    private final List<ConstraintHandler> handler = new Vector<>();
    private final Double initialStepsize;
    private final StepsizeInterval stepsizeInterval;
    private Double stepsize = null;
    private Double endTime = null;
    private Boolean wasStepsizeLimitedByDiscreteConstraint = false;
    private FmuMaxStepSizeHandler fmuMaxStepSizeHandler = null;

    public StepsizeCalculator(final Set<InitializationMsgJson.Constraint> constraints, final StepsizeInterval stepsizeInterval,
            final Double initialStepsize, final Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances) throws InterpreterException {
        this.initialStepsize = stepsizeInterval.saturateStepsize(initialStepsize);
        this.stepsizeInterval = stepsizeInterval;
        for (InitializationMsgJson.Constraint constraint : constraints) {
            try {
                ConstraintHandler h = ConstraintHandlerFactory
                        .getHandler(currentSolutionPoint, constraint, stepsizeInterval, STRONG_RELAXATION_FACTOR, getTypeMap(instances), logger);
                if (h == null) {
                    logger.warn("Unable to instantiate constraint: {}", constraint.getId());
                }

                if (h instanceof FmuMaxStepSizeHandler) {
                    fmuMaxStepSizeHandler = (FmuMaxStepSizeHandler) h;
                } else {
                    handler.add(h);
                }
            } catch (Exception e) {
                throw new InterpreterException(
                        "The simulation has been aborted because the constraint '" + constraint.getId() + "' could not be instantiated. Details: " +
                                e.getMessage(), e);
            }
        }
    }

    public Double getStepsize(final Double currentTime, final Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Object>> currentValues,
            final Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Map<Integer, Double>>> currentDerivatives, final Double maxFmuStepsize) {
        currentSolutionPoint.advance(currentTime, currentValues, currentDerivatives, stepsize, wasStepsizeLimitedByDiscreteConstraint);
        wasStepsizeLimitedByDiscreteConstraint = false;
        final Double stepsizeToEnd = endTime - currentTime;
        final Double maxStepsize = fmuMaxStepSizeHandler == null && maxFmuStepsize == null ? Double.MAX_VALUE : maxFmuStepsize;

        if (isInitialStep()) {
            stepsize = initialStepsize;
            final Double thisStepsize = Math.min(stepsize, stepsizeToEnd);
            if (maxStepsize < thisStepsize) {
                logFmuRequiredConstraint(currentTime, maxStepsize);
                return maxStepsize;
            }
            return thisStepsize;
        }

        Map<ConstraintHandler, Double> stepsizes = collectStepsizes();

        if (stepsizes.isEmpty()) {
            stepsize = stepsizeInterval.getMaximalStepsize();
            final Double thisStepsize = Math.min(stepsize, stepsizeToEnd);
            if (maxStepsize < thisStepsize) {
                logFmuRequiredConstraint(currentTime, maxStepsize);
                wasStepsizeLimitedByDiscreteConstraint = true;
                return maxStepsize;
            }
            return thisStepsize;
        }

        stepsize = Math.min(Collections.min(stepsizes.values()), stepsizeInterval.getMaximalStepsize());

        if (maxStepsize < Math.min(stepsize, stepsizeToEnd)) {
            logFmuRequiredConstraint(currentTime, maxStepsize);
            wasStepsizeLimitedByDiscreteConstraint = true;
            return maxStepsize;
        }

        if (stepsize > stepsizeToEnd) {
            return stepsizeToEnd;
        }

        if (stepsize < stepsizeInterval.getMaximalStepsize()) {
            final Set<ConstraintHandler> limitingConstraints = findLimitingConstraints(stepsizes);
            produceLogOutput(currentTime, limitingConstraints);
            wasStepsizeLimitedByDiscreteConstraint = containsOnlyDiscreteConstraints(limitingConstraints);
        }

        return stepsize;
    }

    public StepValidationResult validateStep(final Double nextTime, final Map<ModelConnection.ModelInstance, Map<ScalarVariable, Object>> nextValues,
            final Boolean supportsRollback) {

        currentSolutionPoint.peek(nextTime, nextValues);

        final Boolean valid = wasStepValid();
        Double reducedStepsize = stepsize;
        Boolean hasReducedStepsize = false;

        if (!valid && supportsRollback) {
            reducedStepsize = Math.max(stepsize * INVALID_STEP_TIGHTENING_FACTOR, stepsizeInterval.getMinimalStepsize());
            hasReducedStepsize = reducedStepsize < stepsize;
            stepsize = reducedStepsize;
            if (hasReducedStepsize) {
                logger.info(ROLLBACK_MSG);
                //	currentSolutionPoint.rollback();
            }
        }

        return new StepValidationResult(valid, hasReducedStepsize, stepsize);
    }

    public void setEndTime(final Double endTime) {
        this.endTime = endTime;
    }

    private Map<ModelConnection.Variable, Types> getTypeMap(final Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances) {
        Map<ModelConnection.Variable, Types> map = new HashMap<>();
        for (ModelConnection.ModelInstance key : instances.keySet()) {
            for (ScalarVariable sv : instances.get(key).config.scalarVariables) {
                map.put(new ModelConnection.Variable(key, sv.getName()), sv.getType().type);
            }
        }
        return map;
    }

    private Boolean wasStepValid() {
        Boolean valid = true;
        for (ConstraintHandler h : handler) {
            if (!h.wasStepValid()) {
                valid = false;
            }
        }
        return valid;
    }

    private void produceLogOutput(final Double currentTime, final Set<ConstraintHandler> limitingConstraints) {
        if (limitingConstraints.size() > 1 && areAllConstraintsRelaxingStrongly(limitingConstraints)) {
            logAllConstraintsRelaxStrongly(currentTime);
        } else {
            logLimitingConstraintsDecisions(currentTime, limitingConstraints);
        }
    }

    private Boolean areAllConstraintsRelaxingStrongly(final Set<ConstraintHandler> limitingConstraints) {
        for (ConstraintHandler h : limitingConstraints) {
            if (!h.isRelaxingStrongly()) {
                return false;
            }
        }
        return true;
    }

    private Boolean containsOnlyDiscreteConstraints(Set<ConstraintHandler> handlers) {
        for (ConstraintHandler h : handlers) {
            if (!(h instanceof SamplingRateHandler)) {
                return false;
            }
        }
        return true;
    }

    private Set<ConstraintHandler> findLimitingConstraints(Map<ConstraintHandler, Double> maxStepsizes) {
        Set<ConstraintHandler> limitingConstraints = new HashSet<>();
        for (ConstraintHandler h : maxStepsizes.keySet()) {
            if (maxStepsizes.get(h).equals(stepsize)) {
                limitingConstraints.add(h);
            }
        }
        return limitingConstraints;
    }

    private void logLimitingConstraintsDecisions(final Double currentTime, Set<ConstraintHandler> limitingConstraints) {
        String msg = "Time " + currentTime + ", stepsize " + stepsize;
        for (ConstraintHandler h : limitingConstraints) {
            msg += ", limited by constraint \"" + h.getId() + "\" with decision to " + h.getDecision();
        }
        logger.debug(msg);
    }

    private void logAllConstraintsRelaxStrongly(final Double currentTime) {
        String msg = "Time " + currentTime + ", stepsize " + stepsize;
        msg += ", all continuous constraint handlers allow strong relaxation";
        logger.debug(msg);
    }

    private void logFmuRequiredConstraint(final Double currentTime, final Double maxStepsize) {
        String msg = "Time " + currentTime + ", stepsize " + maxStepsize;
        msg += ", limited by an FMU";
        logger.debug(msg);
    }

    private Boolean isInitialStep() {
        return stepsize == null;
    }

    private Map<ConstraintHandler, Double> collectStepsizes() {
        final Map<ConstraintHandler, Double> stepsizes = new HashMap<>();
        for (ConstraintHandler h : handler) {
            stepsizes.put(h, h.getMaxStepSize());
        }
        return stepsizes;
    }

}
