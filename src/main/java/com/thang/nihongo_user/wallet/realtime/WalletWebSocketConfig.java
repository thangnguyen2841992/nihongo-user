package com.thang.nihongo_user.wallet.realtime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Configuration @EnableWebSocketMessageBroker
public class WalletWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WalletSocketAuthorization authorization;
    private final String[] origins;
    public WalletWebSocketConfig(WalletSocketAuthorization authorization,
            @Value("${wallet.websocket.allowed-origins:http://localhost:5173}") String[] origins) {
        this.authorization = authorization; this.origins = origins;
    }
    @Bean public ThreadPoolTaskScheduler walletSocketScheduler() {
        var scheduler = new ThreadPoolTaskScheduler(); scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("wallet-socket-"); scheduler.setRemoveOnCancelPolicy(true); return scheduler;
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/nihongo-user/wallets/ws").setAllowedOrigins(origins);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic").setTaskScheduler(walletSocketScheduler()).setHeartbeatValue(new long[]{10000, 10000});
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorization);
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(8192).setSendBufferSizeLimit(65536).setSendTimeLimit(10000);
        registry.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            private final Map<String, ScheduledFuture<?>> expiry = new ConcurrentHashMap<>();
            @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                if (!(session.getPrincipal() instanceof JwtAuthenticationToken auth)
                    || !"access".equals(auth.getToken().getClaimAsString("type"))
                    || auth.getToken().getExpiresAt() == null || !auth.getToken().getExpiresAt().isAfter(Instant.now())) {
                    session.close(CloseStatus.POLICY_VIOLATION); return;
                }
                super.afterConnectionEstablished(session);
                expiry.put(session.getId(), walletSocketScheduler().schedule(() -> {
                    try { session.close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) { }
                }, auth.getToken().getExpiresAt()));
            }
            @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                ScheduledFuture<?> task = expiry.remove(session.getId());
                if (task != null) task.cancel(false);
                super.afterConnectionClosed(session, status);
            }
        });
    }
}
