package com.thang.nihongo_user.wallet.realtime;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class WalletSocketAuthorization implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) throw new AccessDeniedException("Missing STOMP headers");
        StompCommand command = headers.getCommand();
        if (command == StompCommand.DISCONNECT) return message;
        if (!(headers.getUser() instanceof JwtAuthenticationToken auth) || !auth.isAuthenticated()
            || auth.getToken().getExpiresAt() == null || !auth.getToken().getExpiresAt().isAfter(Instant.now())
            || !"access".equals(auth.getToken().getClaimAsString("type")))
            throw new AccessDeniedException("Authentication required");
        if (command == StompCommand.SUBSCRIBE) {
            boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!"/user/queue/wallet".equals(headers.getDestination())
                && !(admin && "/topic/wallet-admin".equals(headers.getDestination())))
                throw new AccessDeniedException("Subscription forbidden");
        } else if (command != null && command != StompCommand.CONNECT && command != StompCommand.STOMP
                   && command != StompCommand.UNSUBSCRIBE) {
            // Browsers may subscribe, never publish application or broker messages.
            throw new AccessDeniedException("Client publishing forbidden");
        }
        return message;
    }
}
