package org.intocps.maestro.webapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.intocps.maestro.webapi.maestro2.interpreter.WebsocketValueConverter;
import org.junit.Test;

import java.util.Arrays;

public class WebsocketValueConverterTest {
    @Test
    public void test() throws JsonProcessingException {
        WebsocketValueConverter wsvc = new WebsocketValueConverter(null);

        wsvc.configure(Arrays.asList("a.b.c", "a.b.e"));

        wsvc.update(0d, Arrays.asList(1, 2));
        System.out.println(wsvc.getJson());
    }
}
