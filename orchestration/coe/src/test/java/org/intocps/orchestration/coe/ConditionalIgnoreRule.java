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
package org.intocps.orchestration.coe;

import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

public class ConditionalIgnoreRule implements MethodRule
{

	public interface IgnoreCondition
	{
		boolean isSatisfied();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface ConditionalIgnore
	{
		Class<? extends IgnoreCondition> condition();
	}

	@Override
	public Statement apply(Statement base, FrameworkMethod method, Object target)
	{
		Statement result = base;
		if (hasConditionalIgnoreAnnotation(method))
		{
			IgnoreCondition condition = getIgnoreContition(target, method);
			if (condition.isSatisfied())
			{
				result = new IgnoreStatement(condition);
			}
		}
		return result;
	}

	private static boolean hasConditionalIgnoreAnnotation(FrameworkMethod method)
	{
		return method.getAnnotation(ConditionalIgnore.class) != null;
	}

	private static IgnoreCondition getIgnoreContition(Object target,
			FrameworkMethod method)
	{
		ConditionalIgnore annotation = method.getAnnotation(ConditionalIgnore.class);
		return new IgnoreConditionCreator(target, annotation).create();
	}

	private static class IgnoreConditionCreator
	{
		private final Object target;
		private final Class<? extends IgnoreCondition> conditionType;

		IgnoreConditionCreator(Object target, ConditionalIgnore annotation)
		{
			this.target = target;
			this.conditionType = annotation.condition();
		}

		IgnoreCondition create()
		{
			checkConditionType();
			try
			{
				return createCondition();
			} catch (RuntimeException re)
			{
				throw re;
			} catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		private IgnoreCondition createCondition() throws Exception
		{
			IgnoreCondition result;
			if (isConditionTypeStandalone())
			{
				result = conditionType.newInstance();
			} else
			{
				result = conditionType.getDeclaredConstructor(target.getClass()).newInstance(target);
			}
			return result;
		}

		private void checkConditionType()
		{
			if (!isConditionTypeStandalone()
					&& !isConditionTypeDeclaredInTarget())
			{
				String msg = "Conditional class '%s' is a member class "
						+ "but was not declared inside the test case using it.\n"
						+ "Either make this class a static class, "
						+ "standalone class (by declaring it in it's own file) "
						+ "or move it inside the test case using it";
				throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
			}
		}

		private boolean isConditionTypeStandalone()
		{
			return !conditionType.isMemberClass()
					|| Modifier.isStatic(conditionType.getModifiers());
		}

		private boolean isConditionTypeDeclaredInTarget()
		{
			return target.getClass().isAssignableFrom(conditionType.getDeclaringClass());
		}
	}

	private static class IgnoreStatement extends Statement
	{
		private final IgnoreCondition condition;

		IgnoreStatement(IgnoreCondition condition)
		{
			this.condition = condition;
		}

		@Override
		public void evaluate()
		{
			Assume.assumeTrue("Ignored by "
					+ condition.getClass().getSimpleName(), false);
		}
	}

}
