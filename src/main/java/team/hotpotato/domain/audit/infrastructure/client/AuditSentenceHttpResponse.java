package team.hotpotato.domain.audit.infrastructure.client;

public record AuditSentenceHttpResponse(
        String sentenceText,
        int startOffset,
        int endOffset
) {
}
