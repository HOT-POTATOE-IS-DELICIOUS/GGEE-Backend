package team.hotpotato.domain.member.application.auth;

import reactor.core.publisher.Mono;

public interface TokenResolver {
    Mono<AuthPrincipal> resolve(String authorizationHeader);
}
