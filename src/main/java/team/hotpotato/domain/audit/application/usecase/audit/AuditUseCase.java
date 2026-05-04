package team.hotpotato.domain.audit.application.usecase.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.audit.application.input.AuditStatement;
import team.hotpotato.domain.audit.application.output.AuditRepository;
import team.hotpotato.domain.audit.application.output.AuditSource;
import team.hotpotato.domain.audit.domain.Audit;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;

@Service
@RequiredArgsConstructor
public class AuditUseCase implements AuditStatement {
    private final AuditSource auditSource;
    private final AuditRepository auditRepository;
    private final GetProtectByUserId getProtectByUserId;
    private final IdGenerator idGenerator;

    @Override
    public Mono<AuditResult> audit(AuditCommand command) {
        return getProtectByUserId.get(command.userId())
                .flatMap(protect -> auditSource.audit(protect.target(), protect.info(), command.text())
                        .map(analysis -> new Audit(
                                idGenerator.generateId(),
                                protect.userId(),
                                protect.target(),
                                protect.info(),
                                command.text(),
                                analysis.reviews()
                        ))
                )
                .flatMap(auditRepository::save)
                .map(audit -> new AuditResult(audit.auditId(), audit.reviews()));
    }
}
