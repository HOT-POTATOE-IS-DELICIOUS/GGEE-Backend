package team.hotpotato.domain.audit.application.dto;

public record AuditCommand(
        Long userId,
        String text
) {
}
