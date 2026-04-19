package team.hotpotato.domain.audit.application.usecase.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.audit.application.input.AuditStatement;
import team.hotpotato.domain.audit.application.output.AuditRepository;
import team.hotpotato.domain.audit.application.output.AuditSource;
import team.hotpotato.domain.audit.domain.Audit;
import team.hotpotato.domain.member.application.input.GetUser;

@Service
@RequiredArgsConstructor
public class AuditUseCase implements AuditStatement {
    private final AuditSource auditSource;
    private final AuditRepository auditRepository;
    private final GetUser getUser;
    private final IdGenerator idGenerator;

    @Override
    public Mono<AuditResult> audit(AuditCommand command) {
        return getUser.get(command.userId())
                .flatMap(user -> auditSource.audit(user.protectTarget(), user.protectTargetInfo(), command.text())
                        .map(analysis -> new Audit(
                                idGenerator.generateId(),
                                user.id(),
                                user.protectTarget(),
                                user.protectTargetInfo(),
                                command.text(),
                                analysis.reviews()
                        ))
                )
                .flatMap(auditRepository::save)
                .map(audit -> new AuditResult(audit.auditId(), audit.reviews()));
    }
}
