package team.hotpotato.domain.member.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.Session;

public interface SessionAppender {
    Mono<Session> save(Session session);

    Mono<Void> invalidateByUserId(Long userId);
}
