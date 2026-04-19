package team.hotpotato.domain.audit.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditSentenceHttpResponse(
        @JsonProperty("sentence_text") String sentenceText,
        @JsonProperty("start_offset") int startOffset,
        @JsonProperty("end_offset") int endOffset
) {
}
