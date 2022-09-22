package org.intocps.maestro;

import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.junit.jupiter.api.Assertions;

public class BaseApiTestAssertion extends BaseApiTest {
    protected BaseApiTestAssertion() {
        super(Assertions::assertTrue, Assertions::assertFalse);
    }
}
