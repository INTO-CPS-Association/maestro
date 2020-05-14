package org.intocps.maestro.webapi;

import org.intocps.maestro.webapi.services.CoeService;
import org.intocps.maestro.webapi.services.SimulatorManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("main")
public class ApplicationConfiguration {

    @Bean
    @Primary
    public CoeService coeService() {
        return new CoeService();
    }


    @Bean
    @Primary
    public SimulatorManagementService simulationManagementService() {
        return new SimulatorManagementService();
    }
}