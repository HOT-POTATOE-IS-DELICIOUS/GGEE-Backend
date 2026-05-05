package team.hotpotato.domain.protect.infrastructure.r2dbc.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;

import java.time.Duration;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxRepositoryAdapter implements ProtectTargetIndexingOutboxRepository {
    private final R2dbcEntityTemplate template;

    @Override
    public Flux<ProtectTargetIndexingOutbox> findPending() {
        return template.select(
                        Query.query(
                                        Criteria.where("status").is(ProtectTargetIndexingOutboxStatus.PENDING.name())
                                                .and("deleted").is(false)
                                )
                                .sort(Sort.by(Sort.Order.asc("createdAt"))),
                        ProtectTargetIndexingOutboxEntity.class
                )
                .map(ProtectTargetIndexingOutboxEntityMapper::toDomain);
    }

    @Override
    public Mono<ProtectTargetIndexingOutbox> save(ProtectTargetIndexingOutbox outbox) {
        return template.insert(ProtectTargetIndexingOutboxEntity.class)
                .using(ProtectTargetIndexingOutboxEntityMapper.toEntity(outbox))
                .map(ProtectTargetIndexingOutboxEntityMapper::toDomain);
    }

    @Override
    public Mono<Long> claim(Long outboxId) {
        return template.update(
                Query.query(
                        Criteria.where("id").is(outboxId)
                                .and("status").is(ProtectTargetIndexingOutboxStatus.PENDING.name())
                                .and("deleted").is(false)
                ),
                Update.update("status", ProtectTargetIndexingOutboxStatus.IN_PROGRESS.name())
                        .set("claimed_at", LocalDateTime.now()),
                ProtectTargetIndexingOutboxEntity.class
        );
    }

    @Override
    public Mono<Void> markPublished(Long outboxId) {
        return template.update(
                        Query.query(
                                Criteria.where("id").is(outboxId)
                                        .and("status").is(ProtectTargetIndexingOutboxStatus.IN_PROGRESS.name())
                                        .and("deleted").is(false)
                        ),
                        Update.update("status", ProtectTargetIndexingOutboxStatus.PUBLISHED.name())
                                .set("published_at", LocalDateTime.now()),
                        ProtectTargetIndexingOutboxEntity.class
                )
                .then();
    }

    @Override
    public Mono<Void> markCompleted(Long outboxId) {
        return template.update(
                        Query.query(
                                Criteria.where("id").is(outboxId)
                                        .and("status").is(ProtectTargetIndexingOutboxStatus.PUBLISHED.name())
                                        .and("deleted").is(false)
                        ),
                        Update.update("status", ProtectTargetIndexingOutboxStatus.COMPLETED.name()),
                        ProtectTargetIndexingOutboxEntity.class
                )
                .then();
    }

    @Override
    public Mono<Void> rollbackToPending(Long outboxId) {
        return template.update(
                        Query.query(
                                Criteria.where("id").is(outboxId)
                                        .and("status").is(ProtectTargetIndexingOutboxStatus.IN_PROGRESS.name())
                                        .and("deleted").is(false)
                        ),
                        Update.update("status", ProtectTargetIndexingOutboxStatus.PENDING.name())
                                .set("claimed_at", null),
                        ProtectTargetIndexingOutboxEntity.class
                )
                .then();
    }

    @Override
    public Mono<Long> recoverStaleClaimed(Duration staleThreshold) {
        LocalDateTime cutoff = LocalDateTime.now().minus(staleThreshold);
        return template.update(
                Query.query(
                        Criteria.where("status").is(ProtectTargetIndexingOutboxStatus.IN_PROGRESS.name())
                                .and("claimed_at").isNotNull()
                                .and("claimed_at").lessThan(cutoff)
                                .and("deleted").is(false)
                ),
                Update.update("status", ProtectTargetIndexingOutboxStatus.PENDING.name())
                        .set("claimed_at", null),
                ProtectTargetIndexingOutboxEntity.class
        );
    }
}
