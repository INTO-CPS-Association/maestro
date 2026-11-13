package org.intocps.maestro.testutils;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.EnumSet;

final class RequiredFmuExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        FmuTestModelRepository.download(requiredFmus(context.getRequiredTestClass()).toArray(TestFmu[]::new));
    }

    static EnumSet<TestFmu> requiredFmus(Class<?> testClass) {
        EnumSet<TestFmu> models = EnumSet.noneOf(TestFmu.class);
        for (Class<?> current = testClass; current != null; current = current.getSuperclass()) {
            for (RequiresFmu requirement : current.getDeclaredAnnotationsByType(RequiresFmu.class)) {
                models.addAll(Arrays.asList(requirement.value()));
            }
        }
        return models;
    }
}