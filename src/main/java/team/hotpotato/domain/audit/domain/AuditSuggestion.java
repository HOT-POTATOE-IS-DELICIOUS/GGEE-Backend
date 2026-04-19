package team.hotpotato.domain.audit.domain;

public record AuditSuggestion(
        int startIndex,
        int endIndex,
        String before,
        String after,
        String reason
) {
}
