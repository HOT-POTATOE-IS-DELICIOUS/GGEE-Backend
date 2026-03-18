package team.hotpotato.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.auth.TokenResolver;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthFilter implements WebFilter {
    private final TokenResolver tokenResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final String headerValue = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (headerValue == null || headerValue.isBlank()) {
            return chain.filter(exchange);
        }

        return tokenResolver.resolve(headerValue)
                .flatMap(principal -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        principal.userId(),
                                        null,
                                        List.of(new SimpleGrantedAuthority(principal.role()))
                                )
                        ))
                );
    }
}
