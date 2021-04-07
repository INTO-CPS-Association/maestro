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
package org.intocps.maestro.interpreter.values.variablestep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintType;
import org.intocps.maestro.interpreter.values.variablestep.constraint.samplingrate.Sampling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@JsonInclude(Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class InitializationMsgJson
{
	public static class Constraint
	{
		final static Logger logger = LoggerFactory.getLogger(Constraint.class);
		private static final Double DEFAULT_ABSOLUTE_TOLERANCE = 1E-3;
		private static final Double DEFAULT_RELATIVE_TOLERANCE = 1E-2;
		private static final Double DEFAULT_SAFETY = 0.0;
		private static final Integer DEFAULT_ORDER = 2;
		private static final Boolean DEFAULT_SKIPDISCRETE = true;

		public String type;
		public Integer order;
		public List<String> ports;
		public Double abstol;
		public Double reltol;
		public Double safety;
		public Integer base;
		public Integer rate;
		public Integer startTime;
		public Boolean skipDiscrete;
		private String id;

		public Double getAbsoluteTolerance()
		{
			return abstol == null ? DEFAULT_ABSOLUTE_TOLERANCE : abstol;
		}

		public Double getRelativeTolerance()
		{
			return reltol == null ? DEFAULT_RELATIVE_TOLERANCE : reltol;
		}

		public void setId(final String id)
		{
			this.id = id;
		}

		public String getId()
		{
			if (id == null)
			{
				throw new IllegalStateException("Each constraint must have an id");
			}
			return id;
		}

		public Boolean getSkipDiscrete()
		{
			return skipDiscrete == null ? DEFAULT_SKIPDISCRETE : skipDiscrete;
		}

		public Double getSafety()
		{
			return safety == null ? DEFAULT_SAFETY : safety;
		}

		public ConstraintType getType()
		{
			return ConstraintType.lookup(type);
		}

		public Integer getOrder()
		{
			return order == null ? DEFAULT_ORDER : order;
		}

		public List<ModelConnection.Variable> getPorts()
		{
			final List<ModelConnection.Variable> v = new Vector<ModelConnection.Variable>();
			if (ports == null || ports.isEmpty())
			{
				return v;
			}
			for (String vs : ports)
			{
				try
				{
					v.add(ModelConnection.Variable.parse(vs));
				} catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return v;
		}

		public Sampling getSampling()
		{
			return new Sampling(base, rate, startTime);
		}

		public static Constraint parse(Map<String, Object> value)
		{
			Constraint constraint = new Constraint();
			constraint.type = value.get("type").toString();

			for (Field f : constraint.getClass().getFields())
			{

				if (value.containsKey(f.getName()))
				{
					Object val = value.get(f.getName());

					//handle basic types
					if(f.getType()== Double.class && val.getClass()==Integer.class)
					{
						val = (double) (int) val;
					}

					if (f.getType().isAssignableFrom(val.getClass()))
					{
						try
						{
							f.set(constraint, val);
						} catch (Exception e)
						{
							logger.error("Failed parsing constraint", e);
						}
					} else
					{
						logger.error("Constraint field type mismatch unable to set. Expected {}, Actual {}", f.getType(), val.getClass());
					}
				} else
				{
					// field not found
				}

			}

			return constraint;
		}

	}

	public Map<String, String> fmus;
	public Map<String, List<String>> connections;
	public Map<String, Object> parameters;
	public Map<String, Object> algorithm;
	public Map<String, List<String>> livestream;
	public Map<String, List<String>> logVariables;

	public boolean visible;
	public boolean loggingOn;
	public String overrideLogLevel = null;
	public boolean parallelSimulation = false;
	public boolean stabalizationEnabled = false;
	public double global_absolute_tolerance = 0.0;
	public double global_relative_tolerance = 0.01;
	public boolean simulationProgramDelay = false;
	public boolean hasExternalSignals = false;

	@JsonIgnore public Map<String, URI> getFmuFiles() throws Exception
	{
		Map<String, URI> files = new HashMap<>();

		if (fmus != null)
		{
			for (Map.Entry<String, String> entry : fmus.entrySet())
			{
				try
				{
					files.put(entry.getKey(), new URI(entry.getValue()));
				} catch (Exception e)
				{
					throw new Exception(
							entry.getKey() + "-" + entry.getValue() + ": "
									+ e.getMessage(), e);
				}
			}
		}

		return files;
	}

}
