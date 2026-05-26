package com.SM.ChatService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, java.util.Map<String, Object> attributes) throws Exception {
                        String userId = request.getHeaders().getFirst("X-Authenticated-User-Id");
                        log.info("WS HANDSHAKE: Path={}, userId Header={}", request.getURI().getPath(), userId);
                        
                        if (userId == null) {
                            String query = request.getURI().getQuery();
                            if (query != null && query.contains("token=")) {
                                String token = java.util.Arrays.stream(query.split("&"))
                                        .filter(s -> s.startsWith("token="))
                                        .map(s -> s.substring(6))
                                        .findFirst().orElse(null);
                                
                                if (token != null && token.contains(".")) {
                                    try {
                                        String[] parts = token.split("\\.");
                                        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"userId\"\\s*:\\s*\"?(\\d+)\"?");
                                        java.util.regex.Matcher m = p.matcher(payload);
                                        if (m.find()) { userId = m.group(1); }
                                    } catch (Exception e) {}
                                }
                            }
                        }
                        if (userId != null) { attributes.put("userId", userId); }
                        return true;
                    }
                    @Override
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {}
                });

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, java.util.Map<String, Object> attributes) throws Exception {
                        String userId = request.getHeaders().getFirst("X-Authenticated-User-Id");
                        log.info("SOCKJS HANDSHAKE: Path={}, userId Header={}", request.getURI().getPath(), userId);
                        
                        if (userId == null) {
                            String query = request.getURI().getQuery();
                            if (query != null && query.contains("token=")) {
                                String token = java.util.Arrays.stream(query.split("&")).filter(s -> s.startsWith("token=")).map(s -> s.substring(6)).findFirst().orElse(null);
                                if (token != null && token.contains(".")) {
                                    try {
                                        String[] parts = token.split("\\.");
                                        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"userId\"\\s*:\\s*\"?(\\d+)\"?");
                                        java.util.regex.Matcher m = p.matcher(payload);
                                        if (m.find()) { userId = m.group(1); }
                                    } catch (Exception e) {}
                                }
                            }
                        }
                        if (userId != null) { attributes.put("userId", userId); }
                        return true;
                    }
                    @Override
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {}
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    StompCommand cmd = accessor.getCommand();
                    if (StompCommand.CONNECT.equals(cmd) || StompCommand.SEND.equals(cmd) || StompCommand.SUBSCRIBE.equals(cmd) || StompCommand.DISCONNECT.equals(cmd)) {
                        Object userIdObj = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("userId") : null;
                        String userId = userIdObj != null ? userIdObj.toString() : null;
                        if (userId != null) {
                            accessor.setUser(new Principal() {
                                @Override
                                public String getName() {
                                    return userId;
                                }
                            });
                        }
                        log.info("STOMP: [{}] dest={} attr.userId={} current.user={}", cmd, accessor.getDestination(), userId, (accessor.getUser() != null ? accessor.getUser().getName() : "anon"));
                    }
                }
                return message;
            }
        });
    }
}
