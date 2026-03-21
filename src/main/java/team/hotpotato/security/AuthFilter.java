package team.hotpotato.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.domain.member.application.auth.TokenResolver;
import team.hotpotato.infrastructure.jwt.TokenProperties;
import team.hotpotato.support.advice.ErrorCodeHttpStatusMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthFilter implements WebFilter {
    private static final String ROLE_PREFIX = "ROLE_";
    private final TokenResolver tokenResolver;
    private final ErrorCodeHttpStatusMapper errorCodeHttpStatusMapper;
    private final TokenProperties tokenProperties;
    private final ServerWebExchangeMatcher publicPathMatcher =
            ServerWebExchangeMatchers.pathMatchers(SecurityPaths.PUBLIC_PATHS);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return isPublicRoute(exchange)
                .flatMap(isPublicRoute -> authenticate(exchange, chain, isPublicRoute));
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, boolean isPublicRoute) {
        final String headerValue = exchange.getRequest().getHeaders().getFirst(tokenProperties.header());

        if (headerValue == null || headerValue.isBlank()) {
            return chain.filter(exchange);
        }

        return tokenResolver.resolve(headerValue)
                .flatMap(principal -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        principal.userId(),
                                        null,
                                        List.of(new SimpleGrantedAuthority(toAuthority(principal.role().name())))
                                )
                        ))
                )
                .onErrorResume(BusinessBaseException.class, e -> {
                    if (isPublicRoute) {
                        return chain.filter(exchange);
                    }
                    return writeErrorResponse(exchange, e);
                });
    }

    private Mono<Boolean> isPublicRoute(ServerWebExchange exchange) {
        return publicPathMatcher.matches(exchange)
                .map(ServerWebExchangeMatcher.MatchResult::isMatch);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, BusinessBaseException exception) {
        exchange.getResponse().setStatusCode(errorCodeHttpStatusMapper.toHttpStatus(exception.getErrorCode()));
        return exchange.getResponse().setComplete();
    }

    private String toAuthority(String role) {
        if (role.startsWith(ROLE_PREFIX)) {
            return role;
        }
        return ROLE_PREFIX + role;
    }
}
