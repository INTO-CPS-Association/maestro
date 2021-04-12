package org.intocps.maestro.webapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@Profile("main")
public class ApplicationConfiguration {

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 10 minutes in milliseconds
        container.setMaxSessionIdleTimeout(600000L);
        return container;
    }

}