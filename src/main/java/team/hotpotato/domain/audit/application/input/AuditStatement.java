package team.hotpotato.domain.audit.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.application.usecase.audit.AuditCommand;
import team.hotpotato.domain.audit.application.usecase.audit.AuditResult;

public interface AuditStatement {
    Mono<AuditResult> audit(AuditCommand command);
}
