package team.hotpotato.domain.audit.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.application.dto.AuditCommand;
import team.hotpotato.domain.audit.application.dto.AuditResult;

public interface AuditStatement {
    Mono<AuditResult> audit(AuditCommand command);
}
