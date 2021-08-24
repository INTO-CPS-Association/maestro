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
package org.intocps.maestro.modeldefinitionchecker;

import org.intocps.fmichecker.Orch;
import org.intocps.fmichecker.Orch.SV_X_;
import org.intocps.fmichecker.quotes.*;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.overture.codegen.runtime.Tuple;

import java.util.ArrayList;
import java.util.List;

public class VdmSvChecker {
    public static List<Fmi2ModelDescription.ScalarVariable> validateModelVariables(
            List<Fmi2ModelDescription.ScalarVariable> modelVariables) throws ScalarVariableConfigException {
        List<Fmi2ModelDescription.ScalarVariable> SV_list = new ArrayList<>();

        StringBuffer sb = new StringBuffer();
        for (Fmi2ModelDescription.ScalarVariable scalarVariable : modelVariables) {
            Orch.SV sv = new Orch.SV(convertCausality(scalarVariable.causality), convertVariability(scalarVariable.variability),
                    convertInitial(scalarVariable.initial), convertType(scalarVariable.type));

            SV_X_ sv_x = Orch.InitSV(sv);

            Tuple res = Orch.Validate(sv_x);

            scalarVariable.causality = convertCausalitySV(sv_x.causality);
            scalarVariable.variability = convertVariabilitySV(sv_x.variability);
            scalarVariable.initial = convertInitialSV(sv_x.initial);

            if (!(Boolean) res.get(0)) {

                String message = "" + res.get(1);
                sb.append("Error in configuration of scalar variable '" + scalarVariable.name + "': " + message + "\n");
            }
            SV_list.add(scalarVariable);
        }

        if (sb.length() > 0) {
            throw new ScalarVariableConfigException(sb.toString());
        }

        return SV_list;
    }

    private static Orch.Type convertType(Fmi2ModelDescription.Type type) {
        Object t = null;
        switch (type.type) {
            case Boolean:
                t = new BooleanQuote();
                break;
            case Integer:
                t = new IntegerQuote();
                break;
            case Real:
                t = new RealQuote();
                break;
            case String:
                t = new StringQuote();
                break;
            case Enumeration:
                t = new EnumerationQuote();
                break;
            default:
                break;
        }
        return new Orch.Type(t, (type.start != null ? type.start : null));
    }

    private static Object convertInitial(Fmi2ModelDescription.Initial initial) {
        if (initial == null) {
            return null;
        }
        switch (initial) {
            case Approx:
                return new approxQuote();
            case Calculated:
                return new calculatedQuote();
            case Exact:
                return new exactQuote();
            default:
                break;
        }
        return null;
    }

    private static Fmi2ModelDescription.Initial convertInitialSV(Object initial) {

		if (initial instanceof approxQuote) {
			return Fmi2ModelDescription.Initial.Approx;
		} else if (initial instanceof calculatedQuote) {
			return Fmi2ModelDescription.Initial.Calculated;
		} else if (initial instanceof exactQuote) {
			return Fmi2ModelDescription.Initial.Exact;
		} else {
			return null;
		}
    }

    private static Object convertVariability(Fmi2ModelDescription.Variability variability) {
        if (variability == null) {
            return null;
        }
        switch (variability) {
            case Constant:
                return new constantQuote();
            case Continuous:
                return new continuousQuote();
            case Discrete:
                return new discreteQuote();
            case Fixed:
                return new fixedQuote();
            case Tunable:
                return new tunableQuote();
            default:
                return null;

        }
    }

    private static Fmi2ModelDescription.Variability convertVariabilitySV(Object variability) {

		if (variability instanceof constantQuote) {
			return Fmi2ModelDescription.Variability.Constant;
		} else if (variability instanceof continuousQuote) {
			return Fmi2ModelDescription.Variability.Continuous;
		} else if (variability instanceof discreteQuote) {
			return Fmi2ModelDescription.Variability.Discrete;
		} else if (variability instanceof fixedQuote) {
			return Fmi2ModelDescription.Variability.Fixed;
		} else if (variability instanceof tunableQuote) {
			return Fmi2ModelDescription.Variability.Tunable;
		} else {
			return null;
		}
    }

    private static Object convertCausality(Fmi2ModelDescription.Causality causality) {
        if (causality == null) {
            return null;
        }

        switch (causality) {
            case CalculatedParameter:
                return new calculatedParameterQuote();
            case Independent:
                return new independentQuote();
            case Input:
                return new inputQuote();
            case Local:
                return new localQuote();
            case Output:
                return new outputQuote();
            case Parameter:
                return new parameterQuote();
            default:
                return null;

        }
    }

    private static Fmi2ModelDescription.Causality convertCausalitySV(Object causality) {

		if (causality instanceof calculatedParameterQuote) {
			return Fmi2ModelDescription.Causality.CalculatedParameter;
		} else if (causality instanceof independentQuote) {
			return Fmi2ModelDescription.Causality.Independent;
		} else if (causality instanceof inputQuote) {
			return Fmi2ModelDescription.Causality.Input;
		} else if (causality instanceof localQuote) {
			return Fmi2ModelDescription.Causality.Local;
		} else if (causality instanceof outputQuote) {
			return Fmi2ModelDescription.Causality.Output;
		} else if (causality instanceof parameterQuote) {
			return Fmi2ModelDescription.Causality.Parameter;
		} else {
			return null;
		}
    }

    public static class ScalarVariableConfigException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public ScalarVariableConfigException(String message) {
            super(message);
        }

    }
}
