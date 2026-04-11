package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.Session;

public interface SessionRepository {
    Mono<Session> findActiveByUserId(Long userId);

    Mono<Session> findBySessionId(String sessionId);

    Mono<Session> save(Session session);

    Mono<Void> invalidateByUserId(Long userId);
}
