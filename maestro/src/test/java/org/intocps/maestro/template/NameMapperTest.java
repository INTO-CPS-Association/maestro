package org.intocps.maestro.template;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

class NameMapperTest {

    @Test
    void removeInvalidCharacters() {
        Assert.assertEquals("a1", NameMapper.handleInvalidCharacters("a%-,.1"));
        Assert.assertEquals("fmu1", NameMapper.handleInvalidCharacters("%-,.1"));
    }
}