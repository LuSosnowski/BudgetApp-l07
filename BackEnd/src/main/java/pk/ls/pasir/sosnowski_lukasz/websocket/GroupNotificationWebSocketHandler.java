package pk.ls.pasir.sosnowski_lukasz.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupExpenseNotification;
import pk.ls.pasir.sosnowski_lukasz.security.JwtUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupNotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;

    private final Map<String, Set<WebSocketSession>> sessionsByEmail = new ConcurrentHashMap<>();

    public GroupNotificationWebSocketHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());

        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(new CloseStatus(1008, "Invalid JWT token"));
            return;
        }

        String email = jwtUtil.extractUsername(token);

        sessionsByEmail
                .computeIfAbsent(email, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionsByEmail.values().forEach(sessions -> sessions.remove(session));
    }

    public void sendToUser(String email, GroupExpenseNotification notification) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String jsonMessage = notification.toJson();

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }

            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                sessions.remove(session);
            }
        }
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        String[] params = uri.getQuery().split("&");

        for (String param : params) {
            String[] keyValue = param.split("=", 2);

            if (keyValue.length == 2 && keyValue[0].equals("token")) {
                return keyValue[1];
            }
        }

        return null;
    }
}