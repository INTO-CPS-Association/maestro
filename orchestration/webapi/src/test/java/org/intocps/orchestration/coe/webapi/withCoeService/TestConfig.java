package org.intocps.orchestration.coe.webapi.withCoeService;

import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.intocps.orchestration.coe.webapi.services.SimulatorManagementService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.UUID;

@Profile("test3")
@Configuration
public class TestConfig {

    @Bean
    @Primary
    public Coe coe() {

        String session = UUID.randomUUID().toString();
        File root = new File(session);
        root.mkdirs();
        Coe coe = new Coe(root);

        return coe;
    }

    @Bean
    @Primary
    public CoeService coeService() {
        return new CoeService(coe());
    }

    @Bean
    @Primary
    public SimulatorManagementService simulationManagementService() {
        return Mockito.mock(SimulatorManagementService.class);
    }
}
