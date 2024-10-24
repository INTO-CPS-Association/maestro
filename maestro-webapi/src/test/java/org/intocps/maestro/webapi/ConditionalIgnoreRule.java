package org.intocps.maestro.webapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
/**
 * If this logic is to be used it needs to be reworked to work with Junit 5 as it does not have rules.
 */
//public class ConditionalIgnoreRule implements MethodRule {
//
//    private static boolean hasConditionalIgnoreAnnotation(FrameworkMethod method) {
//        return method.getAnnotation(ConditionalIgnore.class) != null;
//    }
//
//    private static IgnoreCondition getIgnoreContition(Object target, FrameworkMethod method) {
//        ConditionalIgnore annotation = method.getAnnotation(ConditionalIgnore.class);
//        return new IgnoreConditionCreator(target, annotation).create();
//    }
//
//    @Override
//    public Statement apply(Statement base, FrameworkMethod method, Object target) {
//        Statement result = base;
//        if (hasConditionalIgnoreAnnotation(method)) {
//            IgnoreCondition condition = getIgnoreContition(target, method);
//            if (condition.isSatisfied()) {
//                result = new IgnoreStatement(condition);
//            }
//        }
//        return result;
//    }
//
//    public interface IgnoreCondition {
//        boolean isSatisfied();
//    }
//
//    @Retention(RetentionPolicy.RUNTIME)
//    @Target({ElementType.METHOD})
//    public @interface ConditionalIgnore {
//        Class<? extends IgnoreCondition> condition();
//    }
//
//    private static class IgnoreConditionCreator {
//        private final Object target;
//        private final Class<? extends IgnoreCondition> conditionType;
//
//        IgnoreConditionCreator(Object target, ConditionalIgnore annotation) {
//            this.target = target;
//            this.conditionType = annotation.condition();
//        }
//
//        IgnoreCondition create() {
//            checkConditionType();
//            try {
//                return createCondition();
//            } catch (RuntimeException re) {
//                throw re;
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        private IgnoreCondition createCondition() throws Exception {
//            IgnoreCondition result;
//            if (isConditionTypeStandalone()) {
//                result = conditionType.getDeclaredConstructor().newInstance();
//            } else {
//                result = conditionType.getDeclaredConstructor(target.getClass()).newInstance(target);
//            }
//            return result;
//        }
//
//        private void checkConditionType() {
//            if (!isConditionTypeStandalone() && !isConditionTypeDeclaredInTarget()) {
//                String msg = "Conditional class '%s' is a member class " + "but was not declared inside the test case using it.\n" + "Either make this class a static class, " + "standalone class (by declaring it in it's own file) " + "or move it inside the test case using it";
//                throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
//            }
//        }
//
//        private boolean isConditionTypeStandalone() {
//            return !conditionType.isMemberClass() || Modifier.isStatic(conditionType.getModifiers());
//        }
//
//        private boolean isConditionTypeDeclaredInTarget() {
//            return target.getClass().isAssignableFrom(conditionType.getDeclaringClass());
//        }
//    }
//
//    private static class IgnoreStatement extends Statement {
//        private final IgnoreCondition condition;
//
//        IgnoreStatement(IgnoreCondition condition) {
//            this.condition = condition;
//        }
//
//        @Override
//        public void evaluate() {
//            Assume.assumeTrue("Ignored by " + condition.getClass().getSimpleName(), false);
//        }
//    }
//
//}
