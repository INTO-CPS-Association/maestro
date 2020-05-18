package org.intocps.maestro.webapi.maestro2;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        WebSocketHandlerRegistration registration =
                webSocketHandlerRegistry.addHandler(new SocketHandler(), "/attachSession/*").addInterceptors(new HttpSessionHandshakeInterceptor())
                        .setAllowedOrigins("*");
    }

}
