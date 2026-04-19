package team.hotpotato.domain.audit.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.domain.AuditAnalysis;

public interface AuditSource {
    Mono<AuditAnalysis> audit(String protectTarget, String protectTargetInfo, String text);
}
