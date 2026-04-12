package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.model.AuthPrincipal;

public interface RefreshTokenResolver {
    Mono<AuthPrincipal> resolve(String refreshToken);
}
