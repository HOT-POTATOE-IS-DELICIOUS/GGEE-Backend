package team.hotpotato.infrastructure.r2dbc.member;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxUpdater;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxUpdaterAdapter implements ProtectTargetIndexingOutboxUpdater {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Void> markPublished(Long outboxId) {
        return template.update(
                        Query.query(Criteria.where("id").is(outboxId).and("deleted").is(false)),
                        Update.update("status", ProtectTargetIndexingOutboxStatus.PUBLISHED.name())
                                .set("published_at", LocalDateTime.now()),
                        ProtectTargetIndexingOutboxEntity.class
                )
                .then();
    }
}
