package team.hotpotato.domain.audit.infrastructure.client;

public record AuditSentenceHttpResponse(
        String sentenceText,
        Integer startOffset,
        Integer endOffset
) {
}
