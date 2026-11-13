import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.testutils.RequiresFmu;
import org.intocps.maestro.testutils.TestFmu;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2FmuValidator;
import org.intocps.maestro.framework.fmi2.IFmuValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@RequiresFmu(TestFmu.WATER_TANK_CONTROLLER)
public class FmuValidatorTest {

    @Test
    public void validateTest() {
        // Arrange
        IFmuValidator fmuValidator = new Fmi2FmuValidator();
        IErrorReporter errorReporter = new ErrorReporter();

        // Assert
        Assertions.assertDoesNotThrow(() -> fmuValidator.validate("1", FmuTestModelRepository.cachePath(TestFmu.WATER_TANK_CONTROLLER).toUri(), errorReporter));
        Assertions.assertEquals(2, errorReporter.getWarnings().size());
    }
}
