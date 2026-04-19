package team.hotpotato.domain.audit.infrastructure.r2dbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.application.output.AuditRepository;
import team.hotpotato.domain.audit.domain.Audit;

@Repository
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepository {
    private final R2dbcEntityTemplate template;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Audit> save(Audit audit) {
        return serializeReviews(audit)
                .map(reviewsJson -> AuditEntityMapper.toEntity(audit, reviewsJson))
                .flatMap(entity -> template.insert(AuditEntity.class)
                        .using(entity)
                        .thenReturn(audit)
                );
    }

    private Mono<String> serializeReviews(Audit audit) {
        try {
            return Mono.just(objectMapper.writeValueAsString(audit.reviews()));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("검수 결과 직렬화에 실패했습니다.", exception));
        }
    }
}
