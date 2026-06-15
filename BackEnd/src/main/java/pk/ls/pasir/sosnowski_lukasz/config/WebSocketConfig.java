package pk.ls.pasir.sosnowski_lukasz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pk.ls.pasir.sosnowski_lukasz.websocket.GroupNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupNotificationWebSocketHandler groupNotificationWebSocketHandler;

    public WebSocketConfig(GroupNotificationWebSocketHandler groupNotificationWebSocketHandler) {
        this.groupNotificationWebSocketHandler = groupNotificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupNotificationWebSocketHandler, "/ws/group-notifications")
                .setAllowedOriginPatterns("*");
    }
}