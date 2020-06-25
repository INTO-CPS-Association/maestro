package org.intocps.maestro.webapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.intocps.maestro.webapi.maestro2.interpreter.WebsocketValueConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
public class WebsocketValueConverterTest {
    @Test
    public void test() throws JsonProcessingException {
        WebsocketValueConverter wsvc = new WebsocketValueConverter(null);

        wsvc.configure(Arrays.asList("a.b.c", "a.b.e"));

        wsvc.update(0d, Arrays.asList(1, 2));
        System.out.println(wsvc.getJson());
    }


}
