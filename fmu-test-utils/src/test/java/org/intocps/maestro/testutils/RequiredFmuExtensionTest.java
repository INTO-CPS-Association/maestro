package org.intocps.maestro.testutils;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredFmuExtensionTest {
    @RequiresFmu(TestFmu.SINGLE_WATER_TANK)
    @RequiresFmu(TestFmu.WATER_TANK_CONTROLLER)
    static class ParentFixture {
    }

    @RequiresFmu({TestFmu.ROLLBACK_END, TestFmu.SINGLE_WATER_TANK})
    static class ChildFixture extends ParentFixture {
    }

    @Test
    void includesInheritedAndRepeatedRequirementsWithoutDuplicates() {
        assertEquals(EnumSet.of(TestFmu.SINGLE_WATER_TANK, TestFmu.WATER_TANK_CONTROLLER, TestFmu.ROLLBACK_END),
                RequiredFmuExtension.requiredFmus(ChildFixture.class));
        assertTrue(RequiredFmuExtension.requiredFmus(RequiredFmuExtensionTest.class).isEmpty());
    }

    @Test
    void catalogHasUniqueFileNamesAndAssets() {
        HashSet<String> fileNames = new HashSet<>();
        HashSet<String> assetNames = new HashSet<>();
        for (TestFmu model : TestFmu.values()) {
            assertTrue(fileNames.add(model.fileName()));
            assertTrue(assetNames.add(model.assetName()));
            assertEquals(model, TestFmu.fromFileName(model.fileName()).orElseThrow());
        }
    }
}