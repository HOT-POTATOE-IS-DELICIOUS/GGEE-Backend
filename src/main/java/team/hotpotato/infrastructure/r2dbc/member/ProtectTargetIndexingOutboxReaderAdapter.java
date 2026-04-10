package team.hotpotato.infrastructure.r2dbc.member;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxReader;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

@Repository
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxReaderAdapter implements ProtectTargetIndexingOutboxReader {
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
}
