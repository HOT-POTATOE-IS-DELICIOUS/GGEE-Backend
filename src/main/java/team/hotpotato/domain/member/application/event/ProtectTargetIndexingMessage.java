package team.hotpotato.domain.member.application.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProtectTargetIndexingMessage(
        @JsonProperty("job_id")
        String jobId,
        String keyword
) {
}
