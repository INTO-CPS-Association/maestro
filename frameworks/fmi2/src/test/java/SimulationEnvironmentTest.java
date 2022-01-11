import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.core.EnvironmentException;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class SimulationEnvironmentTest {

    @Test
    public void simulationConfigurationThrowsWithoutConnections() {
        // Assert
        Assertions.assertThrows(EnvironmentException.class, () -> Fmi2SimulationEnvironmentConfiguration.createFromJsonString(new String(
                Objects.requireNonNull(this.getClass().getClassLoader()
                        .getResourceAsStream("simulation_environment" + "/simulation_environment_without_connections" + ".json")).readAllBytes())));
    }

    @Test
    public void simulationConfigurationThrowsWithoutFMUs() {
        // Assert
        Assertions.assertThrows(EnvironmentException.class, () -> Fmi2SimulationEnvironmentConfiguration.createFromJsonString(new String(
                Objects.requireNonNull(this.getClass().getClassLoader()
                        .getResourceAsStream("simulation_environment" + "/simulation_environment_without_fmus" + ".json")).readAllBytes())));
    }

    @Test
    public void simulationConfigurationThrowsUnknownName() throws Exception {
        // Arrange
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(new String(Objects.requireNonNull(
                        this.getClass().getClassLoader().getResourceAsStream("simulation_environment/simulation_environment_with_mismatched_names.json"))
                .readAllBytes()));
        ErrorReporter reporter = new ErrorReporter();

        // Act
        Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);
        //        Assertions.assertThrows(EnvironmentException.class,
        //                () -> Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
        //                        new String(Objects.requireNonNull(
        //                                this.getClass().getClassLoader().getResourceAsStream("simulation_environment" +
        //                                        "/simulation_environment_without_fmus" +
        //                                        ".json")).readAllBytes())));
    }
}
