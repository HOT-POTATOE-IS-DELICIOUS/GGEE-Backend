package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.Session;

public interface SessionReader {
    Mono<Session> findActiveByUserId(Long userId);

    Mono<Session> findBySessionId(String sessionId);
}
