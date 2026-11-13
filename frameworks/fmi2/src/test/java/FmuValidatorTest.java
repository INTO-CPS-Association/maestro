import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2FmuValidator;
import org.intocps.maestro.framework.fmi2.IFmuValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class FmuValidatorTest {
    private static final String FMU_NAME = "watertankcontroller-c.fmu";

    @BeforeAll
    static void downloadFmu() throws IOException {
        FmuTestModelRepository.download(List.of(FmuTestModelRepository.url(FMU_NAME)));
    }

    @Test
    public void validateTest() {
        // Arrange
        IFmuValidator fmuValidator = new Fmi2FmuValidator();
        IErrorReporter errorReporter = new ErrorReporter();

        // Assert
        Assertions.assertDoesNotThrow(() -> fmuValidator.validate("1", FmuTestModelRepository.cachePath(FMU_NAME).toUri(), errorReporter));
        Assertions.assertEquals(2, errorReporter.getWarnings().size());
    }
}
