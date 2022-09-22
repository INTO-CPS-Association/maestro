package org.intocps.maestro.framework.fmi2.api.mabl;

import org.junit.jupiter.api.Assertions;

import java.util.function.BiConsumer;

public class BaseApiAssertTest extends BaseApiTest{
    protected BaseApiAssertTest() {
        super(Assertions::assertTrue, Assertions::assertFalse);
    }
}
