package org.intocps.orchestration.coe.webapi;

import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.intocps.orchestration.coe.webapi.services.SimulatorManagementService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("mocked_coe_service")
@Configuration
public class MockedCoeServiceTestConfiguration {

    @Bean
    @Primary
    public CoeService coeService() {
        return Mockito.mock(CoeService.class);
    }

    @Bean
    @Primary
    public SimulatorManagementService simulationManagementService() {
        return Mockito.mock(SimulatorManagementService.class);
    }
}
