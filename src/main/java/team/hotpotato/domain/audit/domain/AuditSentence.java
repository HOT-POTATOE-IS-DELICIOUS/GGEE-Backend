package team.hotpotato.domain.audit.domain;

public record AuditSentence(
        String sentenceText,
        int startOffset,
        int endOffset
) {
}
