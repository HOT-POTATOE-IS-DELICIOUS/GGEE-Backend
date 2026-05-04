package team.hotpotato.domain.protect.infrastructure.r2dbc.protect;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.domain.Protect;
import team.hotpotato.domain.protect.domain.ProtectTargetSnapshot;

@Repository
@RequiredArgsConstructor
public class ProtectRepositoryAdapter implements ProtectRepository {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Protect> save(Protect protect) {
        return template.insert(ProtectEntity.class)
                .using(ProtectEntityMapper.toEntity(protect))
                .map(ProtectEntityMapper::toDomain);
    }

    @Override
    public Mono<Protect> findByUserId(Long userId) {
        return template.selectOne(
                        Query.query(Criteria.where("user_id").is(userId).and("deleted").is(false)),
                        ProtectEntity.class
                )
                .map(ProtectEntityMapper::toDomain);
    }

    @Override
    public Flux<ProtectTargetSnapshot> findActiveDistinctProtectTargets() {
        return template.getDatabaseClient()
                .sql("SELECT DISTINCT target, info FROM protects WHERE deleted = false")
                .map((row, meta) -> new ProtectTargetSnapshot(
                        row.get("target", String.class),
                        row.get("info", String.class)
                ))
                .all();
    }
}
