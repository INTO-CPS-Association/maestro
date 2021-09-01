import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2FmuValidator;
import org.intocps.maestro.framework.fmi2.IFmuValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Objects;

public class FmuValidatorTest {

    @Test
    public void validateTest() {
        // Arrange
        IFmuValidator fmuValidator = new Fmi2FmuValidator();
        URL fmuUrl = FmuValidatorTest.class.getClassLoader().getResource("watertankcontroller-c.fmu");
        IErrorReporter errorReporter = new ErrorReporter();

        // Assert
        Assertions.assertDoesNotThrow(() -> fmuValidator.validate("1", Objects.requireNonNull(fmuUrl).toURI(), errorReporter));
        Assertions.assertEquals(2, errorReporter.getWarnings().size());
    }
}
