import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.framework.fmi2.LegacyMMSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.intocps.maestro.framework.fmi2.LegacyMMSupport.fixLeadingNumeralsInInstanceNames;
import static org.intocps.maestro.framework.fmi2.LegacyMMSupport.revertInstanceName;

public class LegacyMMSupportTest {
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

        Map<String, List<String>> variablesToLog = new HashMap<>();
        variablesToLog.put("{crtl}.13ctrl.valve", List.of("{wt}.2wt3.valvecontrol"));
        variablesToLog.put("{wt}.2wt1.level", List.of("{crtl}.1ctrl1.level", "{crtl}.33ctrl.level"));

        Fmi2SimulationEnvironmentConfiguration configuration = new Fmi2SimulationEnvironmentConfiguration();
        configuration.connections = connections;
        configuration.logVariables = logVariables;
        configuration.livestream = livestream;
        configuration.variablesToLog = variablesToLog;

        // Act
        fixLeadingNumeralsInInstanceNames(configuration);

        // Assert
        Assertions.assertTrue(
                mappingContainsNOLeadingNumerals(configuration.connections) && mappingContainsNOLeadingNumerals(configuration.logVariables) &&
                        mappingContainsNOLeadingNumerals(configuration.livestream) && mappingContainsNOLeadingNumerals(configuration.variablesToLog));
    }

    @Test
    public void revertTransformedInstanceNamesTest() {
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

        Map<String, List<String>> variablesToLog = new HashMap<>();
        variablesToLog.put("{crtl}.13ctrl.valve", List.of("{wt}.2wt3.valvecontrol"));
        variablesToLog.put("{wt}.2wt1.level", List.of("{crtl}.1ctrl1.level", "{crtl}.33ctrl.level"));

        Fmi2SimulationEnvironmentConfiguration configuration = new Fmi2SimulationEnvironmentConfiguration();
        configuration.connections = connections;
        configuration.logVariables = logVariables;
        configuration.livestream = livestream;
        configuration.variablesToLog = variablesToLog;

        // Act
        fixLeadingNumeralsInInstanceNames(configuration);

        configuration.connections = configuration.connections.entrySet().stream().collect(Collectors.toMap((e) -> revertInstanceName(e.getKey()),
                (e) -> e.getValue().stream().map(LegacyMMSupport::revertInstanceName).collect(Collectors.toList())));
        configuration.logVariables = configuration.logVariables.entrySet().stream().collect(Collectors.toMap((e) -> revertInstanceName(e.getKey()),
                (e) -> e.getValue().stream().map(LegacyMMSupport::revertInstanceName).collect(Collectors.toList())));

        // Assert
        Assertions.assertTrue(
                mappingContainsLeadingNumerals(configuration.connections) && mappingContainsLeadingNumerals(configuration.logVariables) &&
                        mappingContainsNOLeadingNumerals(configuration.livestream) && mappingContainsNOLeadingNumerals(configuration.variablesToLog));
    }

    private boolean mappingContainsNOLeadingNumerals(Map<String, List<String>> mapping) {
        return mapping.entrySet().stream().allMatch(e -> !Character.isDigit(e.getKey().split("\\.")[1].charAt(0)) &&
                e.getValue().stream().noneMatch(v -> Character.isDigit(v.split("\\.")[1].charAt(0))));
    }

    private boolean mappingContainsLeadingNumerals(Map<String, List<String>> mapping) {
        return mapping.entrySet().stream().allMatch(e -> Character.isDigit(e.getKey().split("\\.")[1].charAt(0)) &&
                e.getValue().stream().allMatch(v -> Character.isDigit(v.split("\\.")[1].charAt(0))));
    }
}
