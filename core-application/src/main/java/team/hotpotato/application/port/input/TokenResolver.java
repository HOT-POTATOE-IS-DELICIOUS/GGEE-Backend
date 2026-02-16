package team.hotpotato.application.port.input;

import reactor.core.publisher.Mono;
import team.hotpotato.application.dto.model.AuthPrincipal;

public interface TokenResolver {
    Mono<AuthPrincipal> resolve(String authorizationHeader);
}
