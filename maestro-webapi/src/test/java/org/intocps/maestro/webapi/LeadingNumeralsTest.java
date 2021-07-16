package org.intocps.maestro.webapi;

import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.intocps.maestro.webapi.maestro2.Maestro2SimulationController.fixLeadingNumeralsInInstanceNames;

public class LeadingNumeralsTest {
    @Test
    public void fixLeadingNumeralsInInstanceNamesTest() {
        // Arrange
        Map<String, List<String>> connections = new HashMap<>();
        connections.put("{crtl}.1ctrl.valve", List.of("{wt}.2wt.valvecontrol"));
        connections.put("{wt}.2wt.level", List.of("{crtl}.1ctrl.level", "{crtl}.3ctrl.level"));

        Map<String, List<String>> logVariables = new HashMap<>();
        logVariables.put("{crtl}.13ctrl.valve", List.of("{wt}.2wt3.valvecontrol"));
        logVariables.put("{wt}.2wt1.level", List.of("{crtl}.1ctrl1.level", "{crtl}.33ctrl.level"));

        Map<String, List<String>> livestream = new HashMap<>();
        livestream.put("{crtl}.3ctrl.valve", List.of("{wt}.32wt3.valvecontrol"));
        livestream.put("{wt}.2.level", List.of("{crtl}.1ctrl1.level", "{crtl}.33c.level"));

        InitializationData initData =
                new InitializationData(
                        null,
                        connections,
                        null,
                        logVariables,
                        false,
                        false,
                        0.0,
                        0.0,
                        false,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        livestream,
                        false
                );

        // Act
        fixLeadingNumeralsInInstanceNames(initData);

        // Assert
        Assertions.assertTrue(mappingContainsNoLeadingNumerals(initData.getConnections()) &&
                mappingContainsNoLeadingNumerals(initData.getLogVariables()) &&
                mappingContainsNoLeadingNumerals(initData.getLivestream()));
    }

    private boolean mappingContainsNoLeadingNumerals(Map<String, List<String>> mapping) {
        return mapping.entrySet().stream().allMatch(e -> !Character.isDigit(e.getKey().split("\\.")[1].charAt(0)) &&
                e.getValue().stream().noneMatch(v -> Character.isDigit(v.split("\\.")[1].charAt(0))));
    }
}
