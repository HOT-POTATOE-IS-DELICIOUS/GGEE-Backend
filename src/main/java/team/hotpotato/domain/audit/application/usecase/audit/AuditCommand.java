package team.hotpotato.domain.audit.application.usecase.audit;

public record AuditCommand(
        Long userId,
        String text
) {
}
