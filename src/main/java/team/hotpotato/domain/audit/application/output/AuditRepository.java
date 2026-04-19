package team.hotpotato.domain.audit.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.domain.Audit;

public interface AuditRepository {
    Mono<Audit> save(Audit audit);
}
