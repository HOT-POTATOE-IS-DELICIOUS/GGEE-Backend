package team.hotpotato.domain.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditSentenceResponse(
        @JsonProperty("sentence_text") String sentenceText,
        @JsonProperty("start_offset") int startOffset,
        @JsonProperty("end_offset") int endOffset
) {
}
