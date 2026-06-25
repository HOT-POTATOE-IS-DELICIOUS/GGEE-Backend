package team.hotpotato.domain.reaction.infrastructure.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionRepository;

@Repository
@RequiredArgsConstructor
public class R2dbcIndexingJobCompletionRepository implements IndexingJobCompletionRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Boolean> isCompleted(String jobId) {
        Long outboxId = parseJobId(jobId);
        if (outboxId == null) {
            return Mono.just(false);
        }

        return template.getDatabaseClient()
                .sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM protect_target_indexing_outbox
                            WHERE id = :id
                              AND status = 'COMPLETED'
                              AND deleted = false
                        ) AS completed
                        """)
                .bind("id", outboxId)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("completed", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    private Long parseJobId(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(jobId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
