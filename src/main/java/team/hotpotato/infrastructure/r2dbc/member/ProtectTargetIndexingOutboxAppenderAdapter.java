package team.hotpotato.infrastructure.r2dbc.member;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxAppender;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;

@Repository
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxAppenderAdapter implements ProtectTargetIndexingOutboxAppender {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox) {
        return template.insert(ProtectTargetIndexingOutboxEntity.class)
                .using(ProtectTargetIndexingOutboxEntityMapper.toEntity(outbox))
                .map(ProtectTargetIndexingOutboxEntityMapper::toDomain);
    }
}
