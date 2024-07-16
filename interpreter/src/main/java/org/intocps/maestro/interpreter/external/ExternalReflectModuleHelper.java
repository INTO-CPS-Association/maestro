package org.intocps.maestro.interpreter.external;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.node.ANameType;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExternalReflectModuleHelper {

    public static Map<String, Value> createMembers(AModuleDeclaration module, Object target,
            Predicate<AFunctionDeclaration> functionFilter) throws NoSuchMethodException {

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null");
        }

        Function<ExternalReflectCallHelper.ArgMappingContext, IArgMapping> costumeArgMapper = tctxt -> {
            PType t = tctxt.getArgType();
            if (t instanceof ANameType) {
                if (((ANameType) t).getName().getText().equals("FMI3Instance")) {
                    return new IArgMapping() {
                        @Override
                        public int getDimension() {
                            return 1;
                        }

                        @Override
                        public long[] getLimits() {
                            return null;
                        }

                        @Override
                        public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
                            return ExternalReflectCallHelper.ArgMapping.InOut.Input;
                        }

                        @Override
                        public void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction) {

                        }

                        @Override
                        public Object map(Value v) {
                            if (v instanceof ExternalModuleValue) {
                                return ((ExternalModuleValue<?>) v).getModule();
                            }
                            return null;
                        }

                        @Override
                        public void mapOut(Value original, Object value) {
                            throw new RuntimeException("This is only for input so should not be called");
                        }

                        @Override
                        public Value mapOut(Object value, Map<IArgMapping, Value> outputArgs) {
                            return new ExternalModuleValue<>(null, value) {};
                        }

                        @Override
                        public Class getType() {
                            return Object.class;
                        }

                        @Override
                        public String getDescriptiveName() {
                            return "FMI3Instance";
                        }

                        @Override
                        public String getDefaultTestValue() {
                            return "null";
                        }
                    };
                }
            }
            return null;
        };
        Map<String, Value> members = new HashMap<>();
        for (AFunctionDeclaration function : module.getFunctions()) {
            if (functionFilter == null || functionFilter.test(function)) {
                members.put(function.getName().getText(), new ExternalReflectCallHelper(function, target, costumeArgMapper).build());
            }
        }
        return members;
    }

    public static ExternalModuleValue createExternalModule(AModuleDeclaration module, Object target,
            Predicate<AFunctionDeclaration> functionFilter) throws NoSuchMethodException {
        return new ExternalModuleValue(createMembers(module, target, functionFilter), target) {
            @Override
            public Object getModule() {
                return target;
            }
        };
    }
}
