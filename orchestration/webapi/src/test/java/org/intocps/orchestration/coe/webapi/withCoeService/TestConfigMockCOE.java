package org.intocps.orchestration.coe.webapi.withCoeService;

import org.intocps.orchestration.coe.config.CoeConfiguration;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.intocps.orchestration.coe.webapi.services.SimulatorManagementService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Profile("test2")
@Configuration
public class TestConfigMockCOE {

    @Bean
    @Primary
    public Coe coe() {
        Coe mock = Mockito.mock(Coe.class);
        String session = UUID.randomUUID().toString();
        File root = new File(session);
        root.mkdirs();
        when(mock.getResultRoot()).thenReturn(root);

        CoeConfiguration coeConfiguration = new CoeConfiguration();
        when(mock.getConfiguration()).thenReturn(coeConfiguration);

        when(mock.initialize(any(), any(), any(), any(), any())).thenReturn(new HashMap<>());
        return mock;
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
